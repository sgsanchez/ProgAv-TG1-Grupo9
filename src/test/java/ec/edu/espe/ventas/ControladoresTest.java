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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de los controladores REST (MockMvc sobre H2 en memoria).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControladoresTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // ------------------------------------------------------------------
    // Clientes
    // ------------------------------------------------------------------

    @Test
    void registraUnClienteYDevuelve201ConLaCabeceraLocation() throws Exception {
        ClienteRequest request = new ClienteRequest(
                "1712345678", "Ana", "Perez", "ana.perez@espe.edu.ec", "0991111111", "Quito");

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cedula").value("1712345678"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void listaClientesYFiltraPorApellido() throws Exception {
        clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));
        clienteRepository.saveAndFlush(cliente("1800000001", "Luis", "Torres"));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/clientes").param("apellido", "tor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].apellido").value("Torres"));
    }

    @Test
    void obtieneUnClientePorId() throws Exception {
        Cliente guardado = clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));

        mockMvc.perform(get("/api/clientes/{id}", guardado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cedula").value("1712345678"))
                .andExpect(jsonPath("$.email").value("ana.perez@espe.edu.ec"));
    }

    @Test
    void actualizaUnCliente() throws Exception {
        Cliente guardado = clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));
        ClienteRequest actualizado = new ClienteRequest(
                "1712345678", "Ana Maria", "Perez", "ana.perez@espe.edu.ec", "0988888888", "Quito");

        mockMvc.perform(put("/api/clientes/{id}", guardado.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Maria"))
                .andExpect(jsonPath("$.telefono").value("0988888888"));
    }

    @Test
    void eliminaUnClienteSinVentasYDevuelve204() throws Exception {
        Cliente guardado = clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));

        mockMvc.perform(delete("/api/clientes/{id}", guardado.getId()))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(clienteRepository.findById(guardado.getId()))
                .isEmpty();
    }

    // ------------------------------------------------------------------
    // Productos
    // ------------------------------------------------------------------

    @Test
    void listaProductosConStockBajo() throws Exception {
        productoRepository.saveAndFlush(new Producto("P-001", "Grapas", new BigDecimal("1.00"), 3, 10));
        productoRepository.saveAndFlush(new Producto("P-002", "Clips", new BigDecimal("1.50"), 40, 10));

        mockMvc.perform(get("/api/productos/bajo-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("P-001"))
                .andExpect(jsonPath("$[0].stockBajo").value(true));
    }

    // ------------------------------------------------------------------
    // Ventas
    // ------------------------------------------------------------------

    @Test
    void registraUnaVentaCalculaImportesYDescuentaStock() throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));
        Producto producto = productoRepository.saveAndFlush(
                new Producto("P-001", "Cuaderno", new BigDecimal("10.00"), 5, 2));

        VentaRequest request = new VentaRequest(cliente.getId(),
                List.of(new DetalleVentaRequest(producto.getId(), 2)));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.numero").value("V-0001"))
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.subtotal").value(20.0))
                .andExpect(jsonPath("$.iva").value(3.0))
                .andExpect(jsonPath("$.total").value(23.0))
                .andExpect(jsonPath("$.detalles.length()").value(1));

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(actualizado.getStock()).isEqualTo(3);
    }

    @Test
    void anulaUnaVentaYRestauraElStock() throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));
        Producto producto = productoRepository.saveAndFlush(
                new Producto("P-001", "Cuaderno", new BigDecimal("10.00"), 5, 2));

        VentaRequest request = new VentaRequest(cliente.getId(),
                List.of(new DetalleVentaRequest(producto.getId(), 2)));

        String cuerpo = mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ventaId = objectMapper.readTree(cuerpo).get("id").asLong();

        mockMvc.perform(post("/api/ventas/{id}/anular", ventaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));

        org.assertj.core.api.Assertions.assertThat(
                productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void cambiaElEstadoDeUnaVentaConPatch() throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(cliente("1712345678", "Ana", "Perez"));
        Producto producto = productoRepository.saveAndFlush(
                new Producto("P-001", "Cuaderno", new BigDecimal("10.00"), 5, 2));

        String cuerpo = mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VentaRequest(cliente.getId(),
                                List.of(new DetalleVentaRequest(producto.getId(), 1))))))
                .andReturn().getResponse().getContentAsString();

        Long ventaId = objectMapper.readTree(cuerpo).get("id").asLong();

        mockMvc.perform(patch("/api/ventas/{id}/estado", ventaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"PAGADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"));
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
