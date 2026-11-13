package ec.edu.espe.ventas;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.espe.ventas.dto.ClienteRequest;
import ec.edu.espe.ventas.entity.Cliente;
import ec.edu.espe.ventas.entity.EstadoVenta;
import ec.edu.espe.ventas.entity.Venta;
import ec.edu.espe.ventas.repository.ClienteRepository;
import ec.edu.espe.ventas.repository.VentaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contrato HTTP y persistencia del módulo de clientes, con datos aislados por prueba. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClientesIntegracionTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private ClienteRepository clientes;
    @Autowired private VentaRepository ventas;
    @Autowired private EntityManager em;

    @Test
    void normalizaCorreoYConservaDatosAlActualizarYRecargarDesdeLaBase() throws Exception {
        String cuerpo = mvc.perform(body(post("/api/clientes"), request("1800000001", "ANA@EXAMPLE.COM")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(cuerpo).get("id").asLong();
        em.flush();
        em.clear();
        Cliente original = clientes.findById(id).orElseThrow();
        var fecha = original.getFechaRegistro();
        original.setActivo(false);
        em.flush();
        mvc.perform(body(put("/api/clientes/{id}", id), request("1800000001", "ANA@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        em.flush();
        em.clear();
        Cliente actualizado = clientes.findById(id).orElseThrow();
        assertThat(actualizado.getFechaRegistro()).isEqualTo(fecha);
        assertThat(actualizado.getEmail()).isEqualTo("ana@example.com");
        mvc.perform(api(delete("/api/clientes/{id}", id))).andExpect(status().isNoContent());
        em.flush();
        em.clear();
        assertThat(clientes.findById(id)).isEmpty();
        mvc.perform(api(get("/api/clientes/{id}", id))).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@example.com", "ANA@EXAMPLE.COM", "Ana@Example.com"})
    void rechazaCorreoDuplicadoInclusoSiElRegistroAnteriorTieneMayusculas(String correo) throws Exception {
        guardar("1800000001", "Ana@Example.COM");
        mvc.perform(body(post("/api/clientes"), request("1800000002", correo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("correo")));
        assertThat(clientes.count()).isEqualTo(1);
    }

    @Test
    void rechazaCedulaDuplicada() throws Exception {
        guardar("1800000001", "ana@example.com");
        mvc.perform(body(post("/api/clientes"), request("1800000001", "otra@example.com")))
                .andExpect(status().isConflict());
        assertThat(clientes.count()).isEqualTo(1);
    }

    @Test
    void actualizarNoPermiteTomarCorreoNiCedulaDeOtroCliente() throws Exception {
        guardar("1800000001", "ana@example.com");
        Cliente segundo = guardar("1800000002", "otro@example.com");
        mvc.perform(body(put("/api/clientes/{id}", segundo.getId()), request("1800000002", "ANA@EXAMPLE.COM")))
                .andExpect(status().isConflict());
        mvc.perform(body(put("/api/clientes/{id}", segundo.getId()), request("1800000001", "otro@example.com")))
                .andExpect(status().isConflict());
        em.clear();
        Cliente intacto = clientes.findById(segundo.getId()).orElseThrow();
        assertThat(intacto.getCedula()).isEqualTo("1800000002");
        assertThat(intacto.getEmail()).isEqualTo("otro@example.com");
    }

    @ParameterizedTest
    @EnumSource(EstadoVenta.class)
    void protegeElHistorialDeVentasInclusoCuandoEstaAnulado(EstadoVenta estado) throws Exception {
        Cliente cliente = guardar("1800000001", "ana@example.com");
        Venta venta = new Venta();
        venta.setNumero("CLIENTE-TEST");
        venta.setCliente(cliente);
        venta.setEstado(estado);
        ventas.saveAndFlush(venta);
        mvc.perform(api(delete("/api/clientes/{id}", cliente.getId())))
                .andExpect(status().isConflict());
        em.clear();
        assertThat(clientes.existsById(cliente.getId())).isTrue();
        assertThat(ventas.existsById(venta.getId())).isTrue();
    }

    @Test
    void filtraPorApellidoConEspaciosYMayusculas() throws Exception {
        guardar("1800000001", "ana@example.com");
        mvc.perform(api(get("/api/clientes")).param("apellido", "  PeR  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(api(get("/api/clientes")).param("apellido", "sin-coincidencia"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void devuelve404ParaTodasLasOperacionesSobreUnClienteInexistente() throws Exception {
        mvc.perform(api(get("/api/clientes/999999"))).andExpect(status().isNotFound());
        mvc.perform(body(put("/api/clientes/999999"), request("1800000001", "ana@example.com")))
                .andExpect(status().isNotFound());
        mvc.perform(api(delete("/api/clientes/999999"))).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cedula", "nombre", "apellido", "email", "telefono", "direccion"})
    void validaCadaCampoAntesDePersistir(String campo) throws Exception {
        var datos = json.valueToTree(request("1800000001", "ana@example.com"));
        String invalido = switch (campo) {
            case "cedula" -> "123";
            case "email" -> "sin-arroba";
            case "telefono" -> "abc";
            case "direccion" -> "x".repeat(201);
            default -> " ";
        };
        ((com.fasterxml.jackson.databind.node.ObjectNode) datos).put(campo, invalido);
        mvc.perform(api(post("/api/clientes")).contentType(MediaType.APPLICATION_JSON).content(datos.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.startsWith(campo + ":")));
        assertThat(clientes.count()).isZero();
    }

    @Test
    void interceptorRechazaUnaCreacionSinClaveAntesDeGuardarDatos() throws Exception {
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("1800000001", "ana@example.com"))))
                .andExpect(status().isUnauthorized());
        assertThat(clientes.count()).isZero();
    }

    private Cliente guardar(String cedula, String email) {
        Cliente cliente = new Cliente();
        cliente.setCedula(cedula);
        cliente.setNombre("Ana");
        cliente.setApellido("Perez");
        cliente.setEmail(email);
        return clientes.saveAndFlush(cliente);
    }

    private ClienteRequest request(String cedula, String email) {
        return new ClienteRequest(cedula, " Ana ", " Perez ", email, null, " Quito ");
    }

    private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder builder) {
        return builder.header("X-API-KEY", "test-api-key");
    }

    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder builder, ClienteRequest request)
            throws Exception {
        return api(builder).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request));
    }
}
