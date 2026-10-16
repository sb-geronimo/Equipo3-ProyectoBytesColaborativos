package es.bytescolab.msvehicles.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponse", description = "Respuesta paginada estándar")
public record PageResponse<T>(
        @Schema(description = "Elementos de la página solicitada")
        List<T> content,

        @Schema(description = "Número de página (0 en adelante)", example = "0")
        Integer page,

        @Schema(description = "Tamaño de página efectivo", example = "20")
        Integer size,

        @Schema(description = "Número total de elementos que cumplen los filtros", example = "25")
        Long totalElements,

        @Schema(description = "Número total de páginas", example = "2")
        Integer totalPages
) {
}
