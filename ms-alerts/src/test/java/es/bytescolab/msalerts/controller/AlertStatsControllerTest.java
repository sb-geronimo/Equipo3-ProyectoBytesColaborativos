package es.bytescolab.msalerts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import es.bytescolab.msalerts.dto.request.AlertStatsRequest;
import es.bytescolab.msalerts.dto.response.AlertStatsBySeverity;
import es.bytescolab.msalerts.dto.response.AlertStatsOpen;
import es.bytescolab.msalerts.dto.response.AlertStatsPoint;
import es.bytescolab.msalerts.dto.response.AlertStatsResponse;
import es.bytescolab.msalerts.dto.response.AlertStatsTotals;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.exception.AlertValidationExceptionHandler;
import es.bytescolab.msalerts.service.AlertEvaluationService;
import es.bytescolab.msalerts.service.AlertQueryService;
import es.bytescolab.msalerts.service.AlertRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlertStatsControllerTest {

    private MockMvc mockMvc;
    private AlertQueryService alertQueryService;

    @BeforeEach
    void prepararMockMvc() {
        alertQueryService = mock(AlertQueryService.class);
        AlertRuleService alertRuleService = mock(AlertRuleService.class);
        AlertEvaluationService alertEvaluationService = mock(AlertEvaluationService.class);

        AlertController controller = new AlertController(alertQueryService, alertRuleService, alertEvaluationService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AlertValidationExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void getStats_devuelve200ConSerie() throws Exception {
        Map<AlertSeverity, Long> conteos = new EnumMap<>(AlertSeverity.class);
        conteos.put(AlertSeverity.INFO, 0L);
        conteos.put(AlertSeverity.WARNING, 2L);
        conteos.put(AlertSeverity.CRITICAL, 1L);

        AlertStatsResponse respuesta = new AlertStatsResponse(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 27),
                "WEEK",
                new AlertStatsTotals(3L),
                List.of(new AlertStatsPoint(LocalDate.of(2026, 9, 7), 3L, new AlertStatsBySeverity(conteos))),
                new AlertStatsOpen(conteos));
        when(alertQueryService.getStats(any(AlertStatsRequest.class))).thenReturn(respuesta);

        mockMvc.perform(get("/api/alerts/stats")
                        .param("from", "2026-09-07")
                        .param("to", "2026-09-27")
                        .param("granularity", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-09-07"))
                .andExpect(jsonPath("$.to").value("2026-09-27"))
                .andExpect(jsonPath("$.granularity").value("WEEK"))
                .andExpect(jsonPath("$.totals.created").value(3))
                .andExpect(jsonPath("$.series[0].period").value("2026-09-07"))
                .andExpect(jsonPath("$.series[0].created").value(3))
                .andExpect(jsonPath("$.series[0].bySeverity.WARNING").value(2))
                .andExpect(jsonPath("$.open.WARNING").value(2));
    }

    @Test
    void getStats_sinVehicle_noFalla() throws Exception {
        when(alertQueryService.getStats(any(AlertStatsRequest.class))).thenReturn(
                new AlertStatsResponse(
                        LocalDate.of(2026, 9, 7),
                        LocalDate.of(2026, 9, 8),
                        "DAY",
                        new AlertStatsTotals(0L),
                        List.of(),
                        new AlertStatsOpen(new EnumMap<>(AlertSeverity.class))));

        mockMvc.perform(get("/api/alerts/stats")
                        .param("from", "2026-09-07")
                        .param("to", "2026-09-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("DAY"));
    }

    @Test
    void getStats_sinFrom_retorna400() throws Exception {
        mockMvc.perform(get("/api/alerts/stats")
                        .param("to", "2026-09-08"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El parámetro 'from' es obligatorio"));
    }
}
