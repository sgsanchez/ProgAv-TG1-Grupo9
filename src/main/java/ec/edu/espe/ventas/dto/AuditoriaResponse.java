package ec.edu.espe.ventas.dto;

import java.time.LocalDateTime;

/**
 * Datos de salida de un registro de auditoría.
 */
public record AuditoriaResponse(
        Long id,
        String operacion,
        String descripcion,
        Long duracionMs,
        Boolean exitoso,
        String detalle,
        LocalDateTime fecha
) {
}
