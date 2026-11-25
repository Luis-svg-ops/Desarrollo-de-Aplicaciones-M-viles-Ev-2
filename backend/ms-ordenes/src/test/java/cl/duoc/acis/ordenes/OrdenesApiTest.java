package cl.duoc.acis.ordenes;

import cl.duoc.acis.ordenes.service.DirectorioTecnicos;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la API de OT con los datos de ejemplo (7 OT, 3 locales, 6 equipos).
 * El token se simula con jwt() (no hace falta ms-auth) y el directorio de técnicos es un mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrdenesApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DirectorioTecnicos tecnicos;

    @BeforeEach
    void tecnicosFalsos() {
        when(tecnicos.nombreDe(anyLong(), anyString())).thenReturn(Optional.empty());
        when(tecnicos.nombreDe(eq(1L), anyString())).thenReturn(Optional.of("Pedro Soto"));
    }

    // ---------- Usuarios simulados (lo que vendría dentro del token de ms-auth) ----------

    private RequestPostProcessor como(long id, String nombre, String rol, Long localId) {
        return jwt().jwt(j -> {
            j.subject(String.valueOf(id)).claim("nombre", nombre).claim("rol", rol);
            if (localId != null) j.claim("localId", localId);
        }).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private RequestPostProcessor jefe() { return como(2, "Carolina Muñoz", "JEFE", null); }
    private RequestPostProcessor pedro() { return como(1, "Pedro Soto", "TECNICO", null); }
    private RequestPostProcessor cliente() { return como(6, "Andrea Pino", "CLIENTE", 2L); }

    private ResultActions postJson(String url, RequestPostProcessor quien, String json) throws Exception {
        return mvc.perform(post(url).with(quien).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String nuevaOt(String tecnicoId) {
        long manana = System.currentTimeMillis() + 86_400_000;
        return """
                {"equipoId":6,"tecnicoId":%s,"tipo":"CORRECTIVA","prioridad":"MEDIA",
                 "descripcion":"Equipo con ruido fuerte al partir","fechaProgramada":%d}""".formatted(tecnicoId, manana);
    }

    private int crearOt(String tecnicoId) throws Exception {
        String cuerpo = postJson("/ordenes", jefe(), nuevaOt(tecnicoId))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.id");
    }

    // ---------- Quién ve qué (filtro por token) ----------

    @Test
    void sinToken_401() throws Exception {
        mvc.perform(get("/ordenes")).andExpect(status().isUnauthorized());
    }

    @Test
    void tecnico_soloVeSusOt() throws Exception {
        mvc.perform(get("/ordenes").with(pedro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tecnicoId", everyItem(is(1))));
    }

    @Test
    void cliente_soloVeLasDeSuLocal() throws Exception {
        mvc.perform(get("/ordenes").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].local.id", everyItem(is(2))));
    }

    @Test
    void tecnico_otAjena_404_noRevelaQueExiste() throws Exception {
        // La OT 6 es de Matías Rojas
        mvc.perform(get("/ordenes/6").with(pedro())).andExpect(status().isNotFound());
    }

    @Test
    void tecnico_noPuedeCrearOt_403() throws Exception {
        postJson("/ordenes", pedro(), nuevaOt("1")).andExpect(status().isForbidden());
    }

    // ---------- Recorrido completo ----------

    @Test
    void caminoPrincipal_creaAceptaCheckInCierraYConformidad() throws Exception {
        int id = crearOt("1");
        String url = "/ordenes/" + id;

        mvc.perform(get(url).with(pedro())).andExpect(jsonPath("$.estado").value("ASIGNADA"))
                .andExpect(jsonPath("$.numero", containsString("OT-")))
                .andExpect(jsonPath("$.tecnicoNombre").value("Pedro Soto"));

        postJson(url + "/acciones", pedro(), "{\"accion\":\"ACEPTAR\"}")
                .andExpect(jsonPath("$.estado").value("ACEPTADA"));
        postJson(url + "/check-in", pedro(), "{\"latitud\":-33.42,\"longitud\":-70.61}")
                .andExpect(jsonPath("$.estado").value("EN_EJECUCION"))
                .andExpect(jsonPath("$.checkInLat").value(-33.42));
        postJson(url + "/cierre", pedro(), """
                {"diagnostico":"Rodamiento del ventilador gastado","actividades":"Cambio de rodamiento y prueba",
                 "materiales":"Rodamiento 6203","fotoUri":"content://foto/1"}""")
                .andExpect(jsonPath("$.estado").value("CERRADA"));
        postJson(url + "/acciones", jefe(), "{\"accion\":\"CONFORMIDAD\"}")
                .andExpect(jsonPath("$.estado").value("CONFORME"));

        // Creada, asignada, aceptada, check-in, cerrada, conforme
        mvc.perform(get(url + "/historial").with(jefe()))
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].descripcion").value("Conformidad de la jefatura"));
    }

    // ---------- Reglas que no se pueden saltar ----------

    @Test
    void cerrarSinCheckIn_409() throws Exception {
        // La OT 2 está aceptada: todavía no hay check-in
        postJson("/ordenes/2/cierre", pedro(), """
                {"diagnostico":"Intento de cierre directo","actividades":"Ninguna, me salto el check-in",
                 "fotoUri":"content://foto/x"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Acción no permitida")));
    }

    @Test
    void tecnicoDandoConformidad_403() throws Exception {
        postJson("/ordenes/4/acciones", pedro(), "{\"accion\":\"CONFORMIDAD\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Tu rol no permite esta acción"));
    }

    @Test
    void motivoInventado_400_yMotivoOtroEscrito_ok() throws Exception {
        int id = crearOt("1");
        postJson("/ordenes/" + id + "/acciones", pedro(), "{\"accion\":\"RECHAZAR\",\"motivo\":\"Me dio flojera\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Motivo no válido"));
        postJson("/ordenes/" + id + "/acciones", pedro(), "{\"accion\":\"RECHAZAR\",\"motivo\":\"Otro: Estoy con licencia\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivo").value("Otro: Estoy con licencia"));
    }

    @Test
    void asignarTecnicoInexistente_400() throws Exception {
        postJson("/ordenes", jefe(), nuevaOt("99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El técnico no existe o está desactivado"));
    }

    @Test
    void descripcionCorta_400ConMensaje() throws Exception {
        postJson("/ordenes", jefe(), """
                {"equipoId":6,"tipo":"CORRECTIVA","prioridad":"MEDIA","descripcion":"corta",
                 "fechaProgramada":%d}""".formatted(System.currentTimeMillis() + 86_400_000))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La descripción debe tener entre 10 y 300 caracteres"));
    }

    @Test
    void eliminarOt_soloSiEstaRecienCreada() throws Exception {
        // Con técnico asignado: hay que cancelarla
        mvc.perform(delete("/ordenes/1").with(jefe())).andExpect(status().isConflict());
        // Recién creada y sin técnico: se puede borrar
        int id = crearOt("null");
        mvc.perform(delete("/ordenes/" + id).with(jefe())).andExpect(status().isOk());
        mvc.perform(get("/ordenes/" + id).with(jefe())).andExpect(status().isNotFound());
    }

    // ---------- Locales y equipos ----------

    @Test
    void codigoDeLocal_esPublico_ySoloEntregaIdYNombre() throws Exception {
        mvc.perform(get("/locales/codigo/mall-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.telefono").doesNotExist());
        mvc.perform(get("/locales/codigo/NO-EXISTE")).andExpect(status().isNotFound());
    }

    @Test
    void cliente_veSuLocal_peroNoLaLista() throws Exception {
        mvc.perform(get("/locales/mio").with(cliente())).andExpect(jsonPath("$.nombre").value("Mall Costanera Oeste"));
        mvc.perform(get("/locales").with(cliente())).andExpect(status().isForbidden());
    }

    @Test
    void equipo_conOtEnCurso_409_ySinOt_seElimina() throws Exception {
        // El equipo 2 tiene la OT 1 asignada
        mvc.perform(delete("/equipos/2").with(jefe())).andExpect(status().isConflict());

        String cuerpo = postJson("/equipos", jefe(), """
                {"localId":1,"tipo":"CLIMATIZACION","marca":"Samsung","modelo":"Split 12.000","ubicacion":"INTERIOR"}""")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(cuerpo, "$.id");
        mvc.perform(delete("/equipos/" + id).with(jefe()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Equipo eliminado"));
    }
}
