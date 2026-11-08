package ec.edu.espe.ventas.service;

import ec.edu.espe.ventas.aspect.Auditable;
import ec.edu.espe.ventas.dto.DetalleVentaRequest;
import ec.edu.espe.ventas.dto.RankingProductoResponse;
import ec.edu.espe.ventas.dto.VentaRequest;
import ec.edu.espe.ventas.dto.VentaResponse;
import ec.edu.espe.ventas.entity.Cliente;
import ec.edu.espe.ventas.entity.DetalleVenta;
import ec.edu.espe.ventas.entity.EstadoVenta;
import ec.edu.espe.ventas.entity.Producto;
import ec.edu.espe.ventas.entity.Venta;
import ec.edu.espe.ventas.exception.RecursoNoEncontradoException;
import ec.edu.espe.ventas.exception.ReglaNegocioException;
import ec.edu.espe.ventas.exception.StockInsuficienteException;
import ec.edu.espe.ventas.mapper.VentaMapper;
import ec.edu.espe.ventas.repository.ClienteRepository;
import ec.edu.espe.ventas.repository.ProductoRepository;
import ec.edu.espe.ventas.repository.VentaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio del proceso de ventas y facturación.
 *
 * <p>Todo el registro de una venta ocurre en una única transacción: si algún
 * producto no tiene stock suficiente, no se descuenta nada de ningún producto.</p>
 */
@Service
@Transactional
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ComprobanteService comprobanteService;
    private final BigDecimal tasaIva;

    public VentaService(VentaRepository ventaRepository,
                        ClienteRepository clienteRepository,
                        ProductoRepository productoRepository,
                        ComprobanteService comprobanteService,
                        @Value("${app.ventas.iva:0.15}") BigDecimal tasaIva) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.comprobanteService = comprobanteService;
        this.tasaIva = tasaIva;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        return ventaRepository.findAll().stream()
                .map(VentaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VentaResponse buscarPorId(Long id) {
        return VentaMapper.toResponse(obtenerConDetalles(id));
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarPorCliente(Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaVentaDesc(clienteId).stream()
                .map(VentaMapper::toResponse)
                .toList();
    }

    /**
     * Total facturado a un cliente, sin contar las ventas anuladas.
     */
    @Transactional(readOnly = true)
    public BigDecimal totalFacturadoPorCliente(Long clienteId) {
        return ventaRepository.calcularTotalFacturadoPorCliente(clienteId, EstadoVenta.ANULADA);
    }

    @Transactional(readOnly = true)
    public List<RankingProductoResponse> rankingProductos() {
        return productoRepository.findProductosMasVendidos(EstadoVenta.ANULADA).stream()
                .map(fila -> new RankingProductoResponse(
                        (String) fila[0],
                        ((Number) fila[1]).longValue()))
                .toList();
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    /**
     * Registra una venta: valida el stock, descuenta las cantidades y calcula
     * los importes en el servidor.
     */
    @Auditable(descripcion = "Registro de venta")
    public VentaResponse registrar(VentaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.clienteId()));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException(
                    "El cliente con cédula " + cliente.getCedula() + " está inactivo y no puede registrar ventas");
        }

        Map<Long, Integer> cantidades = consolidarDetalles(request.detalles());

        Venta venta = new Venta();
        venta.setNumero(siguienteNumero());
        venta.setCliente(cliente);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.REGISTRADA);

        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> linea : cantidades.entrySet()) {
            Producto producto = productoRepository.findByIdForUpdate(linea.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto", linea.getKey()));

            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new ReglaNegocioException(
                        "El producto '" + producto.getNombre() + "' está inactivo y no puede venderse");
            }

            int cantidad = linea.getValue();
            if (!producto.hayStockSuficiente(cantidad)) {
                throw new StockInsuficienteException(producto.getNombre(), cantidad, producto.getStock());
            }

            producto.setStock(producto.getStock() - cantidad);

            BigDecimal importe = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(importe);

            venta.agregarDetalle(detalle);
            subtotal = subtotal.add(importe);
        }

        aplicarImportes(venta, subtotal);

        VentaResponse respuesta = VentaMapper.toResponse(ventaRepository.save(venta));

        // Alternativa NO bloqueante: la generación del comprobante se ejecuta en
        // segundo plano y no retrasa la respuesta al cliente.
        comprobanteService.generarAsincrono(respuesta);

        return respuesta;
    }

    /**
     * Cambia el estado de una venta. Si el nuevo estado es ANULADA, delega en
     * {@link #anular(Long)} para que el stock se restituya correctamente.
     */
    @Auditable(descripcion = "Cambio de estado de venta")
    public VentaResponse cambiarEstado(Long id, EstadoVenta nuevoEstado) {
        if (nuevoEstado == null) {
            throw new ReglaNegocioException("Debe indicar el nuevo estado de la venta");
        }
        if (EstadoVenta.ANULADA.equals(nuevoEstado)) {
            return anular(id);
        }

        Venta venta = obtenerConDetalles(id);
        if (EstadoVenta.ANULADA.equals(venta.getEstado())) {
            throw new ReglaNegocioException(
                    "La venta " + venta.getNumero() + " está anulada y no puede modificarse");
        }

        venta.setEstado(nuevoEstado);
        return VentaMapper.toResponse(ventaRepository.save(venta));
    }

    /**
     * Anula una venta y devuelve al inventario todas las unidades vendidas.
     */
    @Auditable(descripcion = "Anulación de venta")
    public VentaResponse anular(Long id) {
        Venta venta = obtenerConDetalles(id);

        if (EstadoVenta.ANULADA.equals(venta.getEstado())) {
            throw new ReglaNegocioException("La venta " + venta.getNumero() + " ya se encuentra anulada");
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findByIdForUpdate(detalle.getProducto().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto", detalle.getProducto().getId()));
            producto.setStock(producto.getStock() + detalle.getCantidad());
        }

        venta.setEstado(EstadoVenta.ANULADA);
        return VentaMapper.toResponse(ventaRepository.save(venta));
    }

    // ------------------------------------------------------------------
    // Apoyo interno
    // ------------------------------------------------------------------

    private Venta obtenerConDetalles(Long id) {
        return ventaRepository.findByIdConDetalles(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));
    }

    /**
     * Une las líneas repetidas del mismo producto para validar el stock una
     * sola vez con la cantidad total solicitada.
     */
    private Map<Long, Integer> consolidarDetalles(List<DetalleVentaRequest> detalles) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DetalleVentaRequest detalle : detalles) {
            cantidades.merge(detalle.productoId(), detalle.cantidad(), Integer::sum);
        }
        return cantidades;
    }

    private void aplicarImportes(Venta venta, BigDecimal subtotal) {
        BigDecimal subtotalRedondeado = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = subtotalRedondeado.multiply(tasaIva).setScale(2, RoundingMode.HALF_UP);

        venta.setSubtotal(subtotalRedondeado);
        venta.setIva(iva);
        venta.setTotal(subtotalRedondeado.add(iva));
    }

    /**
     * Genera el número de factura consecutivo (V-0001, V-0002, ...).
     */
    private String siguienteNumero() {
        long consecutivo = ventaRepository.count();
        String candidato;
        do {
            consecutivo++;
            candidato = String.format("V-%04d", consecutivo);
        } while (ventaRepository.existsByNumero(candidato));
        return candidato;
    }
}
