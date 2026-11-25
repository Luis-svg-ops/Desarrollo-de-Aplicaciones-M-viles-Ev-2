package cl.duoc.acis.ordenes.controller;

import cl.duoc.acis.ordenes.dto.OrdenesDtos.EquipoResponse;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.LocalRequest;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.LocalResponse;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.LocalResumen;
import cl.duoc.acis.ordenes.dto.OrdenesDtos.MensajeResponse;
import cl.duoc.acis.ordenes.exception.AcisException;
import cl.duoc.acis.ordenes.service.EquipoService;
import cl.duoc.acis.ordenes.service.LocalService;
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

/** CRUD de locales (jefe y admin) + 2 consultas especiales. */
@RestController
@RequestMapping("/locales")
public class LocalController {

    private final LocalService locales;
    private final EquipoService equipos;

    public LocalController(LocalService locales, EquipoService equipos) {
        this.locales = locales;
        this.equipos = equipos;
    }

    /** GET /locales/codigo/{codigo} → público, solo id y nombre (lo usa ms-auth en el registro). */
    @GetMapping("/codigo/{codigo}")
    public LocalResumen porCodigo(@PathVariable String codigo) {
        return locales.porCodigo(codigo)
                .map(l -> new LocalResumen(l.getId(), l.getNombre()))
                .orElseThrow(() -> new AcisException.NoEncontrado("Código de local no válido"));
    }

    /** GET /locales/mio → el local del cliente que pregunta. */
    @GetMapping("/mio")
    public LocalResponse mio(@AuthenticationPrincipal Jwt jwt) {
        return LocalResponse.de(locales.delUsuario(UsuarioActual.desde(jwt)));
    }

    @GetMapping
    public List<LocalResponse> listar() {
        return locales.listar().stream().map(LocalResponse::de).toList();
    }

    @GetMapping("/{id}")
    public LocalResponse ver(@PathVariable Long id) {
        return LocalResponse.de(locales.buscar(id));
    }

    @GetMapping("/{id}/equipos")
    public List<EquipoResponse> equipos(@PathVariable Long id) {
        return equipos.delLocal(id).stream().map(EquipoResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocalResponse crear(@Valid @RequestBody LocalRequest pedido) {
        return LocalResponse.de(locales.crear(pedido));
    }

    @PutMapping("/{id}")
    public LocalResponse editar(@PathVariable Long id, @Valid @RequestBody LocalRequest pedido) {
        return LocalResponse.de(locales.editar(id, pedido));
    }

    @DeleteMapping("/{id}")
    public MensajeResponse eliminar(@PathVariable Long id) {
        locales.eliminar(id);
        return new MensajeResponse("Local eliminado");
    }
}
