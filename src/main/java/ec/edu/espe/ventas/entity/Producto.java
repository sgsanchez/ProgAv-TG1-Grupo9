package ec.edu.espe.ventas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import org.hibernate.annotations.Check;

/**
 * Producto disponible para la venta.
 *
 * <p>Restricciones de integridad: el código es único; el precio, el stock y el
 * stock mínimo no pueden ser negativos.</p>
 */
@Entity
@Table(
        name = "productos",
        uniqueConstraints = @UniqueConstraint(name = "uk_producto_codigo", columnNames = "codigo"))
@Check(constraints = "precio >= 0 AND stock >= 0 AND stock_minimo >= 0")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    public Producto() {
    }

    public Producto(String codigo, String nombre, BigDecimal precio, Integer stock, Integer stockMinimo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
    }

    @PrePersist
    @PreUpdate
    void normalizar() {
        if (activo == null) {
            activo = Boolean.TRUE;
        }
        if (stockMinimo == null) {
            stockMinimo = 0;
        }
    }

    /**
     * Indica si el producto está por debajo o igual a su stock mínimo.
     */
    public boolean tieneStockBajo() {
        return stock != null && stockMinimo != null && stock <= stockMinimo;
    }

    /**
     * Indica si hay existencias suficientes para cubrir la cantidad solicitada.
     */
    public boolean hayStockSuficiente(int cantidad) {
        return stock != null && stock >= cantidad;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
