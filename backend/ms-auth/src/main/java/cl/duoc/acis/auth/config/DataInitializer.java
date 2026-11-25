package cl.duoc.acis.auth.config;

import cl.duoc.acis.auth.model.EstadoUsuario;
import cl.duoc.acis.auth.model.Rol;
import cl.duoc.acis.auth.model.Usuario;
import cl.duoc.acis.auth.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Usuarios de ejemplo (contraseña 123456). Se crean solo si la base está vacía.
 * Los ids coinciden con los de la app y con los técnicos de las OT de ejemplo de ms-ordenes.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() > 0) {
            return;
        }
        crear("Pedro Soto", "tecnico@acis.cl", "+56 9 5555 0101", Rol.TECNICO, EstadoUsuario.ACTIVO, null);   // 1
        crear("Carolina Muñoz", "jefe@acis.cl", "+56 9 5555 0102", Rol.JEFE, EstadoUsuario.ACTIVO, null);     // 2
        crear("Matías Rojas", "mrojas@acis.cl", "+56 9 5555 0103", Rol.TECNICO, EstadoUsuario.ACTIVO, null);  // 3
        crear("Camila Fuentes", "cfuentes@acis.cl", "+56 9 5555 0104", Rol.TECNICO, EstadoUsuario.ACTIVO, null); // 4
        crear("Diego Araya", "daraya@acis.cl", "+56 9 5555 0105", Rol.TECNICO, EstadoUsuario.ACTIVO, null);   // 5
        crear("Andrea Pino", "cliente@acis.cl", "+56 9 7345 1122", Rol.CLIENTE, EstadoUsuario.ACTIVO, 2L);    // 6
        crear("Valentina Reyes", "admin@acis.cl", "+56 9 5555 0107", Rol.ADMIN, EstadoUsuario.ACTIVO, null);  // 7
        // Un cliente esperando aprobación, para mostrar el flujo del Administrador
        crear("Jorge Díaz", "jdiaz@acis.cl", "+56 9 8123 4567", Rol.CLIENTE, EstadoUsuario.PENDIENTE, 1L);    // 8
    }

    private void crear(String nombre, String correo, String telefono, Rol rol, EstadoUsuario estado, Long localId) {
        usuarios.save(new Usuario(nombre, correo, passwordEncoder.encode("123456"), telefono, rol, estado, localId));
    }
}
