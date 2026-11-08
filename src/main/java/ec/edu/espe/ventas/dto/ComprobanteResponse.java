package ec.edu.espe.ventas.dto;

import java.math.BigDecimal;

/**
 * Comprobante generado para una venta.
 *
 * <p>Incluye el hilo que lo procesó y el tiempo que tomó, de modo que se pueda
 * comparar la ejecución bloqueante con la asíncrona.</p>
 */
public record ComprobanteResponse(
        String numero,
        String cliente,
        BigDecimal total,
        String contenido,
        long duracionMs,
        String hilo
) {
}
