package es.bytescolab.msdrivers.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Schema(description = "Respuesta paginada con content, page, size, totalElements y totalPages")
public record PageResponse<T>(

        @Schema(description = "Elementos de la página actual")
        List<T> content,

        @Schema(description = "Número de página (0 en adelante)", example = "0")
        int page,

        @Schema(description = "Tamaño de página solicitado", example = "20")
        int size,

        @Schema(description = "Número total de elementos que cumplen el filtro", example = "20")
        long totalElements,

        @Schema(description = "Número total de páginas", example = "1")
        int totalPages
) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}