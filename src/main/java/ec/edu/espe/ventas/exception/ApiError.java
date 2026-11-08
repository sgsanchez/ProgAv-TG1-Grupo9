package ec.edu.espe.ventas.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo uniforme de todas las respuestas de error de la API.
 *
 * <p>Todas las excepciones, sin importar su origen, se traducen a esta misma
 * estructura para que el cliente reciba siempre la información en el mismo
 * formato.</p>
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        List<String> detalles
) {
    public static ApiError de(int status, String error, String mensaje, String ruta) {
        return new ApiError(LocalDateTime.now(), status, error, mensaje, ruta, List.of());
    }

    public static ApiError de(int status, String error, String mensaje, String ruta, List<String> detalles) {
        return new ApiError(LocalDateTime.now(), status, error, mensaje, ruta, detalles);
    }
}
