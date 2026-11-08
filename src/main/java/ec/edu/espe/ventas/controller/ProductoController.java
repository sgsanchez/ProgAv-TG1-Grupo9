package ec.edu.espe.ventas.controller;

import ec.edu.espe.ventas.dto.ProductoRequest;
import ec.edu.espe.ventas.dto.ProductoResponse;
import ec.edu.espe.ventas.dto.RankingProductoResponse;
import ec.edu.espe.ventas.service.ProductoService;
import ec.edu.espe.ventas.service.VentaService;
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
 * Endpoints REST de productos e inventario.
 */
@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Administración del catálogo y del inventario")
public class ProductoController {

    private final ProductoService productoService;
    private final VentaService ventaService;

    public ProductoController(ProductoService productoService, VentaService ventaService) {
        this.productoService = productoService;
        this.ventaService = ventaService;
    }

    @GetMapping
    @Operation(summary = "Lista los productos (opcionalmente filtrados por nombre)")
    public List<ProductoResponse> listar(@RequestParam(required = false) String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return productoService.listar();
        }
        return productoService.buscarPorNombre(nombre);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un producto por su identificador")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }

    @GetMapping("/bajo-stock")
    @Operation(summary = "Lista los productos con stock en el mínimo o por debajo")
    public List<ProductoResponse> bajoStock() {
        return productoService.listarBajoStock();
    }

    @GetMapping("/mas-vendidos")
    @Operation(summary = "Ranking de productos por unidades vendidas (excluye ventas anuladas)")
    public List<RankingProductoResponse> masVendidos() {
        return ventaService.rankingProductos();
    }

    @PostMapping
    @Operation(summary = "Registra un nuevo producto")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = productoService.crear(request);
        return ResponseEntity.created(URI.create("/api/productos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un producto existente (incluye reposición de stock)")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un producto que no haya sido vendido")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
