package ec.edu.espe.ventas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Registro de auditoría generado automáticamente por el aspecto AOP.
 */
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operacion", nullable = false, length = 120)
    private String operacion;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "duracion_ms", nullable = false)
    private Long duracionMs;

    @Column(name = "exitoso", nullable = false)
    private Boolean exitoso;

    @Column(name = "detalle", length = 250)
    private String detalle;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public Auditoria() {
    }

    public Auditoria(String operacion, String descripcion, Long duracionMs,
                     Boolean exitoso, String detalle, LocalDateTime fecha) {
        this.operacion = operacion;
        this.descripcion = descripcion;
        this.duracionMs = duracionMs;
        this.exitoso = exitoso;
        this.detalle = detalle;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperacion() {
        return operacion;
    }

    public void setOperacion(String operacion) {
        this.operacion = operacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getDuracionMs() {
        return duracionMs;
    }

    public void setDuracionMs(Long duracionMs) {
        this.duracionMs = duracionMs;
    }

    public Boolean getExitoso() {
        return exitoso;
    }

    public void setExitoso(Boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
