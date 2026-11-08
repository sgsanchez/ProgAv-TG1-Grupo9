package ec.edu.espe.ventas.dto;

/**
 * Resultado del ranking de productos más vendidos.
 */
public record RankingProductoResponse(
        String producto,
        Long unidadesVendidas
) {
}
