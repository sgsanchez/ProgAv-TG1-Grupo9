package ec.edu.espe.ventas.exception;

/**
 * Se lanza cuando una operación viola una regla de negocio del proceso de ventas.
 * Se traduce a HTTP 409.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
