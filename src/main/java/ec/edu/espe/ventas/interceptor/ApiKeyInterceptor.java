package ec.edu.espe.ventas.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor HTTP transversal sobre {@code /api/**}.
 *
 * <p>Cumple tres funciones transversales:</p>
 * <ol>
 *   <li>Control básico de acceso mediante la cabecera {@code X-API-KEY}.</li>
 *   <li>Registro de cada solicitud (método, ruta, cliente y código de respuesta).</li>
 *   <li>Medición del tiempo de ejecución de cada solicitud (registrado en el log).</li>
 * </ol>
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyInterceptor.class);
    private static final String CABECERA_API_KEY = "X-API-KEY";
    private static final String ATRIBUTO_INICIO = "ec.edu.espe.ventas.inicioPeticion";

    private final String apiKeyEsperada;

    public ApiKeyInterceptor(@Value("${app.security.api-key:}") String apiKeyEsperada) {
        this.apiKeyEsperada = apiKeyEsperada;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        request.setAttribute(ATRIBUTO_INICIO, System.nanoTime());
        log.info("[INTERCEPTOR] Solicitud {} {} desde {}", request.getMethod(),
                request.getRequestURI(), request.getRemoteAddr());

        // Si no se configuró una clave, el control de acceso no se aplica.
        if (apiKeyEsperada == null || apiKeyEsperada.isBlank()) {
            return true;
        }

        String recibida = request.getHeader(CABECERA_API_KEY);
        if (apiKeyEsperada.equals(recibida)) {
            return true;
        }

        log.warn("[INTERCEPTOR] Acceso denegado a {} {} (cabecera {} ausente o incorrecta)",
                request.getMethod(), request.getRequestURI(), CABECERA_API_KEY);

        String mensaje = recibida == null
                ? "Falta la cabecera obligatoria " + CABECERA_API_KEY
                : "El valor de la cabecera " + CABECERA_API_KEY + " no es válido";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\","
                        + "\"mensaje\":\"%s\",\"ruta\":\"%s\",\"detalles\":[]}",
                LocalDateTime.now(), mensaje, request.getRequestURI()));
        response.getWriter().flush();
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object inicio = request.getAttribute(ATRIBUTO_INICIO);
        if (inicio instanceof Long marca) {
            long milisegundos = (System.nanoTime() - marca) / 1_000_000;
            log.info("[INTERCEPTOR] {} {} -> {} en {} ms", request.getMethod(),
                    request.getRequestURI(), response.getStatus(), milisegundos);
        }
    }
}
