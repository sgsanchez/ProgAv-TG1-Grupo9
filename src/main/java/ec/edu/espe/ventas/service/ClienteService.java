package ec.edu.espe.ventas.service;

import ec.edu.espe.ventas.aspect.Auditable;
import ec.edu.espe.ventas.dto.ClienteRequest;
import ec.edu.espe.ventas.dto.ClienteResponse;
import ec.edu.espe.ventas.entity.Cliente;
import ec.edu.espe.ventas.exception.RecursoDuplicadoException;
import ec.edu.espe.ventas.exception.RecursoNoEncontradoException;
import ec.edu.espe.ventas.exception.ReglaNegocioException;
import ec.edu.espe.ventas.mapper.ClienteMapper;
import ec.edu.espe.ventas.repository.ClienteRepository;
import ec.edu.espe.ventas.repository.VentaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de los clientes.
 */
@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;

    public ClienteService(ClienteRepository clienteRepository, VentaRepository ventaRepository) {
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAll().stream()
                .map(ClienteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return ClienteMapper.toResponse(obtenerCliente(id));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorApellido(String apellido) {
        return clienteRepository.findByApellidoContainingIgnoreCase(apellido).stream()
                .map(ClienteMapper::toResponse)
                .toList();
    }

    /**
     * Registra un cliente validando que la cédula y el correo no estén repetidos.
     */
    @Auditable(descripcion = "Registro de cliente")
    public ClienteResponse crear(ClienteRequest request) {
        validarUnicidad(request, null);
        Cliente cliente = ClienteMapper.toEntity(request);
        return ClienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Auditable(descripcion = "Actualización de cliente")
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = obtenerCliente(id);
        validarUnicidad(request, id);
        ClienteMapper.actualizarEntidad(cliente, request);
        return ClienteMapper.toResponse(clienteRepository.save(cliente));
    }

    /**
     * Elimina un cliente siempre que no tenga ventas asociadas,
     * para no romper la integridad referencial del historial de facturación.
     */
    @Auditable(descripcion = "Eliminación de cliente")
    public void eliminar(Long id) {
        Cliente cliente = obtenerCliente(id);
        if (ventaRepository.existsByClienteId(id)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el cliente con cédula " + cliente.getCedula()
                            + " porque tiene ventas registradas");
        }
        clienteRepository.delete(cliente);
    }

    private Cliente obtenerCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    private void validarUnicidad(ClienteRequest request, Long idActual) {
        String cedula = request.cedula() == null ? null : request.cedula().trim();
        String email = request.email() == null ? null : request.email().trim();

        clienteRepository.findByCedula(cedula)
                .filter(existente -> !existente.getId().equals(idActual))
                .ifPresent(existente -> {
                    throw new RecursoDuplicadoException("cédula", cedula);
                });

        clienteRepository.findByEmail(email)
                .filter(existente -> !existente.getId().equals(idActual))
                .ifPresent(existente -> {
                    throw new RecursoDuplicadoException("correo", email);
                });
    }
}
