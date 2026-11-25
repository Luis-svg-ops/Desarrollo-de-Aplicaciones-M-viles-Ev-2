package cl.duoc.acis.ordenes.repository;

import cl.duoc.acis.ordenes.model.Local;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso a la tabla "locales". */
public interface LocalRepository extends JpaRepository<Local, Long> {

    Optional<Local> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Local> findAllByOrderByNombre();
}
