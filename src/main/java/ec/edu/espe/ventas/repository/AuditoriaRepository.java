package ec.edu.espe.ventas.repository;

import ec.edu.espe.ventas.entity.Auditoria;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de los registros de auditoría generados por AOP.
 */
@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findAllByOrderByFechaDesc();

    List<Auditoria> findByOperacionContainingIgnoreCaseOrderByFechaDesc(String operacion);

    /** Versión paginada del filtro por operación (usada por el listado del endpoint). */
    Page<Auditoria> findByOperacionContainingIgnoreCase(String operacion, Pageable pageable);

    long countByExitosoFalse();
}
