package ec.edu.espe.ventas.mapper;

import ec.edu.espe.ventas.dto.ProductoRequest;
import ec.edu.espe.ventas.dto.ProductoResponse;
import ec.edu.espe.ventas.entity.Producto;

/**
 * Conversión entre {@link Producto} y sus DTOs.
 */
public final class ProductoMapper {

    private ProductoMapper() {
        // Clase utilitaria
    }

    public static Producto toEntity(ProductoRequest request) {
        Producto producto = new Producto();
        actualizarEntidad(producto, request);
        return producto;
    }

    public static void actualizarEntidad(Producto producto, ProductoRequest request) {
        producto.setCodigo(limpiar(request.codigo()));
        producto.setNombre(limpiar(request.nombre()));
        producto.setDescripcion(limpiar(request.descripcion()));
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setStockMinimo(request.stockMinimo());
    }

    public static ProductoResponse toResponse(Producto producto) {
        if (producto == null) {
            return null;
        }
        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getStockMinimo(),
                producto.getActivo(),
                producto.tieneStockBajo());
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
