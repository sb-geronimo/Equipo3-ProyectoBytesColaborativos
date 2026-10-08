package es.bytescolab.msroutes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
@Schema(name = "PageResponse", description = "Respuesta paginada estándar")
public record PageResponse<T>(
        @Schema(description = "Elementos de la página solicitada")
        List<T> content,

        @Schema(description = "Número de página (0 en adelante)", example = "0")
        int page,

        @Schema(description = "Tamaño de página efectivo", example = "20")
        int size,

        @Schema(description = "Número total de elementos que cumplen los filtros", example = "153")
        long totalElements,

        @Schema(description = "Número total de páginas", example = "8")
        int totalPages
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
