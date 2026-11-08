package ec.edu.espe.ventas.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejo centralizado de excepciones: convierte cualquier error en una respuesta
 * HTTP coherente con el código de estado adecuado y un mensaje comprensible.
 *
 * <p>Se declara con la máxima precedencia para que tenga prioridad sobre el
 * manejador de errores por defecto de Spring Boot.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Recurso inexistente → 404. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> recursoNoEncontrado(RecursoNoEncontradoException ex,
                                                        HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /** Operación sobre ruta inexistente → 404. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rutaNoEncontrada(NoResourceFoundException ex,
                                                     HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND,
                "La ruta solicitada no existe: " + request.getRequestURI(), request, null);
    }

    /** Stock insuficiente → 409. */
    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ApiError> stockInsuficiente(StockInsuficienteException ex,
                                                      HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), request, List.of(
                "Producto: " + ex.getProducto(),
                "Solicitado: " + ex.getSolicitado(),
                "Disponible: " + ex.getDisponible()));
    }

    /** Violación de regla de negocio → 409. */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /** Dato duplicado → 409. */
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ApiError> recursoDuplicado(RecursoDuplicadoException ex,
                                                     HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /** Restricción de integridad de la base de datos → 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(DataIntegrityViolationException ex,
                                               HttpServletRequest request) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return respuesta(HttpStatus.CONFLICT,
                "La operación viola una restricción de integridad de los datos", request, null);
    }

    /** Errores de validación del cuerpo de la petición → 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex,
                                               HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos", request, detalles);
    }

    /** Errores de validación sobre parámetros sueltos → 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> validacionParametros(ConstraintViolationException ex,
                                                         HttpServletRequest request) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, "Los parámetros enviados no son válidos", request, detalles);
    }

    /** JSON mal formado o con tipos incorrectos → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException ex,
                                                   HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición está vacío o no tiene un formato JSON válido", request, null);
    }

    /** Parámetro de ruta con tipo incorrecto (por ejemplo un id no numérico) → 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametroInvalido(MethodArgumentTypeMismatchException ex,
                                                      HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor no válido", request, null);
    }

    /** Cualquier otro error no previsto → 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> errorInterno(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno al procesar la solicitud", request, null);
    }

    private ResponseEntity<ApiError> respuesta(HttpStatus status, String mensaje,
                                               HttpServletRequest request, List<String> detalles) {
        ApiError cuerpo = ApiError.de(status.value(), status.getReasonPhrase(), mensaje,
                request.getRequestURI(), detalles == null ? List.of() : detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
