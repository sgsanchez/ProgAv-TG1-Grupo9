package ec.edu.espe.ventas;

import ec.edu.espe.ventas.dto.ClienteRequest;
import ec.edu.espe.ventas.dto.DetalleVentaRequest;
import ec.edu.espe.ventas.dto.ProductoRequest;
import ec.edu.espe.ventas.dto.ProductoResponse;
import ec.edu.espe.ventas.dto.VentaRequest;
import ec.edu.espe.ventas.dto.VentaResponse;
import ec.edu.espe.ventas.entity.Cliente;
import ec.edu.espe.ventas.entity.DetalleVenta;
import ec.edu.espe.ventas.entity.EstadoVenta;
import ec.edu.espe.ventas.entity.Producto;
import ec.edu.espe.ventas.entity.Venta;
import ec.edu.espe.ventas.mapper.ClienteMapper;
import ec.edu.espe.ventas.mapper.ProductoMapper;
import ec.edu.espe.ventas.mapper.VentaMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida las restricciones de Bean Validation de los DTOs y las conversiones
 * realizadas por los mappers.
 */
class ValidacionDtoTest {

    private static final Validator VALIDADOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    // ------------------------------------------------------------------
    // Validaciones de entrada
    // ------------------------------------------------------------------

    @Test
    void unClienteValidoNoGeneraErrores() {
        Set<ConstraintViolation<ClienteRequest>> errores = VALIDADOR.validate(clienteValido());

        assertThat(errores).isEmpty();
    }

    @Test
    void rechazaCedulaQueNoTieneDiezDigitos() {
        ClienteRequest request = new ClienteRequest(
                "123", "Ana", "Perez", "ana@espe.edu.ec", "0991111111", null);

        Set<ConstraintViolation<ClienteRequest>> errores = VALIDADOR.validate(request);

        assertThat(errores).hasSize(1);
        assertThat(errores.iterator().next().getMessage()).contains("10 dígitos");
    }

    @Test
    void rechazaCorreoConFormatoInvalido() {
        ClienteRequest request = new ClienteRequest(
                "1712345678", "Ana", "Perez", "correo-sin-arroba", null, null);

        Set<ConstraintViolation<ClienteRequest>> errores = VALIDADOR.validate(request);

        assertThat(errores).hasSize(1);
        assertThat(errores.iterator().next().getMessage()).contains("formato válido");
    }

    @Test
    void rechazaProductoConPrecioMenorOIgualACero() {
        ProductoRequest request = new ProductoRequest(
                "P-010", "Cuaderno", null, new BigDecimal("0.00"), 10, 2);

        Set<ConstraintViolation<ProductoRequest>> errores = VALIDADOR.validate(request);

        assertThat(errores).hasSize(1);
        assertThat(errores.iterator().next().getMessage()).contains("mayor que cero");
    }

    @Test
    void rechazaVentaSinDetalles() {
        VentaRequest request = new VentaRequest(1L, List.of());

        Set<ConstraintViolation<VentaRequest>> errores = VALIDADOR.validate(request);

        assertThat(errores).hasSize(1);
        assertThat(errores.iterator().next().getMessage()).contains("al menos un producto");
    }

    @Test
    void rechazaDetalleConCantidadCero() {
        VentaRequest request = new VentaRequest(1L, List.of(new DetalleVentaRequest(5L, 0)));

        Set<ConstraintViolation<VentaRequest>> errores = VALIDADOR.validate(request);

        assertThat(errores).hasSize(1);
        assertThat(errores.iterator().next().getMessage()).contains("al menos 1");
    }

    // ------------------------------------------------------------------
    // Mappers
    // ------------------------------------------------------------------

    @Test
    void mapeaProductoEIndicaStockBajo() {
        Producto producto = new Producto("P-004", "Calculadora", new BigDecimal("18.75"), 8, 10);
        producto.setId(7L);
        producto.setActivo(Boolean.TRUE);

        ProductoResponse response = ProductoMapper.toResponse(producto);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.codigo()).isEqualTo("P-004");
        assertThat(response.stockBajo()).isTrue();
    }

    @Test
    void mapeaVentaConSusDetalles() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ana");
        cliente.setApellido("Perez");

        Producto producto = new Producto("P-001", "Cuaderno", new BigDecimal("2.50"), 100, 20);
        producto.setId(2L);

        Venta venta = new Venta();
        venta.setId(10L);
        venta.setNumero("V-0001");
        venta.setCliente(cliente);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.PAGADA);
        venta.setSubtotal(new BigDecimal("7.50"));
        venta.setIva(new BigDecimal("1.13"));
        venta.setTotal(new BigDecimal("8.63"));

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(3);
        detalle.setPrecioUnitario(new BigDecimal("2.50"));
        detalle.setSubtotal(new BigDecimal("7.50"));
        venta.agregarDetalle(detalle);

        VentaResponse response = VentaMapper.toResponse(venta);

        assertThat(response.numero()).isEqualTo("V-0001");
        assertThat(response.cliente()).isEqualTo("Ana Perez");
        assertThat(response.detalles()).hasSize(1);
        assertThat(response.detalles().get(0).codigoProducto()).isEqualTo("P-001");
        assertThat(response.detalles().get(0).subtotal()).isEqualByComparingTo("7.50");
    }

    @Test
    void convierteClienteRequestEnEntidadRecortandoEspacios() {
        Cliente cliente = ClienteMapper.toEntity(new ClienteRequest(
                " 1712345678 ", " Ana ", " Perez ", " ana@espe.edu.ec ", "0991111111", " Quito "));

        assertThat(cliente.getCedula()).isEqualTo("1712345678");
        assertThat(cliente.getNombre()).isEqualTo("Ana");
        assertThat(cliente.getEmail()).isEqualTo("ana@espe.edu.ec");
        assertThat(cliente.getDireccion()).isEqualTo("Quito");
    }

    private ClienteRequest clienteValido() {
        return new ClienteRequest(
                "1712345678", "Ana", "Perez", "ana@espe.edu.ec", "0991111111", "Av. General Ruminahui");
    }
}
