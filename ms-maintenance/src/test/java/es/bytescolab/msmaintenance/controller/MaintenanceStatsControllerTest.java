package es.bytescolab.msmaintenance.controller;

import es.bytescolab.msmaintenance.config.SecurityConfig;
import es.bytescolab.msmaintenance.dto.request.MaintenanceStatsRequest;
import es.bytescolab.msmaintenance.dto.response.MaintenanceStatsByType;
import es.bytescolab.msmaintenance.dto.response.MaintenanceStatsPoint;
import es.bytescolab.msmaintenance.dto.response.MaintenanceStatsResponse;
import es.bytescolab.msmaintenance.dto.response.MaintenanceStatsTotals;
import es.bytescolab.msmaintenance.dto.response.MaintenanceStatsUpcoming;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import es.bytescolab.msmaintenance.exception.GlobalExceptionHandler;
import es.bytescolab.msmaintenance.security.InternalKeyFilter;
import es.bytescolab.msmaintenance.security.JwtAuthFilter;
import es.bytescolab.msmaintenance.security.JwtUtil;
import es.bytescolab.msmaintenance.security.TokenAccessDeniedHandler;
import es.bytescolab.msmaintenance.security.TokenAuthenticationEntryPoint;
import es.bytescolab.msmaintenance.service.MaintenanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MaintenanceController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@org.springframework.test.context.ActiveProfiles("test")
class MaintenanceStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaintenanceService maintenanceService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private InternalKeyFilter internalKeyFilter;

    @MockitoBean
    private TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint;

    @MockitoBean
    private TokenAccessDeniedHandler tokenAccessDeniedHandler;

    @Test
    void getStats_devuelve200ConSerie() throws Exception {
        MaintenanceStatsResponse respuesta = new MaintenanceStatsResponse(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 27),
                "WEEK",
                new MaintenanceStatsTotals(9L, new BigDecimal("1480.75")),
                List.of(
                        new MaintenanceStatsPoint(LocalDate.of(2026, 9, 7), 3L, new BigDecimal("420.00")),
                        new MaintenanceStatsPoint(LocalDate.of(2026, 9, 14), 4L, new BigDecimal("715.25"))
                ),
                List.of(new MaintenanceStatsByType(MaintenanceType.OIL_CHANGE, 4L, new BigDecimal("742.00"))),
                new MaintenanceStatsUpcoming(1L, 5L)
        );

        when(maintenanceService.getStats(any(MaintenanceStatsRequest.class))).thenReturn(respuesta);

        mockMvc.perform(get("/api/maintenance/stats")
                        .param("from", "2026-09-07")
                        .param("to", "2026-09-27")
                        .param("granularity", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("WEEK"))
                .andExpect(jsonPath("$.totals.orders").value(9))
                .andExpect(jsonPath("$.totals.cost").value(1480.75))
                .andExpect(jsonPath("$.series[0].period").value("2026-09-07"))
                .andExpect(jsonPath("$.series[0].orders").value(3))
                .andExpect(jsonPath("$.byType[0].type").value("OIL_CHANGE"))
                .andExpect(jsonPath("$.upcoming.overdue").value(1))
                .andExpect(jsonPath("$.upcoming.dueIn7Days").value(5));
    }

    @Test
    void getStats_sinVehicle_noFalla() throws Exception {
        MaintenanceStatsResponse respuesta = new MaintenanceStatsResponse(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 27),
                "DAY",
                new MaintenanceStatsTotals(0L, BigDecimal.ZERO),
                List.of(),
                List.of(),
                new MaintenanceStatsUpcoming(0L, 0L)
        );

        when(maintenanceService.getStats(any(MaintenanceStatsRequest.class))).thenReturn(respuesta);

        mockMvc.perform(get("/api/maintenance/stats")
                        .param("from", "2026-09-07")
                        .param("to", "2026-09-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series").isArray())
                .andExpect(jsonPath("$.byType").isArray())
                .andExpect(jsonPath("$.upcoming.overdue").value(0));
    }
}