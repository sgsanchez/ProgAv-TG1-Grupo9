package ec.edu.espe.ventas.dto;

import java.math.BigDecimal;

/**
 * Datos de salida de una línea de venta.
 */
public record DetalleVentaResponse(
        Long productoId,
        String codigoProducto,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
