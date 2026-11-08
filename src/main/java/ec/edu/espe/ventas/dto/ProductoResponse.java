package ec.edu.espe.ventas.dto;

import java.math.BigDecimal;

/**
 * Datos de salida de un producto.
 */
public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        Integer stockMinimo,
        Boolean activo,
        Boolean stockBajo
) {
}
