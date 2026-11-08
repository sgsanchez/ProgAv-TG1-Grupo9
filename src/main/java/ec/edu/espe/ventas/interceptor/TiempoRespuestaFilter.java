package ec.edu.espe.ventas.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Middleware que mide el tiempo total de cada solicitud a {@code /api/**} y lo
 * devuelve al cliente en la cabecera {@code X-Response-Time}.
 *
 * <p>Se implementa como filtro porque la cabecera debe añadirse antes de que la
 * respuesta HTTP se envíe; el interceptor, en cambio, se ejecuta con la
 * respuesta ya escrita.</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TiempoRespuestaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper respuestaEnvuelta = new ContentCachingResponseWrapper(response);
        long inicio = System.nanoTime();

        try {
            chain.doFilter(request, respuestaEnvuelta);
        } finally {
            long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
            respuestaEnvuelta.setHeader("X-Response-Time", milisegundos + " ms");
            respuestaEnvuelta.copyBodyToResponse();
        }
    }
}
