package ec.edu.espe.ventas;

import ec.edu.espe.ventas.entity.Auditoria;
import ec.edu.espe.ventas.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la paginación del endpoint de auditoría y de la validación
 * de sus parámetros ({@code pagina} y {@code tamano}).
 *
 * <p>La clase es transaccional para que los 25 registros sembrados en cada
 * prueba se reviertan al terminar: así los totales no dependen del orden de
 * ejecución ni de lo que hayan dejado otras clases de prueba.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditoriaPaginacionTest {

    private static final String CLAVE = "test-api-key";

    @Autowired private MockMvc mvc;
    @Autowired private AuditoriaRepository auditoria;

    @BeforeEach
    void sembrar() {
        List<Auditoria> nuevos = IntStream.range(0, 25)
                .mapToObj(i -> new Auditoria("PruebaPaginacion.operacion", "Registro " + i, (long) i,
                        true, null, LocalDateTime.now().minusMinutes(i)))
                .toList();
        auditoria.saveAllAndFlush(nuevos);
    }

    @Test
    void porDefectoDevuelve20DeLaPrimeraPagina() throws Exception {
        mvc.perform(get("/api/auditoria").header("X-API-KEY", CLAVE)
                        .param("operacion", "PruebaPaginacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(20))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido.length()").value(20));
    }

    @Test
    void laSegundaPaginaTraeElResto() throws Exception {
        mvc.perform(get("/api/auditoria").header("X-API-KEY", CLAVE)
                        .param("operacion", "PruebaPaginacion").param("pagina", "1").param("tamano", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(5));
    }

    @Test
    void rechazaTamanoMayorAlMaximo() throws Exception {
        mvc.perform(get("/api/auditoria").header("X-API-KEY", CLAVE).param("tamano", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value("tamano: debe ser menor o igual a 100"));
    }

    @Test
    void rechazaPaginaNegativa() throws Exception {
        mvc.perform(get("/api/auditoria").header("X-API-KEY", CLAVE).param("pagina", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value("pagina: debe ser mayor o igual a 0"));
    }
}
