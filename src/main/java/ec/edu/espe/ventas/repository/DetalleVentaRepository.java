package ec.edu.espe.ventas.repository;

import ec.edu.espe.ventas.entity.DetalleVenta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de los detalles (líneas) de una venta.
 */
@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    List<DetalleVenta> findByVentaId(Long ventaId);

    boolean existsByProductoId(Long productoId);

    /**
     * Consulta personalizada: unidades vendidas de un producto.
     */
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetalleVenta d WHERE d.producto.id = :productoId")
    Long totalUnidadesVendidas(@Param("productoId") Long productoId);
}
