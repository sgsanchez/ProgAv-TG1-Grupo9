package ec.edu.espe.ventas.service;

import ec.edu.espe.ventas.dto.ComprobanteResponse;
import ec.edu.espe.ventas.dto.VentaResponse;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Generación del comprobante de una venta.
 *
 * <p>Se usa como ejemplo del tema <b>programación bloqueante vs. no bloqueante</b>:</p>
 * <ul>
 *   <li>{@link #generarBloqueante(VentaResponse)} ejecuta el trabajo en el mismo hilo
 *       que atiende la petición HTTP, por lo que el cliente espera toda la duración.</li>
 *   <li>{@link #generarAsincrono(VentaResponse)} delega el trabajo al pool
 *       {@code ventas-async-*} configurado en {@code AsyncConfig} y devuelve el control
 *       de inmediato, liberando el hilo de Tomcat.</li>
 * </ul>
 */
@Service
public class ComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteService.class);

    /** Simula el costo de generar el documento y enviarlo por correo. */
    private final long retardoMs;

    public ComprobanteService(@Value("${app.comprobante.retardo-ms:1200}") long retardoMs) {
        this.retardoMs = retardoMs;
    }

    /**
     * Variante bloqueante: el hilo de la petición espera hasta terminar.
     */
    public ComprobanteResponse generarBloqueante(VentaResponse venta) {
        long inicio = System.currentTimeMillis();
        String contenido = construirContenido(venta);

        simularTrabajoCostoso();

        long duracion = System.currentTimeMillis() - inicio;
        String hilo = Thread.currentThread().getName();
        log.info("[COMPROBANTE] {} generado en {} ms en el hilo {}", venta.numero(), duracion, hilo);

        return new ComprobanteResponse(venta.numero(), venta.cliente(), venta.total(),
                contenido, duracion, hilo);
    }

    /**
     * Variante no bloqueante: el trabajo se ejecuta en segundo plano y el hilo de
     * la petición queda libre de inmediato.
     */
    @Async
    public CompletableFuture<ComprobanteResponse> generarAsincrono(VentaResponse venta) {
        log.info("[COMPROBANTE] {} encolado para segundo plano", venta.numero());
        return CompletableFuture.completedFuture(generarBloqueante(venta));
    }

    private String construirContenido(VentaResponse venta) {
        return "COMPROBANTE %s | Cliente: %s | Subtotal: %s | IVA: %s | Total: %s"
                .formatted(venta.numero(), venta.cliente(), venta.subtotal(), venta.iva(), venta.total());
    }

    private void simularTrabajoCostoso() {
        if (retardoMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retardoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[COMPROBANTE] Generación interrumpida");
        }
    }

    /** Total con formato monetario, útil para el comprobante. */
    static String formatoMonetario(BigDecimal valor) {
        return valor == null ? "0.00" : valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
