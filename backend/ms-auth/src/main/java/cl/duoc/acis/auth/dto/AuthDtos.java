package cl.duoc.acis.auth.dto;

import cl.duoc.acis.auth.model.EstadoUsuario;
import cl.duoc.acis.auth.model.Rol;
import cl.duoc.acis.auth.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO = "objeto de transferencia": la forma del JSON que entra y sale de la API.
 * Se separa de la entidad para no exponer nunca la clave cifrada ni campos internos.
 * Las anotaciones (@NotBlank, @Email...) validan en el servidor aunque la app ya haya validado
 * ("nunca confíes en el cliente").
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    private static final String TELEFONO = "^\\+?[0-9 ]{8,16}$";

    // ---------- Entrada ----------

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String clave) {
    }

    public record RegistroRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres") String nombre,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "Formato de correo no válido") String correo,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 6, max = 40, message = "La contraseña debe tener entre 6 y 40 caracteres") String clave,
            @NotBlank(message = "El teléfono es obligatorio")
            @Pattern(regexp = TELEFONO, message = "Teléfono no válido (ej.: +56 9 1234 5678)") String telefono,
            @NotBlank(message = "El código del local es obligatorio") String codigoLocal) {
    }

    public record RecuperarRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "Formato de correo no válido") String correo) {
    }

    public record RestablecerRequest(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "El código es obligatorio")
            @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 dígitos") String codigo,
            @NotBlank(message = "La contraseña nueva es obligatoria")
            @Size(min = 6, max = 40, message = "La contraseña debe tener entre 6 y 40 caracteres") String claveNueva) {
    }

    /** Editar el propio perfil. Para cambiar la clave hay que mandar también la actual. */
    public record PerfilRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres") String nombre,
            @NotBlank(message = "El teléfono es obligatorio")
            @Pattern(regexp = TELEFONO, message = "Teléfono no válido (ej.: +56 9 1234 5678)") String telefono,
            String claveActual,
            @Size(min = 6, max = 40, message = "La contraseña debe tener entre 6 y 40 caracteres") String claveNueva) {
    }

    /** El Admin crea un usuario (técnico, jefe, otro admin o un cliente ya validado). */
    public record CrearUsuarioRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres") String nombre,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "Formato de correo no válido") String correo,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 6, max = 40, message = "La contraseña debe tener entre 6 y 40 caracteres") String clave,
            @NotBlank(message = "El teléfono es obligatorio")
            @Pattern(regexp = TELEFONO, message = "Teléfono no válido (ej.: +56 9 1234 5678)") String telefono,
            @NotNull(message = "El rol es obligatorio") Rol rol,
            Long localId) {
    }

    public record EditarUsuarioRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres") String nombre,
            @NotBlank(message = "El teléfono es obligatorio")
            @Pattern(regexp = TELEFONO, message = "Teléfono no válido (ej.: +56 9 1234 5678)") String telefono,
            @NotNull(message = "El rol es obligatorio") Rol rol,
            @NotNull(message = "El estado es obligatorio") EstadoUsuario estado,
            Long localId) {
    }

    // ---------- Salida ----------

    /** Lo que se muestra de un usuario (sin la clave). */
    public record UsuarioResponse(Long id, String nombre, String correo, String telefono,
                                  Rol rol, EstadoUsuario estado, Long localId) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(),
                    u.getRol(), u.getEstado(), u.getLocalId());
        }
    }

    public record LoginResponse(String token, String tipo, long expiraEnSegundos, UsuarioResponse usuario) {
    }

    /** Respuesta simple con un texto para mostrar en la app. */
    public record MensajeResponse(String mensaje) {
    }
}
