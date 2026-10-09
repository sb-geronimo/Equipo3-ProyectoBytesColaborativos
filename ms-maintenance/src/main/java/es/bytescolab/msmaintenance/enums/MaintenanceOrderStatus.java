package es.bytescolab.msmaintenance.enums;

import java.util.List;

public enum MaintenanceOrderStatus {
    PENDING, IN_PROGRESS, COMPLETED;


    public static List<MaintenanceOrderStatus> openStatuses() {
        return List.of(MaintenanceOrderStatus.PENDING, MaintenanceOrderStatus.IN_PROGRESS);
    }
}
