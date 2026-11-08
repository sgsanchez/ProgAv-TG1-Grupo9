package ec.edu.espe.ventas.controller;

import ec.edu.espe.ventas.dto.AuditoriaResponse;
import ec.edu.espe.ventas.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de consulta de la auditoría generada por el aspecto AOP.
 */
@RestController
@RequestMapping("/api/auditoria")
@Tag(name = "Auditoría", description = "Registro automático de las operaciones de servicio (AOP)")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @Operation(summary = "Lista los registros de auditoría más recientes (filtro opcional por operación)")
    public List<AuditoriaResponse> listar(@RequestParam(required = false) String operacion) {
        return auditoriaService.listar(operacion);
    }
}
