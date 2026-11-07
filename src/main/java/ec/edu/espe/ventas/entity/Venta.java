package ec.edu.espe.ventas.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Check;

/**
 * Venta o factura emitida a un cliente.
 *
 * <p>Los importes {@code subtotal}, {@code iva} y {@code total} se calculan en la
 * capa de servicio a partir de los detalles, nunca a partir de datos del cliente.</p>
 */
@Entity
@Table(
        name = "ventas",
        uniqueConstraints = @UniqueConstraint(name = "uk_venta_numero", columnNames = "numero"))
@Check(constraints = "subtotal >= 0 AND iva >= 0 AND total >= 0")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_venta_cliente"))
    private Cliente cliente;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.REGISTRADA;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    @PrePersist
    void antesDeInsertar() {
        if (fechaVenta == null) {
            fechaVenta = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoVenta.REGISTRADA;
        }
    }

    /**
     * Agrega un detalle manteniendo la relación en ambos sentidos.
     */
    public void agregarDetalle(DetalleVenta detalle) {
        detalle.setVenta(this);
        this.detalles.add(detalle);
    }

    /**
     * Quita un detalle de la venta.
     */
    public void quitarDetalle(DetalleVenta detalle) {
        this.detalles.remove(detalle);
        detalle.setVenta(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getIva() {
        return iva;
    }

    public void setIva(BigDecimal iva) {
        this.iva = iva;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoVenta estado) {
        this.estado = estado;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }
}
