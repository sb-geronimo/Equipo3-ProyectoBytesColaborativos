package es.bytescolab.msdashboard.service.impl;

import es.bytescolab.msdashboard.client.FuelClient;
import es.bytescolab.msdashboard.dto.response.RankingResponse;
import es.bytescolab.msdashboard.dto.response.client.FuelConsumption;
import es.bytescolab.msdashboard.enums.RankingMetric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceImplTest {

    @Mock
    private FuelClient fuelClient;

    @Test
    void fuelCost_ordenaDescendenteYAplicaLimite() {
        when(fuelClient.getConsumption(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(consumo());

        RankingServiceImpl servicio = new RankingServiceImpl(fuelClient);

        RankingResponse respuesta = servicio.getRanking(RankingMetric.FUEL_COST, 30, 2, null);

        assertThat(respuesta.ranking()).hasSize(2);
        assertThat(respuesta.ranking().get(0).rank()).isEqualTo(1);
        assertThat(respuesta.ranking().get(0).plate()).isEqualTo("9999-ZZZ");
        assertThat(respuesta.ranking().get(0).value()).isEqualByComparingTo("2400.00");
        assertThat(respuesta.ranking().get(1).rank()).isEqualTo(2);
        assertThat(respuesta.ranking().get(1).plate()).isEqualTo("4821-KDF");
        assertThat(respuesta.ranking().get(1).value()).isEqualByComparingTo("1118.29");
    }

    @Test
    void filtroTipo_aplicaFiltro() {
        when(fuelClient.getConsumption(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(consumo());

        RankingServiceImpl servicio = new RankingServiceImpl(fuelClient);

        RankingResponse respuesta = servicio.getRanking(
                RankingMetric.FUEL_COST, 30, 5, "VAN");

        assertThat(respuesta.ranking()).hasSize(2);
        assertThat(respuesta.ranking()).extracting(item -> item.type())
                .containsOnly("VAN");
    }

    @Test
    void consumption_ordenaDescendentePorL100() {
        when(fuelClient.getConsumption(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(consumo());

        RankingServiceImpl servicio = new RankingServiceImpl(fuelClient);

        RankingResponse respuesta = servicio.getRanking(RankingMetric.CONSUMPTION, 30, 5, null);

        assertThat(respuesta.ranking().get(0).plate()).isEqualTo("9999-ZZZ");
        assertThat(respuesta.ranking().get(0).value()).isEqualByComparingTo("15.20");
    }

    @Test
    void limitFueraDeRango_lanzaIllegalArgument() {
        RankingServiceImpl servicio = new RankingServiceImpl(fuelClient);

        assertThatThrownBy(() -> servicio.getRanking(RankingMetric.FUEL_COST, 30, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
        assertThatThrownBy(() -> servicio.getRanking(RankingMetric.FUEL_COST, 30, 25, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void daysFueraDeRango_lanzaIllegalArgument() {
        RankingServiceImpl servicio = new RankingServiceImpl(fuelClient);

        assertThatThrownBy(() -> servicio.getRanking(RankingMetric.FUEL_COST, 0, 5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days");
        assertThatThrownBy(() -> servicio.getRanking(RankingMetric.FUEL_COST, 400, 5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days");
    }

    private static FuelConsumption consumo() {
        return new FuelConsumption(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1),
                List.of(
                        new FuelConsumption.VehicleFuelConsumption(
                                "11111111-1111-1111-1111-111111111111",
                                "4821-KDF", "VAN",
                                new BigDecimal("690.3"),
                                new BigDecimal("1118.29"),
                                6310, new BigDecimal("10.9")),
                        new FuelConsumption.VehicleFuelConsumption(
                                "22222222-2222-2222-2222-222222222222",
                                "1234-ABC", "VAN",
                                new BigDecimal("420.0"),
                                new BigDecimal("850.50"),
                                3800, new BigDecimal("11.0")),
                        new FuelConsumption.VehicleFuelConsumption(
                                "33333333-3333-3333-3333-333333333333",
                                "9999-ZZZ", "TRUCK",
                                new BigDecimal("1200.0"),
                                new BigDecimal("2400.00"),
                                7900, new BigDecimal("15.20")),
                        new FuelConsumption.VehicleFuelConsumption(
                                "44444444-4444-4444-4444-444444444444",
                                "7777-GHI", "CAR",
                                new BigDecimal("100.0"),
                                new BigDecimal("150.00"),
                                1200, new BigDecimal("8.30"))
                ));
    }
}
