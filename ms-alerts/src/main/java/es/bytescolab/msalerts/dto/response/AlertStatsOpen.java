package es.bytescolab.msalerts.dto.response;

import com.fasterxml.jackson.annotation.JsonValue;
import es.bytescolab.msalerts.enums.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.EnumMap;
import java.util.Map;

@Schema(name = "AlertStatsOpen", description = "Snapshot de alertas actualmente en estado OPEN, agrupadas por severidad")
public record AlertStatsOpen(

        @JsonValue
        @Schema(description = "Mapa de severidad a conteo de alertas actualmente abiertas",
                example = "{\"INFO\":0,\"WARNING\":9,\"CRITICAL\":2}")
        Map<AlertSeverity, Long> counts
) {
    public AlertStatsOpen {
        if (counts == null) {
            counts = mapaVacio();
        }
    }

    public static AlertStatsOpen empty() {
        return new AlertStatsOpen(mapaVacio());
    }

    private static Map<AlertSeverity, Long> mapaVacio() {
        Map<AlertSeverity, Long> mapa = new EnumMap<>(AlertSeverity.class);
        for (AlertSeverity severidad : AlertSeverity.values()) {
            mapa.put(severidad, 0L);
        }
        return mapa;
    }
}
