package cl.duoc.acis.auth.service;

import cl.duoc.acis.auth.correo.EnviadorCorreo;
import cl.duoc.acis.auth.dto.AuthDtos.CrearUsuarioRequest;
import cl.duoc.acis.auth.dto.AuthDtos.EditarUsuarioRequest;
import cl.duoc.acis.auth.dto.AuthDtos.PerfilRequest;
import cl.duoc.acis.auth.exception.AcisException;
import cl.duoc.acis.auth.model.EstadoUsuario;
import cl.duoc.acis.auth.model.Rol;
import cl.duoc.acis.auth.model.Usuario;
import cl.duoc.acis.auth.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Perfil propio y gestión de usuarios del Administrador. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final EnviadorCorreo correo;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, EnviadorCorreo correo) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
    }

    public Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new AcisException.NoEncontrado("Usuario no encontrado"));
    }

    // ---------- Perfil propio ----------

    /** Cambiar la clave exige la clave actual: si alguien deja el teléfono abierto, no puede cambiarla. */
    @Transactional
    public Usuario actualizarPerfil(Long id, PerfilRequest pedido) {
        Usuario usuario = buscar(id);
        boolean cambiaClave = pedido.claveNueva() != null && !pedido.claveNueva().isBlank();
        if (cambiaClave) {
            if (pedido.claveActual() == null || !passwordEncoder.matches(pedido.claveActual(), usuario.getClave())) {
                throw new AcisException.DatoInvalido("La contraseña actual no es correcta");
            }
            usuario.cambiarClave(passwordEncoder.encode(pedido.claveNueva()));
        }
        usuario.actualizarPerfil(pedido.nombre().trim(), pedido.telefono().trim());
        return usuario;
    }

    /** Técnicos activos: los usa el jefe para asignar una OT. */
    public List<Usuario> tecnicosActivos() {
        return usuarios.findByRolAndEstadoOrderByNombre(Rol.TECNICO, EstadoUsuario.ACTIVO);
    }

    // ---------- Administrador ----------

    public List<Usuario> listar(EstadoUsuario estado) {
        return estado == null ? usuarios.findAllByOrderByNombre() : usuarios.findByEstadoOrderByNombre(estado);
    }

    @Transactional
    public Usuario crear(CrearUsuarioRequest pedido) {
        String correoNormalizado = AuthService.normalizar(pedido.correo());
        if (usuarios.existsByCorreo(correoNormalizado)) {
            throw new AcisException.Conflicto("Ese correo ya está registrado");
        }
        validarLocal(pedido.rol(), pedido.localId());
        return usuarios.save(new Usuario(pedido.nombre().trim(), correoNormalizado,
                passwordEncoder.encode(pedido.clave()), pedido.telefono().trim(), pedido.rol(),
                EstadoUsuario.ACTIVO, pedido.rol() == Rol.CLIENTE ? pedido.localId() : null));
    }

    @Transactional
    public Usuario editar(Long id, EditarUsuarioRequest pedido, Long adminId) {
        Usuario usuario = buscar(id);
        validarLocal(pedido.rol(), pedido.localId());
        if (id.equals(adminId) && (pedido.rol() != Rol.ADMIN || pedido.estado() != EstadoUsuario.ACTIVO)) {
            // Evita que el sistema quede sin administrador por un error
            throw new AcisException.Conflicto("No puedes quitarte el rol de administrador ni desactivarte");
        }
        usuario.actualizarComoAdmin(pedido.nombre().trim(), pedido.telefono().trim(), pedido.rol(),
                pedido.estado(), pedido.rol() == Rol.CLIENTE ? pedido.localId() : null);
        return usuario;
    }

    /** Aprueba a un cliente pendiente y le avisa por correo. */
    @Transactional
    public Usuario aprobar(Long id) {
        Usuario usuario = buscar(id);
        if (usuario.getEstado() != EstadoUsuario.PENDIENTE) {
            throw new AcisException.Conflicto("Este usuario no está pendiente de aprobación");
        }
        usuario.aprobar();
        correo.enviar(usuario.getCorreo(), "ACIS: tu cuenta fue aprobada",
                "Hola " + usuario.getNombre() + ",\n\nYa puedes iniciar sesión en ACIS OT Terreno.");
        return usuario;
    }

    /**
     * Eliminar: un registro pendiente se borra (es rechazarlo). Un usuario que ya trabajó se desactiva,
     * porque su nombre aparece en el historial de las OT y borrarlo rompería esa trazabilidad.
     * @return true si se borró, false si se desactivó
     */
    @Transactional
    public boolean eliminar(Long id, Long adminId) {
        if (id.equals(adminId)) {
            throw new AcisException.Conflicto("No puedes eliminar tu propia cuenta");
        }
        Usuario usuario = buscar(id);
        if (usuario.getEstado() == EstadoUsuario.PENDIENTE) {
            usuarios.delete(usuario);
            return true;
        }
        usuario.desactivar();
        return false;
    }

    private void validarLocal(Rol rol, Long localId) {
        if (rol == Rol.CLIENTE && localId == null) {
            throw new AcisException.DatoInvalido("Un cliente debe tener un local asignado");
        }
    }
}
