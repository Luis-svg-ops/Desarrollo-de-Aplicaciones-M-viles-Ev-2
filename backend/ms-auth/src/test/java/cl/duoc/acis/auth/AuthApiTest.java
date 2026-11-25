package cl.duoc.acis.auth;

import cl.duoc.acis.auth.correo.EnviadorCorreo;
import cl.duoc.acis.auth.service.DirectorioLocales;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la API completa (servidor + base H2 en memoria).
 * Se "mockea" el correo (para leer el código sin enviarlo) y el directorio de locales
 * (para no depender de que ms-ordenes esté encendido).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EnviadorCorreo correo;

    @MockitoBean
    private DirectorioLocales locales;

    @BeforeEach
    void localesFalsos() {
        // Solo existe el código "MALL-02", que corresponde al local 2
        when(locales.buscarPorCodigo(anyString())).thenReturn(Optional.empty());
        when(locales.buscarPorCodigo("MALL-02")).thenReturn(Optional.of(2L));
    }

    // ---------- Ayudas ----------

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions login(String correo, String clave) throws Exception {
        return postJson("/auth/login", "{\"correo\":\"" + correo + "\",\"clave\":\"" + clave + "\"}");
    }

    private String token(String correo) throws Exception {
        String cuerpo = login(correo, "123456").andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(cuerpo, "$.token");
    }

    private ResultActions registrar(String correo, String codigoLocal) throws Exception {
        return postJson("/auth/registro", """
                {"nombre":"Cliente Nuevo","correo":"%s","clave":"secreta1",
                 "telefono":"+56 9 1111 2222","codigoLocal":"%s"}""".formatted(correo, codigoLocal));
    }

    private String ultimoCodigoEnviadoA(String destinatario) {
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq(destinatario), anyString(), texto.capture());
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(texto.getValue());
        if (!m.find()) throw new AssertionError("El correo no trae un código");
        return m.group(1);
    }

    // ---------- Login ----------

    @Test
    void login_correcto_entregaTokenYRol() throws Exception {
        login("jefe@acis.cl", "123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(28800))
                .andExpect(jsonPath("$.usuario.rol").value("JEFE"))
                .andExpect(jsonPath("$.usuario.clave").doesNotExist());
    }

    @Test
    void login_correoConMayusculasYEspacios_igualFunciona() throws Exception {
        login("  Tecnico@ACIS.cl ", "123456").andExpect(status().isOk());
    }

    @Test
    void login_claveIncorrecta_401() throws Exception {
        login("jefe@acis.cl", "otra").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos"));
    }

    @Test
    void login_camposVacios_400() throws Exception {
        login("", "").andExpect(status().isBadRequest());
    }

    @Test
    void login_cuentaPendiente_403ConMensajeClaro() throws Exception {
        login("jdiaz@acis.cl", "123456").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("pendiente de aprobación")));
    }

    @Test
    void login_pendienteConClaveMala_noRevelaQueEstaPendiente() throws Exception {
        login("jdiaz@acis.cl", "mala").andExpect(status().isUnauthorized());
    }

    // ---------- Registro ----------

    @Test
    void registro_quedaPendiente_yAvisaAlAdmin() throws Exception {
        registrar("nuevo1@correo.cl", "MALL-02").andExpect(status().isCreated());
        login("nuevo1@correo.cl", "secreta1").andExpect(status().isForbidden());
        verify(correo).enviar(eq("admin@acis.cl"), org.mockito.ArgumentMatchers.contains("nuevo cliente"), anyString());
    }

    @Test
    void registro_codigoDeLocalInvalido_400() throws Exception {
        registrar("nuevo2@correo.cl", "NO-EXISTE").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Código de local no válido"));
    }

    @Test
    void registro_correoRepetido_409() throws Exception {
        registrar("jefe@acis.cl", "MALL-02").andExpect(status().isConflict());
    }

    @Test
    void registro_datosInvalidos_400ConElMensajeDelCampo() throws Exception {
        postJson("/auth/registro", """
                {"nombre":"Ana","correo":"ana@correo.cl","clave":"123",
                 "telefono":"+56 9 1111 2222","codigoLocal":"MALL-02"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La contraseña debe tener entre 6 y 40 caracteres"));
    }

    // ---------- Recuperar contraseña ----------

    @Test
    void recuperar_correoInexistente_respondeLoMismo_yNoEnviaNada() throws Exception {
        postJson("/auth/recuperar", "{\"correo\":\"nadie@correo.cl\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", containsString("Si el correo está registrado")));
        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void recuperar_yRestablecer_cambiaLaClave() throws Exception {
        postJson("/auth/recuperar", "{\"correo\":\"cfuentes@acis.cl\"}").andExpect(status().isOk());
        String codigo = ultimoCodigoEnviadoA("cfuentes@acis.cl");

        postJson("/auth/restablecer", """
                {"correo":"cfuentes@acis.cl","codigo":"%s","claveNueva":"nueva123"}""".formatted(codigo))
                .andExpect(status().isOk());

        login("cfuentes@acis.cl", "nueva123").andExpect(status().isOk());
        // El código ya se usó: no sirve una segunda vez
        postJson("/auth/restablecer", """
                {"correo":"cfuentes@acis.cl","codigo":"%s","claveNueva":"otra1234"}""".formatted(codigo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restablecer_cincoIntentosFallidos_invalidanElCodigo() throws Exception {
        postJson("/auth/recuperar", "{\"correo\":\"daraya@acis.cl\"}");
        String codigo = ultimoCodigoEnviadoA("daraya@acis.cl");
        String incorrecto = codigo.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            postJson("/auth/restablecer", """
                    {"correo":"daraya@acis.cl","codigo":"%s","claveNueva":"nueva123"}""".formatted(incorrecto))
                    .andExpect(status().isBadRequest());
        }
        // Ahora ni el código correcto sirve
        postJson("/auth/restablecer", """
                {"correo":"daraya@acis.cl","codigo":"%s","claveNueva":"nueva123"}""".formatted(codigo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Pide uno nuevo")));
    }

    // ---------- Perfil ----------

    @Test
    void perfil_sinToken_401() throws Exception {
        mvc.perform(get("/usuarios/yo")).andExpect(status().isUnauthorized());
    }

    @Test
    void perfil_conToken_muestraYEdita() throws Exception {
        String t = token("mrojas@acis.cl");
        mvc.perform(get("/usuarios/yo").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Matías Rojas"));

        mvc.perform(put("/usuarios/yo").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Matías Rojas P.\",\"telefono\":\"+56 9 4444 3333\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("+56 9 4444 3333"));
    }

    @Test
    void perfil_cambiarClaveSinLaActual_400() throws Exception {
        mvc.perform(put("/usuarios/yo").header("Authorization", token("cliente@acis.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Andrea Pino\",\"telefono\":\"+56 9 7345 1122\","
                                + "\"claveActual\":\"mala\",\"claveNueva\":\"nueva123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La contraseña actual no es correcta"));
    }

    @Test
    void tecnicos_losVeElJefe_peroNoElTecnico() throws Exception {
        mvc.perform(get("/usuarios/tecnicos").header("Authorization", token("jefe@acis.cl")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].rol", not(hasItem("JEFE"))));
        mvc.perform(get("/usuarios/tecnicos").header("Authorization", token("tecnico@acis.cl")))
                .andExpect(status().isForbidden());
    }

    // ---------- Administrador ----------

    @Test
    void admin_soloElAdminEntra() throws Exception {
        mvc.perform(get("/admin/usuarios").header("Authorization", token("jefe@acis.cl")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/usuarios").header("Authorization", token("admin@acis.cl")))
                .andExpect(status().isOk());
    }

    @Test
    void admin_apruebaAlPendiente_yYaPuedeEntrar() throws Exception {
        registrar("nuevo3@correo.cl", "MALL-02");
        String admin = token("admin@acis.cl");
        String cuerpo = mvc.perform(get("/admin/usuarios?estado=PENDIENTE").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        java.util.List<Integer> ids = JsonPath.read(cuerpo, "$[?(@.correo=='nuevo3@correo.cl')].id");
        Integer id = ids.get(0);

        mvc.perform(put("/admin/usuarios/" + id + "/aprobar").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        login("nuevo3@correo.cl", "secreta1").andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.localId").value(2));
    }

    @Test
    void admin_creaTecnico_yLoDesactiva() throws Exception {
        String admin = token("admin@acis.cl");
        String cuerpo = mvc.perform(post("/admin/usuarios").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Tomás Vidal","correo":"tvidal@acis.cl","clave":"123456",
                                 "telefono":"+56 9 2222 3333","rol":"TECNICO"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(cuerpo, "$.id");

        mvc.perform(delete("/admin/usuarios/" + id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", containsString("desactivado")));
        login("tvidal@acis.cl", "123456").andExpect(status().isForbidden());
    }

    @Test
    void admin_noPuedeEliminarse_aSiMismo() throws Exception {
        mvc.perform(delete("/admin/usuarios/7").header("Authorization", token("admin@acis.cl")))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_clienteSinLocal_400() throws Exception {
        mvc.perform(post("/admin/usuarios").header("Authorization", token("admin@acis.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cliente Sin Local","correo":"sinlocal@correo.cl","clave":"123456",
                                 "telefono":"+56 9 2222 3333","rol":"CLIENTE"}"""))
                .andExpect(status().isBadRequest());
    }
}
