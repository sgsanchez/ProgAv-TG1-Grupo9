package ec.edu.espe.ventas.exception;

/**
 * Se lanza cuando se intenta vender más cantidad de la disponible en stock.
 * Se traduce a HTTP 409 (conflicto con el estado actual del inventario).
 */
public class StockInsuficienteException extends RuntimeException {

    private final String producto;
    private final int solicitado;
    private final int disponible;

    public StockInsuficienteException(String producto, int solicitado, int disponible) {
        super(String.format(
                "Stock insuficiente para el producto '%s': se solicitan %d unidades pero solo hay %d disponibles",
                producto, solicitado, disponible));
        this.producto = producto;
        this.solicitado = solicitado;
        this.disponible = disponible;
    }

    public String getProducto() {
        return producto;
    }

    public int getSolicitado() {
        return solicitado;
    }

    public int getDisponible() {
        return disponible;
    }
}
