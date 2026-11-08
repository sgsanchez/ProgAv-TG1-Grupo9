package ec.edu.espe.ventas;

import ec.edu.espe.ventas.dto.ClienteRequest;
import ec.edu.espe.ventas.dto.DetalleVentaRequest;
import ec.edu.espe.ventas.dto.VentaRequest;
import ec.edu.espe.ventas.dto.VentaResponse;
import ec.edu.espe.ventas.entity.Cliente;
import ec.edu.espe.ventas.entity.EstadoVenta;
import ec.edu.espe.ventas.entity.Producto;
import ec.edu.espe.ventas.exception.RecursoDuplicadoException;
import ec.edu.espe.ventas.exception.ReglaNegocioException;
import ec.edu.espe.ventas.exception.StockInsuficienteException;
import ec.edu.espe.ventas.repository.ClienteRepository;
import ec.edu.espe.ventas.repository.ProductoRepository;
import ec.edu.espe.ventas.service.ClienteService;
import ec.edu.espe.ventas.service.ProductoService;
import ec.edu.espe.ventas.service.VentaService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de las reglas de negocio de la capa de servicios.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServicioVentasTest {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    private Cliente cliente;
    private Producto producto;

    @BeforeEach
    void prepararDatos() {
        Cliente nuevo = new Cliente();
        nuevo.setCedula("1712345678");
        nuevo.setNombre("Ana");
        nuevo.setApellido("Perez");
        nuevo.setEmail("ana.perez@espe.edu.ec");
        cliente = clienteRepository.saveAndFlush(nuevo);

        producto = productoRepository.saveAndFlush(
                new Producto("P-001", "Cuaderno", new BigDecimal("10.00"), 5, 2));
    }

    @Test
    void registraVentaDescuentaStockYCalculaTotalesEnElServidor() {
        VentaResponse respuesta = ventaService.registrar(ventaDe(2));

        assertThat(respuesta.numero()).isEqualTo("V-0001");
        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.REGISTRADA);
        assertThat(respuesta.subtotal()).isEqualByComparingTo("20.00");
        assertThat(respuesta.iva()).isEqualByComparingTo("3.00");
        assertThat(respuesta.total()).isEqualByComparingTo("23.00");
        assertThat(respuesta.detalles()).hasSize(1);

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertThat(actualizado.getStock()).isEqualTo(3);
    }

    @Test
    void rechazaVentaCuandoNoHayStockSuficienteYNoDescuentaNada() {
        VentaRequest pedido = ventaDe(10);

        StockInsuficienteException error = assertThrows(StockInsuficienteException.class,
                () -> ventaService.registrar(pedido));

        assertThat(error.getSolicitado()).isEqualTo(10);
        assertThat(error.getDisponible()).isEqualTo(5);

        Producto sinCambios = productoRepository.findById(producto.getId()).orElseThrow();
        assertThat(sinCambios.getStock()).isEqualTo(5);
    }

    @Test
    void sumaLasLineasRepetidasDelMismoProductoAntesDeValidarStock() {
        VentaRequest pedido = new VentaRequest(cliente.getId(), List.of(
                new DetalleVentaRequest(producto.getId(), 3),
                new DetalleVentaRequest(producto.getId(), 3)));

        StockInsuficienteException error = assertThrows(StockInsuficienteException.class,
                () -> ventaService.registrar(pedido));

        assertThat(error.getSolicitado()).isEqualTo(6);
    }

    @Test
    void anularVentaRestauraElStock() {
        VentaResponse creada = ventaService.registrar(ventaDe(2));
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(3);

        VentaResponse anulada = ventaService.anular(creada.id());

        assertThat(anulada.estado()).isEqualTo(EstadoVenta.ANULADA);
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void noPermiteAnularDosVecesLaMismaVenta() {
        VentaResponse creada = ventaService.registrar(ventaDe(1));
        ventaService.anular(creada.id());

        assertThrows(ReglaNegocioException.class, () -> ventaService.anular(creada.id()));
    }

    @Test
    void noPermiteModificarUnaVentaAnulada() {
        VentaResponse creada = ventaService.registrar(ventaDe(1));
        ventaService.anular(creada.id());

        assertThrows(ReglaNegocioException.class,
                () -> ventaService.cambiarEstado(creada.id(), EstadoVenta.PAGADA));
    }

    @Test
    void elTotalFacturadoExcluyeLasVentasAnuladas() {
        VentaResponse primera = ventaService.registrar(ventaDe(1));
        ventaService.registrar(ventaDe(2));
        ventaService.anular(primera.id());

        BigDecimal total = ventaService.totalFacturadoPorCliente(cliente.getId());

        // Solo cuenta la venta vigente: 2 x 10.00 + IVA 15% = 23.00
        assertThat(total).isEqualByComparingTo("23.00");
    }

    @Test
    void noPermiteEliminarUnClienteConVentasRegistradas() {
        ventaService.registrar(ventaDe(1));

        assertThrows(ReglaNegocioException.class, () -> clienteService.eliminar(cliente.getId()));
    }

    @Test
    void noPermiteEliminarUnProductoQueYaFueVendido() {
        ventaService.registrar(ventaDe(1));

        assertThrows(ReglaNegocioException.class, () -> productoService.eliminar(producto.getId()));
    }

    @Test
    void noPermiteRegistrarDosClientesConLaMismaCedula() {
        ClienteRequest duplicado = new ClienteRequest(
                "1712345678", "Otra", "Persona", "otra@espe.edu.ec", null, null);

        assertThrows(RecursoDuplicadoException.class, () -> clienteService.crear(duplicado));
    }

    @Test
    void registraElClienteYDevuelveSuIdentificador() {
        ClienteRequest request = new ClienteRequest(
                "1800000001", "Luis", "Torres", "luis.torres@espe.edu.ec", "0992222222", "Quito");

        var creado = clienteService.crear(request);

        assertThat(creado.id()).isNotNull();
        assertThat(creado.activo()).isTrue();
        assertThat(creado.fechaRegistro()).isNotNull();
    }

    private VentaRequest ventaDe(int cantidad) {
        return new VentaRequest(cliente.getId(), List.of(new DetalleVentaRequest(producto.getId(), cantidad)));
    }
}
