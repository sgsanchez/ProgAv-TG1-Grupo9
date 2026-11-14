package ec.edu.espe.ventas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI (Swagger UI) de la API.
 *
 * <p>Declara la cabecera {@code X-API-KEY} como esquema de seguridad para que el
 * botón <b>Authorize</b> de Swagger UI permita probar los endpoints protegidos.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_API_KEY = "apiKey";

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
                                .url("https://www.espe.edu.ec")))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_API_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-KEY")
                                .description("Cabecera obligatoria en todos los endpoints /api/**")))
                .security(List.of(new SecurityRequirement().addList(ESQUEMA_API_KEY)));
    }
}
