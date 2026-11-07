package ec.edu.espe.ventas.repository;

import ec.edu.espe.ventas.entity.EstadoVenta;
import ec.edu.espe.ventas.entity.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de productos, con las consultas personalizadas del inventario.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    List<Producto> findByActivoTrue();

    /**
     * Consulta personalizada 1: productos activos cuyo stock está en el nivel
     * mínimo o por debajo (alerta de reposición).
     */
    @Query("""
            SELECT p FROM Producto p
            WHERE p.activo = true AND p.stock <= p.stockMinimo
            ORDER BY p.stock ASC
            """)
    List<Producto> findProductosBajoStock();

    /**
     * Consulta personalizada 2: productos cuyo stock no supera un umbral dado.
     */
    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stock <= :umbral ORDER BY p.stock ASC")
    List<Producto> findPorStockMenorOIgual(@Param("umbral") Integer umbral);

    /**
     * Consulta personalizada 3: ranking de productos por unidades vendidas,
     * excluyendo las ventas con el estado indicado (normalmente ANULADA).
     *
     * @return arreglos de dos posiciones: [nombre del producto, unidades vendidas]
     */
    @Query("""
            SELECT d.producto.nombre, SUM(d.cantidad)
            FROM DetalleVenta d
            WHERE d.venta.estado <> :estadoExcluido
            GROUP BY d.producto.id, d.producto.nombre
            ORDER BY SUM(d.cantidad) DESC
            """)
    List<Object[]> findProductosMasVendidos(@Param("estadoExcluido") EstadoVenta estadoExcluido);
}
