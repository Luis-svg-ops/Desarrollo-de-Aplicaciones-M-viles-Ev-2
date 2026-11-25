package cl.duoc.acis.ordenes.controller;

import cl.duoc.acis.ordenes.dto.OrdenesDtos.AccionRequest;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.CheckInRequest;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.CierreRequest;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.EventoResponse;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.MensajeResponse;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.OtRequest;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.OtResponse;
import cl.duoc.acis.ordenes.service.OrdenService;
import cl.duoc.acis.ordenes.service.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de las OT. Quién es el usuario sale siempre del token (@AuthenticationPrincipal Jwt),
 * nunca de lo que mande la app.
 */
@RestController
@RequestMapping("/ordenes")
public class OrdenController {

    private final OrdenService servicio;

    public OrdenController(OrdenService servicio) {
        this.servicio = servicio;
    }

    /** GET /ordenes → las OT que le corresponden a quien pregunta. */
    @GetMapping
    public List<OtResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listar(UsuarioActual.desde(jwt)).stream().map(OtResponse::de).toList();
    }

    @GetMapping("/{id}")
    public OtResponse ver(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return OtResponse.de(servicio.ver(id, UsuarioActual.desde(jwt)));
    }

    /** GET /ordenes/{id}/historial → eventos, del más reciente al más antiguo. */
    @GetMapping("/{id}/historial")
    public List<EventoResponse> historial(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return servicio.historial(id, UsuarioActual.desde(jwt)).stream().map(EventoResponse::de).toList();
    }

    /** POST /ordenes → crear (jefe). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OtResponse crear(@Valid @RequestBody OtRequest pedido, @AuthenticationPrincipal Jwt jwt) {
        return OtResponse.de(servicio.crear(pedido, UsuarioActual.desde(jwt)));
    }

    /** PUT /ordenes/{id} → editar, asignar o reprogramar (jefe). */
    @PutMapping("/{id}")
    public OtResponse editar(@PathVariable Long id, @Valid @RequestBody OtRequest pedido, @AuthenticationPrincipal Jwt jwt) {
        return OtResponse.de(servicio.editar(id, pedido, UsuarioActual.desde(jwt)));
    }

    /** DELETE /ordenes/{id} → solo si está recién creada (jefe). */
    @DeleteMapping("/{id}")
    public MensajeResponse eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return new MensajeResponse("OT eliminada");
    }

    /** POST /ordenes/{id}/acciones → {"accion":"ACEPTAR"} o {"accion":"RECHAZAR","motivo":"..."}. */
    @PostMapping("/{id}/acciones")
    public OtResponse accion(@PathVariable Long id, @Valid @RequestBody AccionRequest pedido, @AuthenticationPrincipal Jwt jwt) {
        return OtResponse.de(servicio.aplicar(id, pedido.accion(), pedido.motivo(), UsuarioActual.desde(jwt)));
    }

    /** POST /ordenes/{id}/check-in → con las coordenadas del GPS (técnico). */
    @PostMapping("/{id}/check-in")
    public OtResponse checkIn(@PathVariable Long id, @Valid @RequestBody CheckInRequest pedido, @AuthenticationPrincipal Jwt jwt) {
        return OtResponse.de(servicio.checkIn(id, pedido, UsuarioActual.desde(jwt)));
    }

    /** POST /ordenes/{id}/cierre → diagnóstico, actividades, materiales y foto (técnico). */
    @PostMapping("/{id}/cierre")
    public OtResponse cierre(@PathVariable Long id, @Valid @RequestBody CierreRequest pedido, @AuthenticationPrincipal Jwt jwt) {
        return OtResponse.de(servicio.cerrar(id, pedido, UsuarioActual.desde(jwt)));
    }
}
