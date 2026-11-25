package cl.duoc.acis.ordenes.repository;

import cl.duoc.acis.ordenes.model.EstadoOT;
import cl.duoc.acis.ordenes.model.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Acceso a la tabla "ordenes_trabajo". Cada rol tiene su consulta:
 * todas (jefe y admin), las del técnico, o las del local del cliente.
 */
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {

    List<OrdenTrabajo> findAllByOrderByFechaProgramada();

    List<OrdenTrabajo> findByTecnicoIdOrderByFechaProgramada(Long tecnicoId);

    List<OrdenTrabajo> findByLocalIdOrderByFechaProgramada(Long localId);

    long countByEquipoId(Long equipoId);

    /** OT del equipo que todavía no terminan (para no dar de baja un equipo con trabajo en curso). */
    long countByEquipoIdAndEstadoNotIn(Long equipoId, Collection<EstadoOT> terminados);

    long countByLocalId(Long localId);
}
