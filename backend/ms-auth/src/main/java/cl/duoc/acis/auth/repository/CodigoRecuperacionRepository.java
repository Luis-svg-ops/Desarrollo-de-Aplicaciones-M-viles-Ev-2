package cl.duoc.acis.auth.repository;

import cl.duoc.acis.auth.model.CodigoRecuperacion;
import cl.duoc.acis.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    /** El último código pedido por el usuario (es el único que vale). */
    Optional<CodigoRecuperacion> findFirstByUsuarioOrderByIdDesc(Usuario usuario);

    List<CodigoRecuperacion> findByUsuarioAndUsadoFalse(Usuario usuario);
}
