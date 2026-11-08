package ec.edu.espe.ventas.dto;

import ec.edu.espe.ventas.entity.EstadoVenta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de salida de una venta, incluyendo sus detalles.
 */
public record VentaResponse(
        Long id,
        String numero,
        Long clienteId,
        String cliente,
        LocalDateTime fechaVenta,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        EstadoVenta estado,
        List<DetalleVentaResponse> detalles
) {
}
