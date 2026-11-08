package ec.edu.espe.ventas.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de servicio para que sus invocaciones queden registradas
 * en la auditoría mediante {@link AuditoriaAspect}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Descripción de la operación auditada. */
    String descripcion() default "";
}
