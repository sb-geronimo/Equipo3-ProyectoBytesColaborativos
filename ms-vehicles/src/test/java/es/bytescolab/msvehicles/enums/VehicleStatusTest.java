package es.bytescolab.msvehicles.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleStatusTest {

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void canTransitionTo_should_returnTrue_when_transitionIsAllowed(
            VehicleStatus origin, VehicleStatus target
    ) {
        assertThat(origin.canTransitionTo(target)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("deniedTransitions")
    void canTransitionTo_should_returnFalse_when_transitionIsDenied(
            VehicleStatus origin, VehicleStatus target
    ) {
        assertThat(origin.canTransitionTo(target)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = VehicleStatus.class, names = "OUT_OF_SERVICE")
    void canTransitionTo_should_returnFalse_when_originIsOutOfServiceAndTargetIsNotAvailable(
            VehicleStatus target
    ) {
        if (target == VehicleStatus.AVAILABLE) {
            return;
        }
        assertThat(VehicleStatus.OUT_OF_SERVICE.canTransitionTo(target)).isFalse();
    }

    static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(VehicleStatus.AVAILABLE, VehicleStatus.IN_USE),
                Arguments.of(VehicleStatus.AVAILABLE, VehicleStatus.IN_MAINTENANCE),
                Arguments.of(VehicleStatus.AVAILABLE, VehicleStatus.OUT_OF_SERVICE),
                Arguments.of(VehicleStatus.IN_USE, VehicleStatus.AVAILABLE),
                Arguments.of(VehicleStatus.IN_USE, VehicleStatus.OUT_OF_SERVICE),
                Arguments.of(VehicleStatus.IN_MAINTENANCE, VehicleStatus.AVAILABLE),
                Arguments.of(VehicleStatus.IN_MAINTENANCE, VehicleStatus.OUT_OF_SERVICE),
                Arguments.of(VehicleStatus.OUT_OF_SERVICE, VehicleStatus.AVAILABLE)
        );
    }

    static Stream<Arguments> deniedTransitions() {
        return Stream.of(
                Arguments.of(VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE),
                Arguments.of(VehicleStatus.IN_USE, VehicleStatus.IN_USE),
                Arguments.of(VehicleStatus.IN_USE, VehicleStatus.IN_MAINTENANCE),
                Arguments.of(VehicleStatus.IN_MAINTENANCE, VehicleStatus.IN_MAINTENANCE),
                Arguments.of(VehicleStatus.IN_MAINTENANCE, VehicleStatus.IN_USE),
                Arguments.of(VehicleStatus.OUT_OF_SERVICE, VehicleStatus.IN_USE),
                Arguments.of(VehicleStatus.OUT_OF_SERVICE, VehicleStatus.IN_MAINTENANCE),
                Arguments.of(VehicleStatus.OUT_OF_SERVICE, VehicleStatus.OUT_OF_SERVICE)
        );
    }
}
