package ec.edu.espe.ventas.mapper;

import ec.edu.espe.ventas.dto.DetalleVentaResponse;
import ec.edu.espe.ventas.dto.VentaResponse;
import ec.edu.espe.ventas.entity.DetalleVenta;
import ec.edu.espe.ventas.entity.Venta;
import java.util.List;

/**
 * Conversión entre {@link Venta} y sus DTOs.
 */
public final class VentaMapper {

    private VentaMapper() {
        // Clase utilitaria
    }

    public static VentaResponse toResponse(Venta venta) {
        if (venta == null) {
            return null;
        }

        List<DetalleVentaResponse> detalles = venta.getDetalles() == null
                ? List.of()
                : venta.getDetalles().stream().map(VentaMapper::toDetalle).toList();

        return new VentaResponse(
                venta.getId(),
                venta.getNumero(),
                venta.getCliente() != null ? venta.getCliente().getId() : null,
                venta.getCliente() != null ? nombreCompleto(venta) : null,
                venta.getFechaVenta(),
                venta.getSubtotal(),
                venta.getIva(),
                venta.getTotal(),
                venta.getEstado(),
                detalles);
    }

    public static DetalleVentaResponse toDetalle(DetalleVenta detalle) {
        if (detalle == null) {
            return null;
        }
        return new DetalleVentaResponse(
                detalle.getProducto() != null ? detalle.getProducto().getId() : null,
                detalle.getProducto() != null ? detalle.getProducto().getCodigo() : null,
                detalle.getProducto() != null ? detalle.getProducto().getNombre() : null,
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal());
    }

    private static String nombreCompleto(Venta venta) {
        return venta.getCliente().getNombre() + " " + venta.getCliente().getApellido();
    }
}
