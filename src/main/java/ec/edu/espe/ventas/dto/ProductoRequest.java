package ec.edu.espe.ventas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos de entrada para crear o actualizar un producto.
 */
public record ProductoRequest(

        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9-]{3,30}",
                message = "El código debe tener entre 3 y 30 caracteres alfanuméricos o guiones")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre,

        @Size(max = 250, message = "La descripción no puede superar los 250 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El precio admite como máximo 2 decimales")
        BigDecimal precio,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo
) {
}
