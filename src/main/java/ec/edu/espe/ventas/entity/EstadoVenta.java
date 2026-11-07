package ec.edu.espe.ventas.entity;

/**
 * Estados por los que pasa una venta.
 */
public enum EstadoVenta {

    /** Venta registrada, pendiente de pago. */
    REGISTRADA,

    /** Venta pagada y facturada. */
    PAGADA,

    /** Venta anulada: el stock de sus productos fue restituido. */
    ANULADA
}
