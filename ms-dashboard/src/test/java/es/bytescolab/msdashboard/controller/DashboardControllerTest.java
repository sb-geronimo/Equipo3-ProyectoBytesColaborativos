package es.bytescolab.msdashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import es.bytescolab.msdashboard.dto.response.DashboardAlertsBlock;
import es.bytescolab.msdashboard.dto.response.DashboardFleetBlock;
import es.bytescolab.msdashboard.dto.response.DashboardKpisBlock;
import es.bytescolab.msdashboard.dto.response.DashboardPeriodBlock;
import es.bytescolab.msdashboard.dto.response.DashboardResponse;
import es.bytescolab.msdashboard.dto.response.DashboardUpcomingMaintenanceItem;
import es.bytescolab.msdashboard.dto.response.RankingResponse;
import es.bytescolab.msdashboard.dto.response.TimeseriesResponse;
import es.bytescolab.msdashboard.exception.DashboardExceptionHandler;
import es.bytescolab.msdashboard.service.DashboardAggregator;
import es.bytescolab.msdashboard.service.RankingService;
import es.bytescolab.msdashboard.service.TimeseriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

    private MockMvc mockMvc;
    private DashboardAggregator dashboardAggregator;
    private TimeseriesService timeseriesService;
    private RankingService rankingService;

    @BeforeEach
    void prepararMockMvc() {
        dashboardAggregator = mock(DashboardAggregator.class);
        timeseriesService = mock(TimeseriesService.class);
        rankingService = mock(RankingService.class);

        DashboardController controller = new DashboardController(
                dashboardAggregator, timeseriesService, rankingService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new DashboardExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void getSummary_todoResponde_devuelve200ConPartialFalse() throws Exception {
        DashboardResponse respuesta = new DashboardResponse(
                Instant.parse("2026-10-05T10:30:00Z"),
                new DashboardPeriodBlock(
                        LocalDate.of(2026, 9, 5), LocalDate.of(2026, 10, 5)),
                false,
                List.of(),
                new DashboardFleetBlock(25L, 14L, 8L, 2L, 1L),
                new DashboardKpisBlock(
                        412L,
                        new BigDecimal("38420.50"),
                        new BigDecimal("4226.30"),
                        new BigDecimal("6846.61"),
                        new BigDecimal("11.00"),
                        new BigDecimal("2140.25")),
                new DashboardAlertsBlock(11L, 2L, 9L, 0L),
                List.of(new DashboardUpcomingMaintenanceItem(
                        UUID.randomUUID(), UUID.randomUUID(),
                        "4821-KDF", "OIL_CHANGE", LocalDate.of(2026, 10, 8)))
        );
        when(dashboardAggregator.getSummary(30)).thenReturn(respuesta);

        mockMvc.perform(get("/api/dashboard").param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partial").value(false))
                .andExpect(jsonPath("$.unavailable").isArray())
                .andExpect(jsonPath("$.fleet.total").value(25))
                .andExpect(jsonPath("$.fleet.available").value(14))
                .andExpect(jsonPath("$.kpis.routesCompleted").value(412))
                .andExpect(jsonPath("$.alerts.open").value(11))
                .andExpect(jsonPath("$.upcomingMaintenance[0].plate").value("4821-KDF"));
    }

    @Test
    void getTimeseries_parametrosValidos_devuelve200() throws Exception {
        when(timeseriesService.getTimeseries(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TimeseriesResponse(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 10, 14),
                        "DAY",
                        List.of(),
                        List.of()));

        mockMvc.perform(get("/api/dashboard/timeseries")
                        .param("metrics", "DISTANCE_KM,FUEL_COST")
                        .param("from", "2026-09-01")
                        .param("to", "2026-10-14")
                        .param("granularity", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-09-01"))
                .andExpect(jsonPath("$.to").value("2026-10-14"))
                .andExpect(jsonPath("$.granularity").value("DAY"))
                .andExpect(jsonPath("$.series").isArray())
                .andExpect(jsonPath("$.unavailable").isArray());
    }

    @Test
    void getRanking_parametrosValidos_devuelve200() throws Exception {
        when(rankingService.getRanking(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new RankingResponse(
                        es.bytescolab.msdashboard.enums.RankingMetric.FUEL_COST,
                        30, 5, null, "EUR", List.of()));

        mockMvc.perform(get("/api/dashboard/vehicles/ranking")
                        .param("metric", "FUEL_COST")
                        .param("days", "30")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric").value("FUEL_COST"))
                .andExpect(jsonPath("$.days").value(30))
                .andExpect(jsonPath("$.limit").value(5))
                .andExpect(jsonPath("$.unit").value("EUR"))
                .andExpect(jsonPath("$.ranking").isArray());
    }
}
