package es.bytescolab.msvehicles.enums;

public enum VehicleStatus {
    AVAILABLE, IN_USE, IN_MAINTENANCE, OUT_OF_SERVICE;

    public boolean canTransitionTo(VehicleStatus target) {
        return switch (this) {
            case AVAILABLE -> target == IN_USE
                              || target == IN_MAINTENANCE
                              || target == OUT_OF_SERVICE;

            case IN_USE, IN_MAINTENANCE -> target == AVAILABLE
                                           || target == OUT_OF_SERVICE;

            case OUT_OF_SERVICE -> target == AVAILABLE;
        };
    }
}
