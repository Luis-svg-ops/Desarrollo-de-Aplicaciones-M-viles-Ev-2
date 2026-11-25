package cl.duoc.acis.ordenes.controller;

import cl.duoc.acis.ordenes.dto.OrdenesDtos.EquipoRequest;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.EquipoResponse;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.MensajeResponse;
import cl.duoc.acis.ordenes.service.EquipoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de equipos (jefe y admin). La lista por local está en GET /locales/{id}/equipos. */
@RestController
@RequestMapping("/equipos")
public class EquipoController {

    private final EquipoService equipos;

    public EquipoController(EquipoService equipos) {
        this.equipos = equipos;
    }

    @GetMapping("/{id}")
    public EquipoResponse ver(@PathVariable Long id) {
        return EquipoResponse.de(equipos.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse crear(@Valid @RequestBody EquipoRequest pedido) {
        return EquipoResponse.de(equipos.crear(pedido));
    }

    @PutMapping("/{id}")
    public EquipoResponse editar(@PathVariable Long id, @Valid @RequestBody EquipoRequest pedido) {
        return EquipoResponse.de(equipos.editar(id, pedido));
    }

    /** DELETE /equipos/{id} → se borra, se da de baja (si tiene historial) o 409 si tiene una OT en curso. */
    @DeleteMapping("/{id}")
    public MensajeResponse eliminar(@PathVariable Long id) {
        return new MensajeResponse(switch (equipos.eliminar(id)) {
            case ELIMINADO -> "Equipo eliminado";
            case DADO_DE_BAJA -> "El equipo tiene historial: se dio de baja";
        });
    }
}
