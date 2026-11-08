package ec.edu.espe.ventas.repository;

import ec.edu.espe.ventas.entity.Auditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de los registros de auditoría generados por AOP.
 */
@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findAllByOrderByFechaDesc();

    List<Auditoria> findByOperacionContainingIgnoreCaseOrderByFechaDesc(String operacion);

    long countByExitosoFalse();
}
