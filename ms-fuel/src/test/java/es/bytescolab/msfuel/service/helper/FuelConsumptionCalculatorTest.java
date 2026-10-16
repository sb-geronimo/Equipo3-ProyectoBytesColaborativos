package es.bytescolab.msfuel.service.helper;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FuelConsumptionCalculatorTest {

    @ParameterizedTest(name = "liters={0}, km={1} -> expected={2} L/100km")
    @MethodSource("happyPathCases")
    void calculateLitersPer100Km_should_returnExpected_when_inputsValid(
            BigDecimal liters, long km, BigDecimal expected) {
        assertThat(FuelConsumptionCalculator.calculateLitersPer100Km(liters, km))
                .isEqualByComparingTo(expected);
    }

    static Stream<Arguments> happyPathCases() {
        return Stream.of(
                Arguments.of(new BigDecimal("50"), 500L, new BigDecimal("10.00")),
                Arguments.of(new BigDecimal("33"), 400L, new BigDecimal("8.25"))
        );
    }

    @ParameterizedTest(name = "liters={0}, km={1} -> null")
    @MethodSource("edgeCases")
    void calculateLitersPer100Km_should_returnNull_when_invalidOrBoundaryInputs(
            BigDecimal liters, long km) {
        assertThat(FuelConsumptionCalculator.calculateLitersPer100Km(liters, km))
                .isNull();
    }

    static Stream<Arguments> edgeCases() {
        return Stream.of(
                Arguments.of((BigDecimal) null, 100L),
                Arguments.of(BigDecimal.ZERO, 100L),
                Arguments.of(new BigDecimal("-5"), 100L),
                Arguments.of(new BigDecimal("10"), 0L),
                Arguments.of(new BigDecimal("10"), -1L)
        );
    }
}