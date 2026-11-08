package ec.edu.espe.ventas.service;

import ec.edu.espe.ventas.dto.AuditoriaResponse;
import ec.edu.espe.ventas.entity.Auditoria;
import ec.edu.espe.ventas.repository.AuditoriaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de los registros generados por el aspecto de auditoría.
 */
@Service
@Transactional(readOnly = true)
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public List<AuditoriaResponse> listar(String operacion) {
        List<Auditoria> registros = (operacion == null || operacion.isBlank())
                ? auditoriaRepository.findAllByOrderByFechaDesc()
                : auditoriaRepository.findByOperacionContainingIgnoreCaseOrderByFechaDesc(operacion);

        return registros.stream().map(AuditoriaService::aRespuesta).toList();
    }

    private static AuditoriaResponse aRespuesta(Auditoria auditoria) {
        return new AuditoriaResponse(
                auditoria.getId(),
                auditoria.getOperacion(),
                auditoria.getDescripcion(),
                auditoria.getDuracionMs(),
                auditoria.getExitoso(),
                auditoria.getDetalle(),
                auditoria.getFecha());
    }
}
