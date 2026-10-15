package es.bytescolab.msdashboard.enums;

public enum DashboardMetric {

    DISTANCE_KM("km"),
    FUEL_LITERS("L"),
    FUEL_COST("EUR"),
    CONSUMPTION_L100KM("L/100km"),
    MAINTENANCE_COST("EUR"),
    ALERTS_CREATED("alerts");

    private final String unidad;

    DashboardMetric(String unidad) {
        this.unidad = unidad;
    }

    public String unidad() {
        return unidad;
    }
}
