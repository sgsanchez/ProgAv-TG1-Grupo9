package ec.edu.espe.ventas;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.espe.ventas.dto.ClienteRequest;
import ec.edu.espe.ventas.dto.DetalleVentaRequest;
import ec.edu.espe.ventas.dto.VentaRequest;
import ec.edu.espe.ventas.entity.Cliente;
import ec.edu.espe.ventas.entity.Producto;
import ec.edu.espe.ventas.repository.ClienteRepository;
import ec.edu.espe.ventas.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los componentes transversales: manejo centralizado de excepciones,
 * interceptor HTTP y aspecto de auditoría.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransversalesTest {

    private static final String CLAVE = "test-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ec.edu.espe.ventas.repository.AuditoriaRepository auditoriaRepository;

    // ------------------------------------------------------------------
    // Interceptor
    // ------------------------------------------------------------------

    @Test
    void sinCabeceraApiKeyDevuelve401() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Falta la cabecera obligatoria X-API-KEY"));
    }

    @Test
    void conApiKeyIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(get("/api/clientes").header("X-API-KEY", "clave-invalida"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("El valor de la cabecera X-API-KEY no es válido"));
    }

    @Test
    void conApiKeyCorrectaRespondeYAgregaElTiempoDeEjecucion() throws Exception {
        mockMvc.perform(api(get("/api/clientes")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Response-Time"));
    }

    // ------------------------------------------------------------------
    // Manejo centralizado de excepciones
    // ------------------------------------------------------------------

    @Test
    void recursoInexistenteDevuelve404ConMensajeComprensible() throws Exception {
        mockMvc.perform(api(get("/api/clientes/{id}", 999)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Cliente no encontrado con id 999"))
                .andExpect(jsonPath("$.ruta").value("/api/clientes/999"));
    }

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(api(get("/api/no-existe")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void stockInsuficienteDevuelve409ConElDetalleDelProducto() throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));
        Producto producto = productoRepository.saveAndFlush(
                new Producto("P-001", "Cuaderno", new BigDecimal("10.00"), 2, 1));

        VentaRequest request = new VentaRequest(cliente.getId(),
                List.of(new DetalleVentaRequest(producto.getId(), 5)));

        mockMvc.perform(api(post("/api/ventas"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Stock insuficiente")))
                .andExpect(jsonPath("$.detalles[1]").value("Solicitado: 5"))
                .andExpect(jsonPath("$.detalles[2]").value("Disponible: 2"));
    }

    @Test
    void datoDuplicadoDevuelve409() throws Exception {
        ClienteRequest request = new ClienteRequest(
                "1712345678", "Ana", "Perez", "ana.perez@espe.edu.ec", null, null);

        mockMvc.perform(api(post("/api/clientes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(api(post("/api/clientes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString("Ya existe un registro con cédula")));
    }

    @Test
    void datosInvalidosDevuelven400ConElDetalleDeLosCampos() throws Exception {
        String cuerpo = "{\"cedula\":\"123\",\"nombre\":\"\",\"apellido\":\"Perez\","
                + "\"email\":\"sin-arroba\",\"telefono\":null,\"direccion\":null}";

        mockMvc.perform(api(post("/api/clientes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(api(post("/api/clientes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ------------------------------------------------------------------
    // Aspecto de auditoría
    // ------------------------------------------------------------------

    @Test
    void elAspectoRegistraLaOperacionDeVenta() throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));
        Producto producto = productoRepository.saveAndFlush(
                new Producto("P-001", "Cuaderno", new BigDecimal("10.00"), 5, 2));

        VentaRequest request = new VentaRequest(cliente.getId(),
                List.of(new DetalleVentaRequest(producto.getId(), 1)));

        mockMvc.perform(api(post("/api/ventas"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var registros = auditoriaRepository.findByOperacionContainingIgnoreCaseOrderByFechaDesc("VentaService.registrar");

        org.assertj.core.api.Assertions.assertThat(registros).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(
                registros.stream().anyMatch(r -> Boolean.TRUE.equals(r.getExitoso()))).isTrue();
        org.assertj.core.api.Assertions.assertThat(
                registros.stream().anyMatch(r -> r.getDuracionMs() != null && r.getDuracionMs() >= 0)).isTrue();
    }

    @Test
    void elAspectoRegistraTambienLasOperacionesFallidas() throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));
        Producto producto = productoRepository.saveAndFlush(
                new Producto("P-001", "Cuaderno", new BigDecimal("10.00"), 1, 1));

        VentaRequest request = new VentaRequest(cliente.getId(),
                List.of(new DetalleVentaRequest(producto.getId(), 9)));

        mockMvc.perform(api(post("/api/ventas"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        var registros = auditoriaRepository.findByOperacionContainingIgnoreCaseOrderByFechaDesc("VentaService.registrar");

        org.assertj.core.api.Assertions.assertThat(registros).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(registros.stream().anyMatch(
                r -> Boolean.FALSE.equals(r.getExitoso())
                        && r.getDetalle() != null
                        && r.getDetalle().contains("StockInsuficienteException"))).isTrue();
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder builder) {
        return builder.header("X-API-KEY", CLAVE);
    }

    private Cliente cliente(String cedula, String nombre, String apellido) {
        Cliente cliente = new Cliente();
        cliente.setCedula(cedula);
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setEmail(nombre.toLowerCase() + "." + apellido.toLowerCase() + "@espe.edu.ec");
        return cliente;
    }
}
