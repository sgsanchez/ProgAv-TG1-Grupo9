package ec.edu.espe.ventas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punto de entrada del Sistema de Ventas y Facturación (Grupo 9).
 *
 * <p>{@code @EnableAsync} habilita la ejecución de tareas no bloqueantes
 * (por ejemplo, la generación del comprobante posterior a una venta).</p>
 */
@SpringBootApplication
@EnableAsync
public class VentasApplication {

    public static void main(String[] args) {
        SpringApplication.run(VentasApplication.class, args);
    }
}
