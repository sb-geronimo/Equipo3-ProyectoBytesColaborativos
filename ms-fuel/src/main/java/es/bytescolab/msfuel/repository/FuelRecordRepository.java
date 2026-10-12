package es.bytescolab.msfuel.repository;

import es.bytescolab.msfuel.entity.FuelRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuelRecordRepository extends JpaRepository<FuelRecordEntity, UUID>, JpaSpecificationExecutor<FuelRecordEntity> {

    default Page<FuelRecordEntity> findByFilters(UUID vehicle, Instant fromInclusive, Instant toExclusive, Pageable pageable) {
        return this.findAll(FuelRecordSpecifications.inRange(vehicle, fromInclusive, toExclusive), pageable);
    }

    default List<FuelRecordEntity> findRecordsInRange(UUID vehicle, Instant fromInclusive, Instant toExclusive, Sort sort) {
        return this.findAll(FuelRecordSpecifications.inRange(vehicle, fromInclusive, toExclusive), sort);
    }

    default Optional<FuelRecordEntity> findPreviousRefuelBefore(UUID vehicleId, Instant before) {
        Specification<FuelRecordEntity> spec = FuelRecordSpecifications.previousRefuelBefore(vehicleId, before);
        return this.findAll(spec, Sort.by(Sort.Direction.DESC, "refueledAt"))
                .stream()
                .findFirst();
    }

    default Optional<FuelRecordEntity> findLastFullTankAtOrBefore(UUID vehicleId, Instant atOrBefore) {
        Specification<FuelRecordEntity> spec = FuelRecordSpecifications.lastFullTankAtOrBefore(vehicleId, atOrBefore);
        return this.findAll(spec, Sort.by(Sort.Direction.DESC, "refueledAt"))
                .stream()
                .findFirst();
    }

    default List<FuelRecordEntity> findRefuelsStrictlyBetween(
            UUID vehicleId, Instant fromExclusive, Instant toInclusive) {
        Specification<FuelRecordEntity> spec =
                FuelRecordSpecifications.refuelsStrictlyBetween(vehicleId, fromExclusive, toInclusive);
        return this.findAll(spec, Sort.by(Sort.Direction.ASC, "refueledAt"));
    }
}
