package es.bytescolab.msvehicles.service.impl;

import es.bytescolab.msvehicles.dto.response.FleetSummaryResponse;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.mapper.VehicleMapper;
import es.bytescolab.msvehicles.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplFleetSummaryTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private VehicleServiceImpl service;

    @Test
    void getFleetSummary_agregaConteosPorEstadoYTipo() {
        when(vehicleRepository.count()).thenReturn(25L);
        Map<VehicleStatus, Long> esperadoEstados = new EnumMap<>(VehicleStatus.class);
        esperadoEstados.put(VehicleStatus.AVAILABLE, 14L);
        esperadoEstados.put(VehicleStatus.IN_USE, 8L);
        esperadoEstados.put(VehicleStatus.IN_MAINTENANCE, 2L);
        esperadoEstados.put(VehicleStatus.OUT_OF_SERVICE, 1L);
        for (Map.Entry<VehicleStatus, Long> entrada : esperadoEstados.entrySet()) {
            when(vehicleRepository.countByStatus(entrada.getKey())).thenReturn(entrada.getValue());
        }
        Map<VehicleType, Long> esperadoTipos = new EnumMap<>(VehicleType.class);
        esperadoTipos.put(VehicleType.CAR, 9L);
        esperadoTipos.put(VehicleType.VAN, 10L);
        esperadoTipos.put(VehicleType.TRUCK, 4L);
        esperadoTipos.put(VehicleType.MOTORCYCLE, 2L);
        for (Map.Entry<VehicleType, Long> entrada : esperadoTipos.entrySet()) {
            when(vehicleRepository.countByType(entrada.getKey())).thenReturn(entrada.getValue());
        }

        FleetSummaryResponse resumen = service.getFleetSummary();

        assertThat(resumen.total()).isEqualTo(25L);
        assertThat(resumen.byStatus()).containsAllEntriesOf(esperadoEstados);
        assertThat(resumen.byType()).containsAllEntriesOf(esperadoTipos);
    }

    @Test
    void getFleetSummary_sinVehiculos_devuelveConteosEnCero() {
        when(vehicleRepository.count()).thenReturn(0L);
        when(vehicleRepository.countByStatus(any())).thenReturn(0L);
        when(vehicleRepository.countByType(any())).thenReturn(0L);

        FleetSummaryResponse resumen = service.getFleetSummary();

        assertThat(resumen.total()).isZero();
        assertThat(resumen.byStatus()).containsOnlyKeys(VehicleStatus.values());
        assertThat(resumen.byStatus().values()).containsOnly(0L);
        assertThat(resumen.byType()).containsOnlyKeys(VehicleType.values());
        assertThat(resumen.byType().values()).containsOnly(0L);
    }
}
