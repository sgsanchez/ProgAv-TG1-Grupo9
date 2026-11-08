package ec.edu.espe.ventas.service;

import ec.edu.espe.ventas.aspect.Auditable;
import ec.edu.espe.ventas.dto.ProductoRequest;
import ec.edu.espe.ventas.dto.ProductoResponse;
import ec.edu.espe.ventas.entity.Producto;
import ec.edu.espe.ventas.exception.RecursoDuplicadoException;
import ec.edu.espe.ventas.exception.RecursoNoEncontradoException;
import ec.edu.espe.ventas.exception.ReglaNegocioException;
import ec.edu.espe.ventas.mapper.ProductoMapper;
import ec.edu.espe.ventas.repository.DetalleVentaRepository;
import ec.edu.espe.ventas.repository.ProductoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de los productos y del inventario.
 */
@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           DetalleVentaRepository detalleVentaRepository) {
        this.productoRepository = productoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id) {
        return ProductoMapper.toResponse(obtenerProducto(id));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre).stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    /**
     * Productos cuyo stock está en el mínimo o por debajo (alerta de reposición).
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarBajoStock() {
        return productoRepository.findProductosBajoStock().stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    @Auditable(descripcion = "Registro de producto")
    public ProductoResponse crear(ProductoRequest request) {
        String codigo = request.codigo() == null ? null : request.codigo().trim();
        if (productoRepository.existsByCodigo(codigo)) {
            throw new RecursoDuplicadoException("código de producto", codigo);
        }
        Producto producto = ProductoMapper.toEntity(request);
        return ProductoMapper.toResponse(productoRepository.save(producto));
    }

    @Auditable(descripcion = "Actualización de producto")
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = obtenerProducto(id);
        String codigo = request.codigo() == null ? null : request.codigo().trim();

        productoRepository.findByCodigo(codigo)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new RecursoDuplicadoException("código de producto", codigo);
                });

        ProductoMapper.actualizarEntidad(producto, request);
        return ProductoMapper.toResponse(productoRepository.save(producto));
    }

    /**
     * Elimina un producto solo si nunca fue vendido, para conservar el
     * histórico de las facturas ya emitidas.
     */
    @Auditable(descripcion = "Eliminación de producto")
    public void eliminar(Long id) {
        Producto producto = obtenerProducto(id);
        if (detalleVentaRepository.existsByProductoId(id)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el producto '" + producto.getNombre()
                            + "' porque forma parte de ventas registradas");
        }
        productoRepository.delete(producto);
    }

    private Producto obtenerProducto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
    }
}
