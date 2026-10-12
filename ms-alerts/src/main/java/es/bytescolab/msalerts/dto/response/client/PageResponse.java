package es.bytescolab.msalerts.dto.response.client;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CoversPageResponse", description = "Respuesta paginada genérica")
public record PageResponse<T>(

        @Schema(description = "Elementos de la página solicitada")
        List<T> content,

        @Schema(description = "Número de página (0 en adelante)")
        int page,

        @Schema(description = "Tamaño de página efectivo")
        int size,

        @Schema(description = "Número total de elementos que cumplen los filtros")
        long totalElements,

        @Schema(description = "Número total de páginas")
        int totalPages
) {
}