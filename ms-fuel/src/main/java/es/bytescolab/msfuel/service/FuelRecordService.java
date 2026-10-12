package es.bytescolab.msfuel.service;

import es.bytescolab.msfuel.dto.request.RefuelConsumptionRequest;
import es.bytescolab.msfuel.dto.request.RefuelCreateRequest;
import es.bytescolab.msfuel.dto.request.RefuelStatsRequest;
import es.bytescolab.msfuel.dto.response.ConsumptionResponse;
import es.bytescolab.msfuel.dto.response.FuelStatsResponse;
import es.bytescolab.msfuel.dto.response.RefuelResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface FuelRecordService {

    RefuelResponse create(RefuelCreateRequest request);

    Page<RefuelResponse> findAll(UUID vehicle, LocalDate from, LocalDate to, int page, int size);

    ConsumptionResponse getConsumption(RefuelConsumptionRequest request);

    FuelStatsResponse getStats(RefuelStatsRequest request);
}
