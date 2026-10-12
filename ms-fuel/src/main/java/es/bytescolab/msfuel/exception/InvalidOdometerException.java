package es.bytescolab.msfuel.exception;

public class InvalidOdometerException extends FuelBusinessException {

    public InvalidOdometerException(int providedOdometerKm, int previousOdometerKm) {
        super("INVALID_ODOMETER",
                "El odómetro " + providedOdometerKm + " km es menor o igual que el del último "
                        + "repostaje del vehículo (" + previousOdometerKm + " km)");
    }
}
