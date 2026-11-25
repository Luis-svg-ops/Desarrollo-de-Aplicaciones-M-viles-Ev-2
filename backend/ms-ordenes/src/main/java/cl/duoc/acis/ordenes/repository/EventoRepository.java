package cl.duoc.acis.ordenes.repository;

import cl.duoc.acis.ordenes.model.EventoOT;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso al historial de las OT. */
public interface EventoRepository extends JpaRepository<EventoOT, Long> {

    List<EventoOT> findByOtIdOrderByFechaDescIdDesc(Long otId);

    void deleteByOtId(Long otId);
}
