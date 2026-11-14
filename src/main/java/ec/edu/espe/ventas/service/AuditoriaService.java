package ec.edu.espe.ventas.service;

import ec.edu.espe.ventas.dto.AuditoriaResponse;
import ec.edu.espe.ventas.dto.PaginaResponse;
import ec.edu.espe.ventas.entity.Auditoria;
import ec.edu.espe.ventas.repository.AuditoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /**
     * Lista los registros de auditoría de forma paginada, siempre del más reciente
     * al más antiguo, con filtro opcional por nombre de operación.
     *
     * @param operacion texto a buscar dentro del nombre de la operación (puede ser nulo o vacío)
     * @param pagina    número de página, base 0
     * @param tamano    cantidad de registros por página
     */
    public PaginaResponse<AuditoriaResponse> listar(String operacion, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fecha"));
        Page<Auditoria> registros = (operacion == null || operacion.isBlank())
                ? auditoriaRepository.findAll(pageable)
                : auditoriaRepository.findByOperacionContainingIgnoreCase(operacion, pageable);

        return new PaginaResponse<>(
                registros.getContent().stream().map(AuditoriaService::aRespuesta).toList(),
                registros.getNumber(),
                registros.getSize(),
                registros.getTotalElements(),
                registros.getTotalPages());
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
