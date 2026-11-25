package cl.duoc.acis.ordenes.repository;

import cl.duoc.acis.ordenes.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a la tabla "equipos". */
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findByLocalIdOrderByMarca(Long localId);

    long countByLocalId(Long localId);
}
