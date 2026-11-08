package ec.edu.espe.ventas.controller;

import ec.edu.espe.ventas.dto.ClienteRequest;
import ec.edu.espe.ventas.dto.ClienteResponse;
import ec.edu.espe.ventas.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de clientes.
 */
@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Administración de los clientes del sistema de ventas")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    @Operation(summary = "Lista los clientes (opcionalmente filtrados por apellido)")
    public List<ClienteResponse> listar(@RequestParam(required = false) String apellido) {
        if (apellido == null || apellido.isBlank()) {
            return clienteService.listar();
        }
        return clienteService.buscarPorApellido(apellido);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un cliente por su identificador")
    public ClienteResponse obtener(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Registra un nuevo cliente")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse creado = clienteService.crear(request);
        return ResponseEntity.created(URI.create("/api/clientes/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza los datos de un cliente existente")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un cliente que no tenga ventas registradas")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
