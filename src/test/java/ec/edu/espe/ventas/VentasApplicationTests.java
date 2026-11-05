package ec.edu.espe.ventas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifica que el contexto de Spring Boot se levante correctamente
 * con el perfil de pruebas (H2 en memoria).
 */
@SpringBootTest
@ActiveProfiles("test")
class VentasApplicationTests {

    @Test
    void contextLoads() {
        assertNotNull(VentasApplication.class);
    }
}
