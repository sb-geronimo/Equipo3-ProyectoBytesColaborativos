package es.bytescolab.msroutes.service.impl;

import es.bytescolab.msroutes.client.DriverClient;
import es.bytescolab.msroutes.client.VehicleClient;
import es.bytescolab.msroutes.dto.response.RouteResponse;
import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.exception.InvalidExportRangeException;
import es.bytescolab.msroutes.mapper.RouteMapper;
import es.bytescolab.msroutes.repository.RouteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceExportTest {

    @Mock
    private RouteMapper mapper;

    @Mock
    private DriverClient driverClient;

    @Mock
    private VehicleClient vehicleClient;

    @Mock
    private RouteRepository repository;

    @InjectMocks
    private RouteServiceImpl routeService;

    @Test
    void findForExport_rangoInvertidoLanzaExcepcion() {
        LocalDate from = LocalDate.parse("2026-10-31");
        LocalDate to = LocalDate.parse("2026-10-01");

        assertThatThrownBy(() -> routeService.findForExport(null, null, null, from, to))
                .isInstanceOf(InvalidExportRangeException.class);
    }

    @Test
    void findForExport_limitaADiezMilFilas() {
        List<RouteEntity> stored = new ArrayList<>();
        for (int i = 0; i < 10005; i++) {
            stored.add(new RouteEntity());
        }
        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(stored);
        when(mapper.toResponse(any(RouteEntity.class))).thenReturn(mock(RouteResponse.class));

        List<RouteResponse> exported = routeService.findForExport(
                null, null, null, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));

        assertThat(exported).hasSize(10000);
    }

    @Test
    void findForExport_sinFiltrosDevuelveTodo() {
        RouteEntity stored = new RouteEntity();
        RouteResponse mapped = mock(RouteResponse.class);
        when(repository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(stored));
        when(mapper.toResponse(stored)).thenReturn(mapped);

        List<RouteResponse> exported = routeService.findForExport(null, null, null, null, null);

        assertThat(exported).containsExactly(mapped);
    }
}
