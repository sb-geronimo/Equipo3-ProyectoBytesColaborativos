package es.bytescolab.msfuel.service.impl;

import es.bytescolab.msfuel.client.VehicleClient;
import es.bytescolab.msfuel.dto.request.RefuelConsumptionRequest;
import es.bytescolab.msfuel.dto.request.RefuelCreateRequest;
import es.bytescolab.msfuel.dto.request.RefuelStatsRequest;
import es.bytescolab.msfuel.dto.response.ConsumptionResponse;
import es.bytescolab.msfuel.dto.response.FuelStatsPoint;
import es.bytescolab.msfuel.dto.response.FuelStatsResponse;
import es.bytescolab.msfuel.dto.response.FuelStatsTotals;
import es.bytescolab.msfuel.dto.response.RefuelResponse;
import es.bytescolab.msfuel.dto.response.VehicleConsumption;
import es.bytescolab.msfuel.dto.response.VehicleDetailResponse;
import es.bytescolab.msfuel.entity.FuelRecordEntity;
import es.bytescolab.msfuel.exception.InvalidOdometerException;
import es.bytescolab.msfuel.exception.LitersExceedTankCapacityException;
import es.bytescolab.msfuel.exception.VehicleNotFoundException;
import es.bytescolab.msfuel.exception.VehiclesServiceUnavailableException;
import es.bytescolab.msfuel.mapper.FuelRecordMapper;
import es.bytescolab.msfuel.repository.FuelRecordRepository;
import es.bytescolab.msfuel.service.FuelRecordService;
import es.bytescolab.msfuel.service.helper.FuelConsumptionCalculator;
import es.bytescolab.msfuel.service.helper.FuelTimeSeriesBuilder;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuelRecordServiceImpl implements FuelRecordService {

    private static final String MS_VEHICLES = "ms-vehicles";
    private static final String DEFAULT_GRANULARITY = "DAY";

    private final FuelRecordRepository fuelRecordRepository;
    private final VehicleClient vehicleClient;
    private final FuelRecordMapper fuelRecordMapper;

    @Override
    @Transactional
    public RefuelResponse create(RefuelCreateRequest request) {
        VehicleDetailResponse vehicle = fetchVehicleOrThrow(request.vehicleId());
        ensureTankCapacitySufficient(vehicle.tankCapacityL(), request.liters());
        ensureOdometerGreaterThanPrevious(
                request.vehicleId(), request.refueledAt(), request.odometerKm());

        FuelRecordEntity entity = fuelRecordMapper.toEntity(request);
        entity.setFuelType(vehicle.fuelType());
        entity.setConsumptionL100km(
                resolveConsumptionL100km(
                        request.vehicleId(),
                        request.refueledAt(),
                        request.odometerKm(),
                        request.fullTank()));
        entity.setNew(false);

        FuelRecordEntity saved = fuelRecordRepository.save(entity);
        log.info("Repostaje registrado: id={}, vehicleId={}, liters={}, totalCost={}",
                saved.getId(), saved.getVehicleId(), saved.getLiters(), saved.getTotalCost());
        return fuelRecordMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RefuelResponse> findAll(UUID vehicle, LocalDate from, LocalDate to, int page, int size) {
        Instant fromInstant = toInstantStartOfDay(from);
        Instant toInstant = toInstantStartOfDayAfter(to);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "refueledAt"));
        return fuelRecordRepository
                .findByFilters(vehicle, fromInstant, toInstant, pageable)
                .map(fuelRecordMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsumptionResponse getConsumption(RefuelConsumptionRequest request) {
        Instant fromInstant = toInstantStartOfDay(request.from());
        Instant toInstant = toInstantStartOfDayAfter(request.to());
        List<FuelRecordEntity> refuels = fuelRecordRepository.findRecordsInRange(
                null, fromInstant, toInstant, Sort.by(Sort.Direction.ASC, "refueledAt"));

        Map<UUID, List<FuelRecordEntity>> byVehicle = refuels.stream()
                .collect(Collectors.groupingBy(FuelRecordEntity::getVehicleId));

        List<VehicleConsumption> vehicles = byVehicle.entrySet().stream()
                .map(entry -> buildVehicleConsumption(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(VehicleConsumption::cost).reversed())
                .toList();

        return new ConsumptionResponse(request.from(), request.to(), vehicles);
    }

    @Override
    @Transactional(readOnly = true)
    public FuelStatsResponse getStats(RefuelStatsRequest request) {
        String granularity = normalizeGranularity(request.granularity());
        Instant fromInstant = toInstantStartOfDay(request.from());
        Instant toInstant = toInstantStartOfDayAfter(request.to());

        List<FuelRecordEntity> refuels = fuelRecordRepository.findRecordsInRange(
                request.vehicle(), fromInstant, toInstant,
                Sort.by(Sort.Direction.ASC, "refueledAt"));

        FuelStatsTotals totals = FuelTimeSeriesBuilder.computeTotals(refuels);
        List<LocalDate> periods = FuelTimeSeriesBuilder.fillPeriods(
                request.from(), request.to(), granularity);
        List<FuelStatsPoint> series = FuelTimeSeriesBuilder.buildSeries(
                refuels, periods, granularity);

        return new FuelStatsResponse(request.from(), request.to(), granularity, totals, series);
    }

    private VehicleDetailResponse fetchVehicleOrThrow(UUID vehicleId) {
        try {
            return vehicleClient.getVehicle(vehicleId);
        } catch (FeignException.NotFound ex) {
            throw new VehicleNotFoundException(vehicleId);
        } catch (FeignException ex) {
            throw new VehiclesServiceUnavailableException(
                    "ms-vehicles no está disponible");
        }
    }

    private void ensureTankCapacitySufficient(Integer tankCapacityL, BigDecimal liters) {
        if (tankCapacityL == null || liters == null) {
            return;
        }
        if (liters.compareTo(BigDecimal.valueOf(tankCapacityL)) > 0) {
            throw new LitersExceedTankCapacityException(liters, tankCapacityL);
        }
    }

    private void ensureOdometerGreaterThanPrevious(
            UUID vehicleId, Instant refueledAt, Integer odometerKm) {
        Optional<FuelRecordEntity> previous =
                fuelRecordRepository.findPreviousRefuelBefore(vehicleId, refueledAt);
        if (previous.isEmpty()) {
            return;
        }
        Integer previousOdometerKm = previous.get().getOdometerKm();
        if (previousOdometerKm != null && previousOdometerKm >= odometerKm) {
            throw new InvalidOdometerException(odometerKm, previousOdometerKm);
        }
    }

    private BigDecimal resolveConsumptionL100km(
            UUID vehicleId, Instant refueledAt, Integer currentOdometerKm, Boolean isFullTank) {
        if (!Boolean.TRUE.equals(isFullTank) || currentOdometerKm == null) {
            return null;
        }
        Optional<FuelRecordEntity> previousFullTank =
                fuelRecordRepository.findLastFullTankAtOrBefore(vehicleId, refueledAt);
        if (previousFullTank.isEmpty()) {
            return null;
        }
        FuelRecordEntity prev = previousFullTank.get();
        Integer prevOdometerKm = prev.getOdometerKm();
        if (prevOdometerKm == null || currentOdometerKm <= prevOdometerKm) {
            return null;
        }
        long kmBetween = currentOdometerKm.longValue() - prevOdometerKm.longValue();
        BigDecimal litersBetween = sumLitersBetweenRefuels(
                vehicleId, prev.getRefueledAt(), refueledAt);
        return FuelConsumptionCalculator.calculateLitersPer100Km(litersBetween, kmBetween);
    }

    private BigDecimal sumLitersBetweenRefuels(
            UUID vehicleId, Instant fromExclusive, Instant toInclusive) {
        return fuelRecordRepository
                .findRefuelsStrictlyBetween(vehicleId, fromExclusive, toInclusive)
                .stream()
                .map(FuelRecordEntity::getLiters)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private VehicleConsumption buildVehicleConsumption(
            UUID vehicleId, List<FuelRecordEntity> vehicleRefuels) {
        VehicleDetailResponse vehicle = fetchVehicleOrThrow(vehicleId);
        BigDecimal liters = sumLiters(vehicleRefuels);
        BigDecimal cost = sumCost(vehicleRefuels);
        Integer distanceKm = distanceKm(vehicleRefuels);
        BigDecimal avgL100km = averageLitersPer100Km(liters, distanceKm);
        return new VehicleConsumption(
                vehicleId,
                vehicle.plate(),
                vehicle.type(),
                liters.setScale(2, RoundingMode.HALF_UP),
                cost.setScale(2, RoundingMode.HALF_UP),
                distanceKm,
                avgL100km
        );
    }

    private static BigDecimal sumLiters(List<FuelRecordEntity> refuels) {
        return refuels.stream()
                .map(FuelRecordEntity::getLiters)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumCost(List<FuelRecordEntity> refuels) {
        return refuels.stream()
                .map(FuelRecordEntity::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Integer distanceKm(List<FuelRecordEntity> refuels) {
        List<Integer> odometers = refuels.stream()
                .map(FuelRecordEntity::getOdometerKm)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (odometers.size() < 2) {
            return 0;
        }
        return odometers.get(odometers.size() - 1) - odometers.get(0);
    }

    private static BigDecimal averageLitersPer100Km(BigDecimal totalLiters, Integer distanceKm) {
        if (distanceKm == null || distanceKm <= 0
                || totalLiters == null || totalLiters.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return totalLiters
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(distanceKm), 2, RoundingMode.HALF_UP);
    }

    private static Instant toInstantStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant toInstantStartOfDayAfter(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static String normalizeGranularity(String granularity) {
        return (granularity == null || granularity.isBlank())
                ? DEFAULT_GRANULARITY
                : granularity.toUpperCase();
    }
}
