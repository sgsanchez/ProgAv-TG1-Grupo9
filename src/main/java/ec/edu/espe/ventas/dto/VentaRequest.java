package ec.edu.espe.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Datos de entrada para registrar una venta.
 *
 * <p>Los importes (subtotal, IVA y total) los calcula el servidor.</p>
 */
public record VentaRequest(

        @NotNull(message = "El cliente es obligatorio")
        @Positive(message = "El identificador del cliente debe ser positivo")
        Long clienteId,

        @NotEmpty(message = "La venta debe contener al menos un producto")
        @Valid
        List<DetalleVentaRequest> detalles
) {
}
