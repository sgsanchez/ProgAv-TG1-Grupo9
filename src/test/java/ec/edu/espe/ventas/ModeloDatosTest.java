package ec.edu.espe.ventas;

import ec.edu.espe.ventas.entity.Cliente;
import ec.edu.espe.ventas.entity.DetalleVenta;
import ec.edu.espe.ventas.entity.EstadoVenta;
import ec.edu.espe.ventas.entity.Producto;
import ec.edu.espe.ventas.entity.Venta;
import ec.edu.espe.ventas.repository.ClienteRepository;
import ec.edu.espe.ventas.repository.ProductoRepository;
import ec.edu.espe.ventas.repository.VentaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Valida el modelo de datos: relaciones, restricciones de integridad y
 * consultas personalizadas, sobre una base de datos real (H2 en memoria).
 */
@DataJpaTest
@ActiveProfiles("test")
class ModeloDatosTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Test
    void guardaVentaConDetallesYRecuperaLaRelacionCompleta() {
        Cliente cliente = clienteRepository.saveAndFlush(cliente("1712345678", "ana@espe.edu.ec"));
        Producto cuaderno = productoRepository.saveAndFlush(producto("P-001", "Cuaderno", "10.00", 50, 10));
        Producto lapiz = productoRepository.saveAndFlush(producto("P-002", "Lapiz", "15.50", 30, 5));

        Venta venta = new Venta();
        venta.setNumero("V-0001");
        venta.setCliente(cliente);

        // 2 x 10.00 = 20.00 y 2 x 15.50 = 31.00
        venta.agregarDetalle(detalle(cuaderno, 2));
        venta.agregarDetalle(detalle(lapiz, 2));

        BigDecimal subtotal = new BigDecimal("51.00");
        BigDecimal iva = subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        venta.setSubtotal(subtotal);
        venta.setIva(iva);
        venta.setTotal(subtotal.add(iva));

        Venta guardada = ventaRepository.saveAndFlush(venta);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getEstado()).isEqualTo(EstadoVenta.REGISTRADA);

        Venta recuperada = ventaRepository.findByIdConDetalles(guardada.getId()).orElseThrow();
        assertThat(recuperada.getDetalles()).hasSize(2);
        assertThat(recuperada.getTotal()).isEqualByComparingTo("58.65");
        assertThat(recuperada.getCliente().getCedula()).isEqualTo("1712345678");
    }

    @Test
    void rechazaCodigoDeProductoDuplicado() {
        productoRepository.saveAndFlush(producto("P-100", "Borrador", "2.50", 20, 5));

        Producto duplicado = producto("P-100", "Otro borrador", "3.00", 10, 2);

        assertThrows(DataIntegrityViolationException.class,
                () -> productoRepository.saveAndFlush(duplicado));
    }

    @Test
    void rechazaCedulaDeClienteDuplicada() {
        clienteRepository.saveAndFlush(cliente("1800000001", "uno@espe.edu.ec"));

        Cliente duplicado = cliente("1800000001", "dos@espe.edu.ec");

        assertThrows(DataIntegrityViolationException.class,
                () -> clienteRepository.saveAndFlush(duplicado));
    }

    @Test
    void encuentraProductosEnNivelDeStockBajo() {
        productoRepository.saveAndFlush(producto("P-200", "Grapas", "1.00", 3, 10));
        productoRepository.saveAndFlush(producto("P-201", "Clips", "1.50", 40, 10));

        List<Producto> bajoStock = productoRepository.findProductosBajoStock();

        assertThat(bajoStock).hasSize(1);
        assertThat(bajoStock.get(0).getCodigo()).isEqualTo("P-200");
        assertThat(bajoStock.get(0).tieneStockBajo()).isTrue();
    }

    @Test
    void calculaTotalFacturadoExcluyendoVentasAnuladas() {
        Cliente cliente = clienteRepository.saveAndFlush(cliente("1800000002", "tres@espe.edu.ec"));
        Producto producto = productoRepository.saveAndFlush(producto("P-300", "Resma", "4.00", 100, 10));

        // 40.00 + IVA 15% = 46.00 vigente; la venta anulada no debe sumar
        ventaConTotal(cliente, producto, "V-0002", new BigDecimal("40.00"), EstadoVenta.PAGADA);
        ventaConTotal(cliente, producto, "V-0003", new BigDecimal("25.00"), EstadoVenta.ANULADA);

        BigDecimal total = ventaRepository.calcularTotalFacturadoPorCliente(
                cliente.getId(), EstadoVenta.ANULADA);

        assertThat(total).isEqualByComparingTo("46.00");
    }

    // ------------------------------------------------------------------
    // Datos de apoyo para las pruebas
    // ------------------------------------------------------------------

    private Cliente cliente(String cedula, String email) {
        Cliente cliente = new Cliente();
        cliente.setCedula(cedula);
        cliente.setNombre("Ana");
        cliente.setApellido("Perez");
        cliente.setEmail(email);
        cliente.setTelefono("0999999999");
        cliente.setDireccion("Av. General Ruminahui");
        return cliente;
    }

    private Producto producto(String codigo, String nombre, String precio, int stock, int stockMinimo) {
        return new Producto(codigo, nombre, new BigDecimal(precio), stock, stockMinimo);
    }

    private DetalleVenta detalle(Producto producto, int cantidad) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(producto.getPrecio());
        detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
        return detalle;
    }

    private Venta ventaConTotal(Cliente cliente, Producto producto, String numero,
                                BigDecimal total, EstadoVenta estado) {
        Venta venta = new Venta();
        venta.setNumero(numero);
        venta.setCliente(cliente);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(estado);
        venta.agregarDetalle(detalle(producto, 1));
        BigDecimal iva = total.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        venta.setSubtotal(total);
        venta.setIva(iva);
        venta.setTotal(total.add(iva));
        return ventaRepository.saveAndFlush(venta);
    }
}
