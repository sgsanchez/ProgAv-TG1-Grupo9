package ec.edu.espe.ventas.dto;

import ec.edu.espe.ventas.entity.EstadoVenta;
import jakarta.validation.constraints.NotNull;

/**
 * Datos de entrada para cambiar el estado de una venta (PATCH).
 */
public record CambioEstadoVentaRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoVenta estado
) {
}
