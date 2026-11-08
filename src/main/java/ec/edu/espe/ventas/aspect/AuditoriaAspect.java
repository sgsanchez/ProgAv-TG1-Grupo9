package ec.edu.espe.ventas.aspect;

import ec.edu.espe.ventas.entity.Auditoria;
import ec.edu.espe.ventas.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Aspecto de auditoría: intercepta los métodos anotados con {@link Auditable},
 * mide su duración, registra el resultado y guarda la evidencia en la tabla
 * {@code auditoria}.
 *
 * <p>El registro se guarda en una transacción independiente
 * ({@code REQUIRES_NEW}) para que quede almacenado incluso cuando la operación
 * de negocio falla y su transacción se revierte.</p>
 */
@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    private final AuditoriaRepository auditoriaRepository;
    private final TransactionTemplate transaccionIndependiente;

    public AuditoriaAspect(AuditoriaRepository auditoriaRepository,
                           PlatformTransactionManager transactionManager) {
        this.auditoriaRepository = auditoriaRepository;

        TransactionTemplate plantilla = new TransactionTemplate(transactionManager);
        plantilla.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaccionIndependiente = plantilla;
    }

    @Around("@annotation(auditable)")
    public Object auditar(ProceedingJoinPoint puntoDeUnion, Auditable auditable) throws Throwable {
        long inicio = System.nanoTime();
        boolean exitoso = false;
        String detalle = auditable.descripcion();

        try {
            Object resultado = puntoDeUnion.proceed();
            exitoso = true;
            return resultado;
        } catch (Throwable error) {
            detalle = error.getClass().getSimpleName()
                    + (error.getMessage() == null ? "" : ": " + error.getMessage());
            throw error;
        } finally {
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000;
            String operacion = puntoDeUnion.getSignature().getDeclaringType().getSimpleName()
                    + "." + puntoDeUnion.getSignature().getName();

            log.info("[AUDITORIA] {} | exito={} | {} ms | {}", operacion, exitoso, duracionMs, detalle);
            guardar(operacion, detalle, duracionMs, exitoso);
        }
    }

    private void guardar(String operacion, String detalle, long duracionMs, boolean exitoso) {
        try {
            Auditoria registro = new Auditoria(operacion, auditable(detalle), duracionMs,
                    exitoso, recortar(detalle), LocalDateTime.now());
            transaccionIndependiente.executeWithoutResult(estado -> auditoriaRepository.save(registro));
        } catch (RuntimeException error) {
            // La auditoría nunca debe interrumpir la operación de negocio.
            log.warn("[AUDITORIA] No se pudo guardar el registro de {}: {}", operacion, error.getMessage());
        }
    }

    private String auditable(String detalle) {
        return detalle.length() > 200 ? detalle.substring(0, 200) : detalle;
    }

    private String recortar(String texto) {
        return texto != null && texto.length() > 250 ? texto.substring(0, 250) : texto;
    }
}
