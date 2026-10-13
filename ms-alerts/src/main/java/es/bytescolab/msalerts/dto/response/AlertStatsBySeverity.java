package es.bytescolab.msalerts.dto.response;

import com.fasterxml.jackson.annotation.JsonValue;
import es.bytescolab.msalerts.enums.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.EnumMap;
import java.util.Map;

@Schema(name = "AlertStatsBySeverity", description = "Desglose de alertas creadas dentro de un periodo, agrupadas por severidad")
public record AlertStatsBySeverity(

        @JsonValue
        @Schema(description = "Mapa de severidad a conteo de alertas creadas en el periodo",
                example = "{\"INFO\":0,\"WARNING\":2,\"CRITICAL\":1}")
        Map<AlertSeverity, Long> counts
) {
    public AlertStatsBySeverity {
        if (counts == null) {
            counts = mapaVacio();
        }
    }

    public static AlertStatsBySeverity empty() {
        return new AlertStatsBySeverity(mapaVacio());
    }

    private static Map<AlertSeverity, Long> mapaVacio() {
        Map<AlertSeverity, Long> mapa = new EnumMap<>(AlertSeverity.class);
        for (AlertSeverity severidad : AlertSeverity.values()) {
            mapa.put(severidad, 0L);
        }
        return mapa;
    }
}
