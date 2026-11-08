package ec.edu.espe.ventas.dto;

/**
 * Respuesta inmediata cuando un proceso se delega a un hilo en segundo plano.
 */
public record SolicitudAsincronaResponse(
        String mensaje,
        String numero,
        String estado
) {
}
