package ec.edu.espe.ventas.exception;

/**
 * Se lanza cuando se busca un recurso que no existe en la base de datos.
 * Se traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(String.format("%s no encontrado con id %d", recurso, id));
    }
}
