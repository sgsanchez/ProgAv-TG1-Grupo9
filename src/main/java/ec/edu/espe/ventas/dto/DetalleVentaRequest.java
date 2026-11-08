package ec.edu.espe.ventas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Línea de una venta solicitada por el cliente.
 *
 * <p>El precio no se recibe desde el cliente: lo toma el servidor del producto,
 * de modo que no se pueda manipular el importe de la factura.</p>
 */
public record DetalleVentaRequest(

        @NotNull(message = "El producto es obligatorio")
        @Positive(message = "El identificador del producto debe ser positivo")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad
) {
}
