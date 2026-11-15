package ec.edu.espe.ventas;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.espe.ventas.dto.ClienteRequest;
import ec.edu.espe.ventas.repository.AuditoriaRepository;
import ec.edu.espe.ventas.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas ampliadas de los componentes transversales: interceptor de API key,
 * manejo centralizado de excepciones y aspecto de auditoría.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransversalesAmpliadoTest {

    private static final String CLAVE = "test-api-key";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private ClienteRepository clientes;
    @Autowired private AuditoriaRepository auditoria;

    private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder b) {
        return b.header("X-API-KEY", CLAVE);
    }

    private ClienteRequest request(String cedula, String email) {
        return new ClienteRequest(cedula, "Ana", "Perez", email, null, null);
    }

    @Test
    void apiKeyConEspaciosEsRechazada() throws Exception {
        mvc.perform(get("/api/clientes").header("X-API-KEY", CLAVE + " "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("El valor de la cabecera X-API-KEY no es válido"));
    }

    @Test
    void elNombreDeLaCabeceraNoDistingueMayusculas() throws Exception {
        mvc.perform(get("/api/clientes").header("x-api-key", CLAVE)).andExpect(status().isOk());
    }

    @Test
    void swaggerNoRequiereApiKey() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void auditoriaSinApiKeyDevuelve401() throws Exception {
        mvc.perform(get("/api/auditoria")).andExpect(status().isUnauthorized());
    }

    @Test
    void idNoNumericoDevuelve400() throws Exception {
        mvc.perform(api(get("/api/clientes/{id}", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'id' tiene un valor no válido"));
    }

    @Test
    void todasLasRespuestasDeErrorTienenLaMismaEstructura() throws Exception {
        mvc.perform(api(get("/api/clientes/{id}", 999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.ruta").exists())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void elAspectoRegistraLaCreacionDeCliente() throws Exception {
        mvc.perform(api(post("/api/clientes")).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("1712345678", "ana@espe.edu.ec"))))
                .andExpect(status().isCreated());
        var r = auditoria.findByOperacionContainingIgnoreCaseOrderByFechaDesc("ClienteService.crear");
        assertThat(r).isNotEmpty();
        assertThat(r.get(0).getExitoso()).isTrue();
        assertThat(r.get(0).getDuracionMs()).isNotNegative();
        assertThat(r.get(0).getDescripcion()).isEqualTo("Registro de cliente");
    }

    @Test
    void elAspectoRegistraElFalloAunqueSeRevierta() throws Exception {
        mvc.perform(api(post("/api/clientes")).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("1712345678", "ana@espe.edu.ec"))))
                .andExpect(status().isCreated());
        mvc.perform(api(post("/api/clientes")).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("0900000001", "ana@espe.edu.ec"))))
                .andExpect(status().isConflict());
        var r = auditoria.findByOperacionContainingIgnoreCaseOrderByFechaDesc("ClienteService.crear");
        assertThat(r.stream().anyMatch(x -> Boolean.FALSE.equals(x.getExitoso())
                && x.getDetalle() != null && x.getDetalle().contains("RecursoDuplicadoException"))).isTrue();
        assertThat(clientes.count()).isEqualTo(1);
    }

    @Test
    void lasConsultasNoSeAuditan() throws Exception {
        long antes = auditoria.count();
        mvc.perform(api(get("/api/clientes"))).andExpect(status().isOk());
        assertThat(auditoria.count()).isEqualTo(antes);
    }

    @Test
    void metodoHttpNoSoportadoDevuelve405ConCabeceraAllow() throws Exception {
        mvc.perform(api(put("/api/ventas")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.mensaje")
                        .value("El método PUT no está permitido en esta ruta"));
    }

    @Test
    void tipoDeContenidoNoSoportadoDevuelve415() throws Exception {
        mvc.perform(api(post("/api/clientes")).contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.mensaje")
                        .value("El tipo de contenido de la petición no es compatible; envíe application/json"));
    }

    @Test
    void estadoFueraDelDominioDevuelve400ConMensajeClaro() throws Exception {
        mvc.perform(api(patch("/api/ventas/{id}/estado", 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"PENDIENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El valor 'PENDIENTE' no es válido para el campo 'estado'"))
                .andExpect(jsonPath("$.detalles[0]", containsString("REGISTRADA")));
    }
}
