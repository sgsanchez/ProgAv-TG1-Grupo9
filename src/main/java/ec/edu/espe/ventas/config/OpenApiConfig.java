package ec.edu.espe.ventas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI (Swagger UI) de la API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ventasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Ventas y Facturación - Grupo 9")
                        .description("""
                                API REST para la administración de clientes, productos, ventas y
                                detalles de venta. Proyecto de la Unidad 1 - Programación Avanzada.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Grupo 9 - Programación Avanzada")
                                .email("grupo9@espe.edu.ec"))
                        .license(new License()
                                .name("Uso académico")
                                .url("https://www.espe.edu.ec")));
    }
}
