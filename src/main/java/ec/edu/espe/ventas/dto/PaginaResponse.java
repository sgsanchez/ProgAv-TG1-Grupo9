package ec.edu.espe.ventas.dto;

import java.util.List;

/**
 * Envoltorio genérico para respuestas paginadas.
 *
 * @param contenido     elementos de la página solicitada
 * @param pagina        número de página (base 0)
 * @param tamano        tamaño de página solicitado
 * @param total         total de elementos que cumplen el filtro
 * @param totalPaginas  cantidad total de páginas disponible
 * @param <T>           tipo de los elementos
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long total,
        int totalPaginas
) {
}
