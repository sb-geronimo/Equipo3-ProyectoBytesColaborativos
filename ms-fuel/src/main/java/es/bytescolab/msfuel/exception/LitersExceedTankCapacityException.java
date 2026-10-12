package es.bytescolab.msfuel.exception;

import java.math.BigDecimal;

public class LitersExceedTankCapacityException extends FuelBusinessException {

    public LitersExceedTankCapacityException(BigDecimal liters, int tankCapacityL) {
        super("LITERS_EXCEED_TANK_CAPACITY",
                "Los " + liters + " litros repostados superan la capacidad del depósito ("
                        + tankCapacityL + " L)");
    }
}
