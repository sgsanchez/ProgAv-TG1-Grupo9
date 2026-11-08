package ec.edu.espe.ventas.dto;

import java.time.LocalDateTime;

/**
 * Datos de salida de un cliente.
 */
public record ClienteResponse(
        Long id,
        String cedula,
        String nombre,
        String apellido,
        String email,
        String telefono,
        String direccion,
        LocalDateTime fechaRegistro,
        Boolean activo
) {
}
