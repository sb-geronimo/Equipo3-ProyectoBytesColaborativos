package es.bytescolab.msfuel.service.impl;

import es.bytescolab.msfuel.client.VehicleClient;
import es.bytescolab.msfuel.dto.response.RefuelResponse;
import es.bytescolab.msfuel.entity.FuelRecordEntity;
import es.bytescolab.msfuel.exception.InvalidExportRangeException;
import es.bytescolab.msfuel.mapper.FuelRecordMapper;
import es.bytescolab.msfuel.repository.FuelRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FuelRecordServiceExportTest {

    private final FuelRecordRepository fuelRecordRepository = mock(FuelRecordRepository.class);
    private final VehicleClient vehicleClient = mock(VehicleClient.class);
    private final FuelRecordMapper fuelRecordMapper = mock(FuelRecordMapper.class);
    private final FuelRecordServiceImpl fuelRecordService =
            new FuelRecordServiceImpl(fuelRecordRepository, vehicleClient, fuelRecordMapper);

    @Test
    void findForExport_rangoInvertidoLanzaExcepcion() {
        LocalDate from = LocalDate.parse("2026-09-30");
        LocalDate to = LocalDate.parse("2026-09-01");

        assertThrows(InvalidExportRangeException.class,
                () -> fuelRecordService.findForExport(null, from, to));
    }

    @Test
    void findForExport_limitaADiezMilFilas() {
        List<FuelRecordEntity> stored = new ArrayList<>();
        for (int i = 0; i < 10005; i++) {
            stored.add(FuelRecordEntity.builder().build());
        }
        when(fuelRecordRepository.findRecordsInRange(
                isNull(), any(), any(), any(Sort.class))).thenReturn(stored);
        when(fuelRecordMapper.toResponse(any(FuelRecordEntity.class)))
                .thenReturn(mock(RefuelResponse.class));

        List<RefuelResponse> exported = fuelRecordService.findForExport(
                null, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"));

        assertEquals(10000, exported.size());
    }

    @Test
    void findForExport_sinFiltrosDevuelveTodo() {
        FuelRecordEntity stored = FuelRecordEntity.builder()
                .vehicleId(UUID.randomUUID())
                .build();
        RefuelResponse mapped = mock(RefuelResponse.class);
        when(fuelRecordRepository.findRecordsInRange(
                isNull(), isNull(), isNull(), any(Sort.class))).thenReturn(List.of(stored));
        when(fuelRecordMapper.toResponse(stored)).thenReturn(mapped);

        List<RefuelResponse> exported = fuelRecordService.findForExport(null, null, null);

        assertEquals(List.of(mapped), exported);
    }
}
