package cl.duoc.acis.ordenes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Convierte cualquier error en una respuesta JSON clara: {"status": 409, "error": "Ese correo ya está registrado"}.
 * Así la app siempre recibe el mismo formato y puede mostrar el mensaje tal cual.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Errores de negocio: cada subclase trae su propio código HTTP (polimorfismo). */
    @ExceptionHandler(AcisException.class)
    ResponseEntity<Map<String, Object>> negocio(AcisException e) {
        return error(e.estadoHttp(), e.getMessage());
    }

    /** Falla una anotación de validación (@NotBlank, @Email...): se devuelve el primer mensaje. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("La solicitud no es válida");
        return error(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** El JSON viene mal formado o con un valor que no existe (ej. rol "SUPERUSUARIO"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> jsonInvalido() {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", estado.value(),
                "error", mensaje));
    }
}
