package ec.edu.espe.ventas.mapper;

import ec.edu.espe.ventas.dto.ClienteRequest;
import ec.edu.espe.ventas.dto.ClienteResponse;
import ec.edu.espe.ventas.entity.Cliente;

/**
 * Conversión entre {@link Cliente} y sus DTOs.
 */
public final class ClienteMapper {

    private ClienteMapper() {
        // Clase utilitaria
    }

    public static Cliente toEntity(ClienteRequest request) {
        Cliente cliente = new Cliente();
        actualizarEntidad(cliente, request);
        return cliente;
    }

    /**
     * Copia los datos editables del DTO sobre la entidad existente,
     * conservando los campos que administra el servidor.
     */
    public static void actualizarEntidad(Cliente cliente, ClienteRequest request) {
        cliente.setCedula(limpiar(request.cedula()));
        cliente.setNombre(limpiar(request.nombre()));
        cliente.setApellido(limpiar(request.apellido()));
        cliente.setEmail(limpiar(request.email()));
        cliente.setTelefono(limpiar(request.telefono()));
        cliente.setDireccion(limpiar(request.direccion()));
    }

    public static ClienteResponse toResponse(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        return new ClienteResponse(
                cliente.getId(),
                cliente.getCedula(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getFechaRegistro(),
                cliente.getActivo());
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
