package ec.edu.espe.ventas.exception;

/**
 * Se lanza cuando se intenta registrar un dato que debe ser único
 * (cédula o correo de cliente, código de producto, número de venta, etc.).
 * Se traduce a HTTP 409.
 */
public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }

    public RecursoDuplicadoException(String campo, String valor) {
        super(String.format("Ya existe un registro con %s = '%s'", campo, valor));
    }
}
