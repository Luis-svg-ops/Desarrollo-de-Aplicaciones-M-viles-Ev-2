package cl.duoc.acis.auth.repository;

import cl.duoc.acis.auth.model.EstadoUsuario;
import cl.duoc.acis.auth.model.Rol;
import cl.duoc.acis.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla "usuarios". Spring Data crea las consultas solo a partir del nombre del método
 * (findByCorreo → SELECT * FROM usuarios WHERE correo = ?).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findByEstadoOrderByNombre(EstadoUsuario estado);

    List<Usuario> findByRolAndEstadoOrderByNombre(Rol rol, EstadoUsuario estado);

    List<Usuario> findAllByOrderByNombre();
}
