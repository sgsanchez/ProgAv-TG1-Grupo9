package ec.edu.espe.ventas;

import ec.edu.espe.ventas.dto.ComprobanteResponse;
import ec.edu.espe.ventas.dto.DetalleVentaRequest;
import ec.edu.espe.ventas.dto.VentaRequest;
import ec.edu.espe.ventas.dto.VentaResponse;
import ec.edu.espe.ventas.entity.Cliente;
import ec.edu.espe.ventas.entity.Producto;
import ec.edu.espe.ventas.repository.ClienteRepository;
import ec.edu.espe.ventas.repository.ProductoRepository;
import ec.edu.espe.ventas.service.ComprobanteService;
import ec.edu.espe.ventas.service.VentaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del procesamiento bloqueante frente a la alternativa asíncrona.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AsincroniaTest {

    private static final String CLAVE = "test-api-key";

    @Autowired
    private ComprobanteService comprobanteService;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void laVersionBloqueanteUsaElHiloDeLaPeticion() {
        VentaResponse venta = ventaDePrueba();

        ComprobanteResponse comprobante = comprobanteService.generarBloqueante(venta);

        assertThat(comprobante.hilo()).isEqualTo(Thread.currentThread().getName());
        assertThat(comprobante.contenido()).contains(venta.numero());
        assertThat(comprobante.total()).isEqualByComparingTo(venta.total());
    }

    @Test
    void laVersionAsincronaSeEjecutaEnUnHiloDelPool() throws Exception {
        VentaResponse venta = ventaDePrueba();

        CompletableFuture<ComprobanteResponse> futuro = comprobanteService.generarAsincrono(venta);
        ComprobanteResponse comprobante = futuro.get(5, TimeUnit.SECONDS);

        assertThat(comprobante.hilo()).startsWith("ventas-async-");
        assertThat(comprobante.numero()).isEqualTo(venta.numero());
    }

    @Test
    void elEndpointBloqueanteDevuelveElComprobanteTerminado() throws Exception {
        VentaResponse venta = ventaDePrueba();

        mockMvc.perform(get("/api/ventas/{id}/comprobante", venta.id())
                        .header("X-API-KEY", CLAVE)
                        .param("modo", "bloqueante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(venta.numero()))
                .andExpect(jsonPath("$.contenido").exists())
                .andExpect(jsonPath("$.hilo").exists());
    }

    @Test
    void elEndpointAsincronoResponde202DeInmediato() throws Exception {
        VentaResponse venta = ventaDePrueba();

        mockMvc.perform(get("/api/ventas/{id}/comprobante", venta.id())
                        .header("X-API-KEY", CLAVE)
                        .param("modo", "asincrono"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"))
                .andExpect(jsonPath("$.numero").value(venta.numero()));
    }

    @Test
    void registrarUnaVentaNoEsperaLaGeneracionDelComprobante() {
        Cliente cliente = clienteRepository.saveAndFlush(cliente());
        Producto producto = productoRepository.saveAndFlush(producto());
        VentaRequest request = new VentaRequest(cliente.getId(),
                List.of(new DetalleVentaRequest(producto.getId(), 1)));

        long inicio = System.currentTimeMillis();
        VentaResponse venta = ventaService.registrar(request);
        long duracion = System.currentTimeMillis() - inicio;

        assertThat(venta.id()).isNotNull();
        // Con el retardo desactivado en pruebas, la venta no debe quedar esperando
        // el procesamiento del comprobante.
        assertThat(duracion).isLessThan(2000L);
    }

    private VentaResponse ventaDePrueba() {
        Cliente cliente = clienteRepository.saveAndFlush(cliente());
        Producto producto = productoRepository.saveAndFlush(producto());
        return ventaService.registrar(new VentaRequest(cliente.getId(),
                List.of(new DetalleVentaRequest(producto.getId(), 2))));
    }

    private Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setCedula("1712345678");
        cliente.setNombre("Ana");
        cliente.setApellido("Perez");
        cliente.setEmail("ana.perez@espe.edu.ec");
        return cliente;
    }

    private Producto producto() {
        return new Producto("P-001", "Cuaderno", new BigDecimal("10.00"), 50, 5);
    }
}
