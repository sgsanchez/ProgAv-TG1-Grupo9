package ec.edu.espe.ventas;

import ec.edu.espe.ventas.entity.Cliente;
import ec.edu.espe.ventas.repository.ClienteRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de persistencia contra un PostgreSQL real levantado con Testcontainers.
 *
 * <p>Están etiquetadas con {@code containers} y se excluyen del build normal
 * (ver {@code <excludedGroups>} en el pom), de modo que {@code mvnw verify}
 * funciona incluso sin Docker. En GitHub Actions las ejecuta el job
 * {@code containers}.</p>
 */
@DataJpaTest(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Tag("containers")
class PostgresContenedorTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("ventasdb")
            .withUsername("ventas")
            .withPassword("ventas123");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired private ClienteRepository clientes;

    @Test
    void guardaYConsultaClientesEnPostgreSqlReal() {
        clientes.saveAndFlush(cliente("1712345678", "ana@espe.edu.ec"));
        assertThat(clientes.findByCedula("1712345678")).isPresent();
        assertThat(clientes.findByEmail("ana@espe.edu.ec")).isPresent();
        assertThat(clientes.count()).isEqualTo(1);
    }

    @Test
    void laCedulaNoSePuedeRepetirEnPostgreSql() {
        clientes.saveAndFlush(cliente("1712345678", "ana@espe.edu.ec"));
        assertThatThrownBy(() -> clientes.saveAndFlush(cliente("1712345678", "otra@espe.edu.ec")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBusquedaPorApellidoIgnoraMayusculasEnPostgreSql() {
        clientes.saveAndFlush(cliente("1712345678", "ana@espe.edu.ec"));
        assertThat(clientes.findByApellidoContainingIgnoreCase("per")).hasSize(1);
        assertThat(clientes.findByApellidoContainingIgnoreCase("PEREZ")).hasSize(1);
    }

    private Cliente cliente(String cedula, String email) {
        Cliente cliente = new Cliente();
        cliente.setCedula(cedula);
        cliente.setNombre("Ana");
        cliente.setApellido("Perez");
        cliente.setEmail(email);
        return cliente;
    }
}
