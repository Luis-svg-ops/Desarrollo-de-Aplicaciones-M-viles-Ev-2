package cl.duoc.acis.ordenes.service;

import cl.duoc.acis.ordenes.dto.OrdenesDtos.EquipoRequest;
import cl.duoc.acis.ordenes.exception.AcisException;
import cl.duoc.acis.ordenes.model.Equipo;
import cl.duoc.acis.ordenes.model.Local;
import cl.duoc.acis.ordenes.model.MaquinaEstadosOT;
import cl.duoc.acis.ordenes.repository.EquipoRepository;
import cl.duoc.acis.ordenes.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD de equipos, con la misma regla de borrado que la app. */
@Service
public class EquipoService {

    /** Resultado del DELETE, para mostrar el mensaje correcto. */
    public enum ResultadoBorrado { ELIMINADO, DADO_DE_BAJA }

    private final EquipoRepository equipos;
    private final OrdenTrabajoRepository ordenes;
    private final LocalService locales;

    public EquipoService(EquipoRepository equipos, OrdenTrabajoRepository ordenes, LocalService locales) {
        this.equipos = equipos;
        this.ordenes = ordenes;
        this.locales = locales;
    }

    public List<Equipo> delLocal(Long localId) {
        locales.buscar(localId);
        return equipos.findByLocalIdOrderByMarca(localId);
    }

    public Equipo buscar(Long id) {
        return equipos.findById(id).orElseThrow(() -> new AcisException.NoEncontrado("Equipo no encontrado"));
    }

    @Transactional
    public Equipo crear(EquipoRequest p) {
        Local local = locales.buscar(p.localId());
        return equipos.save(new Equipo(local, p.tipo(), p.marca().trim(), p.modelo().trim(), p.ubicacion(),
                p.fechaUltimaMantencion()));
    }

    @Transactional
    public Equipo editar(Long id, EquipoRequest p) {
        Equipo equipo = buscar(id);
        equipo.actualizar(p.tipo(), p.marca().trim(), p.modelo().trim(), p.ubicacion(), p.fechaUltimaMantencion());
        return equipo;
    }

    /**
     * Con una OT en curso no se toca. Con OT terminadas se da de baja (se conserva el historial).
     * Sin ninguna OT se borra.
     */
    @Transactional
    public ResultadoBorrado eliminar(Long id) {
        Equipo equipo = buscar(id);
        if (ordenes.countByEquipoIdAndEstadoNotIn(id, MaquinaEstadosOT.FINALES) > 0) {
            throw new AcisException.Conflicto("El equipo tiene una OT en curso: termínala o cancélala primero");
        }
        if (ordenes.countByEquipoId(id) > 0) {
            equipo.darDeBaja();
            return ResultadoBorrado.DADO_DE_BAJA;
        }
        equipos.delete(equipo);
        return ResultadoBorrado.ELIMINADO;
    }
}
