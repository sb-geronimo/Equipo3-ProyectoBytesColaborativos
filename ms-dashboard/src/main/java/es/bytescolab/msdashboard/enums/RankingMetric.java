package es.bytescolab.msdashboard.enums;

public enum RankingMetric {

    FUEL_COST("EUR"),
    CONSUMPTION("L/100km");

    private final String unidad;

    RankingMetric(String unidad) {
        this.unidad = unidad;
    }

    public String unidad() {
        return unidad;
    }
}
