package cl.duoc.acis.ordenes.service;

import cl.duoc.acis.ordenes.dto.OrdenesDtos.LocalRequest;
import cl.duoc.acis.ordenes.exception.AcisException;
import cl.duoc.acis.ordenes.model.Local;
import cl.duoc.acis.ordenes.repository.EquipoRepository;
import cl.duoc.acis.ordenes.repository.LocalRepository;
import cl.duoc.acis.ordenes.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** CRUD de locales. */
@Service
public class LocalService {

    private final LocalRepository locales;
    private final EquipoRepository equipos;
    private final OrdenTrabajoRepository ordenes;

    public LocalService(LocalRepository locales, EquipoRepository equipos, OrdenTrabajoRepository ordenes) {
        this.locales = locales;
        this.equipos = equipos;
        this.ordenes = ordenes;
    }

    public List<Local> listar() {
        return locales.findAllByOrderByNombre();
    }

    public Local buscar(Long id) {
        return locales.findById(id).orElseThrow(() -> new AcisException.NoEncontrado("Local no encontrado"));
    }

    public Optional<Local> porCodigo(String codigo) {
        return locales.findByCodigo(codigo.trim().toUpperCase());
    }

    /** El cliente solo puede ver su propio local. */
    public Local delUsuario(UsuarioActual usuario) {
        if (usuario.localId() == null) {
            throw new AcisException.NoEncontrado("No tienes un local asociado");
        }
        return buscar(usuario.localId());
    }

    @Transactional
    public Local crear(LocalRequest p) {
        if (locales.existsByCodigo(p.codigo())) {
            throw new AcisException.Conflicto("Ya existe un local con ese código");
        }
        return locales.save(new Local(p.codigo(), p.nombre().trim(), p.direccion().trim(), p.comuna().trim(),
                p.nombreAdministrador().trim(), p.telefono().trim(), p.latitud(), p.longitud()));
    }

    @Transactional
    public Local editar(Long id, LocalRequest p) {
        Local local = buscar(id);
        locales.findByCodigo(p.codigo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> { throw new AcisException.Conflicto("Ya existe un local con ese código"); });
        local.actualizar(p.codigo(), p.nombre().trim(), p.direccion().trim(), p.comuna().trim(),
                p.nombreAdministrador().trim(), p.telefono().trim(), p.latitud(), p.longitud());
        return local;
    }

    /** Un local con equipos u OT no se borra: perderíamos el historial. */
    @Transactional
    public void eliminar(Long id) {
        Local local = buscar(id);
        if (equipos.countByLocalId(id) > 0 || ordenes.countByLocalId(id) > 0) {
            throw new AcisException.Conflicto("El local tiene equipos u OT registradas: no se puede eliminar");
        }
        locales.delete(local);
    }
}
