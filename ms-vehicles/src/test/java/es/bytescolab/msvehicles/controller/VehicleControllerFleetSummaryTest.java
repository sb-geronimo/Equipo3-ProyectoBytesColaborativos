package es.bytescolab.msvehicles.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import es.bytescolab.msvehicles.dto.response.FleetSummaryResponse;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.EnumMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleControllerFleetSummaryTest {

    private MockMvc mockMvc;
    private VehicleService vehicleService;

    @BeforeEach
    void prepararMockMvc() {
        vehicleService = mock(VehicleService.class);
        VehicleController controller = new VehicleController(vehicleService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void getFleetSummary_devuelve200ConConteos() throws Exception {
        Map<VehicleStatus, Long> porEstado = new EnumMap<>(VehicleStatus.class);
        porEstado.put(VehicleStatus.AVAILABLE, 14L);
        porEstado.put(VehicleStatus.IN_USE, 8L);
        porEstado.put(VehicleStatus.IN_MAINTENANCE, 2L);
        porEstado.put(VehicleStatus.OUT_OF_SERVICE, 1L);
        Map<VehicleType, Long> porTipo = new EnumMap<>(VehicleType.class);
        porTipo.put(VehicleType.CAR, 9L);
        porTipo.put(VehicleType.VAN, 10L);
        porTipo.put(VehicleType.TRUCK, 4L);
        porTipo.put(VehicleType.MOTORCYCLE, 2L);
        when(vehicleService.getFleetSummary())
                .thenReturn(new FleetSummaryResponse(25L, porEstado, porTipo));

        mockMvc.perform(get("/api/vehicles/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.byStatus.AVAILABLE").value(14))
                .andExpect(jsonPath("$.byStatus.IN_USE").value(8))
                .andExpect(jsonPath("$.byStatus.IN_MAINTENANCE").value(2))
                .andExpect(jsonPath("$.byStatus.OUT_OF_SERVICE").value(1))
                .andExpect(jsonPath("$.byType.CAR").value(9))
                .andExpect(jsonPath("$.byType.VAN").value(10))
                .andExpect(jsonPath("$.byType.TRUCK").value(4))
                .andExpect(jsonPath("$.byType.MOTORCYCLE").value(2));
    }

    @Test
    void getFleetSummary_flotaVacia_devuelveMapaEnCero() throws Exception {
        when(vehicleService.getFleetSummary())
                .thenReturn(new FleetSummaryResponse(0L, null, null));

        mockMvc.perform(get("/api/vehicles/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.byStatus.AVAILABLE").value(0))
                .andExpect(jsonPath("$.byType.CAR").value(0));
    }
}
