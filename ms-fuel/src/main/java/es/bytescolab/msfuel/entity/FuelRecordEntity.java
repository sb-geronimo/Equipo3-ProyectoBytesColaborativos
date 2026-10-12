package es.bytescolab.msfuel.entity;

import es.bytescolab.msfuel.enums.FuelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "fuel_records",
        indexes = {
                @Index(name = "idx_fuel_records_vehicle_id", columnList = "vehicle_id"),
                @Index(name = "idx_fuel_records_refueled_at", columnList = "refueled_at"),
                @Index(name = "idx_fuel_records_fuel_type", columnList = "fuel_type")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FuelRecordEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "refueled_at", nullable = false)
    private Instant refueledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 255)
    private FuelType fuelType;

    @Column(name = "liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal liters;

    @Column(name = "price_per_liter", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerLiter;

    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "odometer_km", nullable = false)
    private Integer odometerKm;

    @Column(name = "full_tank", nullable = false)
    private Boolean fullTank;

    @Column(name = "station", length = 255)
    private String station;

    @Column(name = "consumption_l100_km", precision = 10, scale = 2)
    private BigDecimal consumptionL100km;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return this.isNew;
    }

}
