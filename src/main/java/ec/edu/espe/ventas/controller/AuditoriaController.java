package ec.edu.espe.ventas.controller;

import ec.edu.espe.ventas.dto.AuditoriaResponse;
import ec.edu.espe.ventas.dto.PaginaResponse;
import ec.edu.espe.ventas.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de consulta de la auditoría generada por el aspecto AOP.
 */
@Validated
@RestController
@RequestMapping("/api/auditoria")
@Tag(name = "Auditoría", description = "Registro automático de las operaciones de servicio (AOP)")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @Operation(summary = "Lista los registros de auditoría paginados (filtro opcional por operación)")
    public PaginaResponse<AuditoriaResponse> listar(
            @RequestParam(required = false) String operacion,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "pagina: debe ser mayor o igual a 0") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "tamano: debe ser mayor o igual a 1")
            @Max(value = 100, message = "tamano: debe ser menor o igual a 100") int tamano) {
        return auditoriaService.listar(operacion, pagina, tamano);
    }
}
