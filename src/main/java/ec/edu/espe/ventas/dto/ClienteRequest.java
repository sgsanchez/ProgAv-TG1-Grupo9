package ec.edu.espe.ventas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para crear o actualizar un cliente.
 */
public record ClienteRequest(

        @NotBlank(message = "La cédula es obligatoria")
        @Pattern(regexp = "\\d{10}", message = "La cédula debe contener exactamente 10 dígitos")
        String cedula,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 80, message = "El apellido no puede superar los 80 caracteres")
        String apellido,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
        String email,

        @Pattern(regexp = "^\\d{7,10}$", message = "El teléfono debe tener entre 7 y 10 dígitos")
        String telefono,

        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
        String direccion
) {
}
