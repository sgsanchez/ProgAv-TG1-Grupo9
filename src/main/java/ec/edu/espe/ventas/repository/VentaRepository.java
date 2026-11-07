package ec.edu.espe.ventas.repository;

import ec.edu.espe.ventas.entity.EstadoVenta;
import ec.edu.espe.ventas.entity.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de ventas.
 */
@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByNumero(String numero);

    boolean existsByNumero(String numero);

    List<Venta> findByClienteIdOrderByFechaVentaDesc(Long clienteId);

    List<Venta> findByEstado(EstadoVenta estado);

    List<Venta> findByFechaVentaBetween(LocalDateTime desde, LocalDateTime hasta);

    /**
     * Recupera la venta con sus detalles y productos en una sola consulta
     * (evita el problema de consultas N+1).
     */
    @Query("""
            SELECT DISTINCT v FROM Venta v
            JOIN FETCH v.detalles d
            JOIN FETCH d.producto
            WHERE v.id = :id
            """)
    Optional<Venta> findByIdConDetalles(@Param("id") Long id);

    /**
     * Consulta personalizada: total facturado a un cliente, excluyendo las
     * ventas con el estado indicado (normalmente ANULADA).
     */
    @Query("""
            SELECT COALESCE(SUM(v.total), 0)
            FROM Venta v
            WHERE v.cliente.id = :clienteId AND v.estado <> :estadoExcluido
            """)
    BigDecimal calcularTotalFacturadoPorCliente(
            @Param("clienteId") Long clienteId,
            @Param("estadoExcluido") EstadoVenta estadoExcluido);

    /**
     * Consulta personalizada: total facturado en un rango de fechas.
     */
    @Query("""
            SELECT COALESCE(SUM(v.total), 0)
            FROM Venta v
            WHERE v.fechaVenta BETWEEN :desde AND :hasta AND v.estado <> :estadoExcluido
            """)
    BigDecimal calcularTotalFacturadoEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estadoExcluido") EstadoVenta estadoExcluido);
}
