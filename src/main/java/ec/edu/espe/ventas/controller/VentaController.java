package ec.edu.espe.ventas.controller;

import ec.edu.espe.ventas.dto.CambioEstadoVentaRequest;
import ec.edu.espe.ventas.dto.SolicitudAsincronaResponse;
import ec.edu.espe.ventas.dto.VentaRequest;
import ec.edu.espe.ventas.dto.VentaResponse;
import ec.edu.espe.ventas.service.ComprobanteService;
import ec.edu.espe.ventas.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de ventas y facturación.
 */
@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas", description = "Registro, consulta y anulación de ventas")
public class VentaController {

    private final VentaService ventaService;
    private final ComprobanteService comprobanteService;

    public VentaController(VentaService ventaService, ComprobanteService comprobanteService) {
        this.ventaService = ventaService;
        this.comprobanteService = comprobanteService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las ventas registradas")
    public List<VentaResponse> listar() {
        return ventaService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una venta con sus detalles")
    public VentaResponse obtener(@PathVariable Long id) {
        return ventaService.buscarPorId(id);
    }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Lista las ventas de un cliente, de la más reciente a la más antigua")
    public List<VentaResponse> porCliente(@PathVariable Long clienteId) {
        return ventaService.listarPorCliente(clienteId);
    }

    @GetMapping("/cliente/{clienteId}/total")
    @Operation(summary = "Total facturado a un cliente, excluyendo las ventas anuladas")
    public Map<String, Object> totalPorCliente(@PathVariable Long clienteId) {
        BigDecimal total = ventaService.totalFacturadoPorCliente(clienteId);
        return Map.of("clienteId", clienteId, "totalFacturado", total);
    }

    @PostMapping
    @Operation(summary = "Registra una venta, descuenta el stock y calcula los importes")
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request) {
        VentaResponse creada = ventaService.registrar(request);
        return ResponseEntity.created(URI.create("/api/ventas/" + creada.id())).body(creada);
    }

    @PostMapping("/{id}/anular")
    @Operation(summary = "Anula una venta y devuelve el stock al inventario")
    public VentaResponse anular(@PathVariable Long id) {
        return ventaService.anular(id);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de una venta (REGISTRADA / PAGADA / ANULADA)")
    public VentaResponse cambiarEstado(@PathVariable Long id,
                                       @Valid @RequestBody CambioEstadoVentaRequest request) {
        return ventaService.cambiarEstado(id, request.estado());
    }

    @GetMapping("/{id}/comprobante")
    @Operation(summary = "Genera el comprobante de la venta: modo=bloqueante espera el proceso; "
            + "modo=asincrono responde 202 de inmediato y lo procesa en segundo plano")
    public ResponseEntity<?> comprobante(@PathVariable Long id,
                                         @RequestParam(defaultValue = "asincrono") String modo) {
        VentaResponse venta = ventaService.buscarPorId(id);

        if ("bloqueante".equalsIgnoreCase(modo)) {
            return ResponseEntity.ok(comprobanteService.generarBloqueante(venta));
        }

        comprobanteService.generarAsincrono(venta);
        return ResponseEntity.accepted().body(new SolicitudAsincronaResponse(
                "El comprobante de " + venta.numero() + " se está generando en segundo plano",
                venta.numero(),
                "EN_PROCESO"));
    }
}
