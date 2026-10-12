package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.client.DriverClient;
import es.bytescolab.msalerts.dto.response.client.DriverSummaryResponse;
import es.bytescolab.msalerts.dto.response.client.PageResponse;
import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseEvaluator implements AlertEvaluator {

    private static final int PAGE_SIZE = 100;

    private final DriverClient driverClient;
    private final Clock clock;

    @Override
    public List<AlertCandidate> evaluate(AlertRuleEntity rule) {
        LocalDate today = LocalDate.now(clock);
        return switch (rule.getType()) {
            case LICENSE_EXPIRING -> evaluateExpiring(rule, today);
            case LICENSE_EXPIRED -> evaluateExpired(rule, today);
            default -> List.of();
        };
    }

    private List<AlertCandidate> evaluateExpiring(AlertRuleEntity rule, LocalDate today) {
        int daysBefore = rule.getParams().path("daysBefore").asInt(30);
        List<DriverSummaryResponse> drivers = fetchDriversExpiring(daysBefore);
        List<AlertCandidate> candidates = new ArrayList<>(drivers.size());
        for (DriverSummaryResponse driver : drivers) {
            if (driver.licenseExpiresAt() == null || driver.licenseExpiresAt().isBefore(today)) {
                continue;
            }
            String message = "La licencia de " + driver.fullName()
                    + " caduca el " + driver.licenseExpiresAt();
            candidates.add(new AlertCandidate(
                    AlertType.LICENSE_EXPIRING,
                    null,
                    driver.id(),
                    AlertDedupKeyBuilder.build(AlertType.LICENSE_EXPIRING, driver.id()),
                    message,
                    rule.getSeverity()
            ));
        }
        return candidates;
    }

    private List<AlertCandidate> evaluateExpired(AlertRuleEntity rule, LocalDate today) {
        List<DriverSummaryResponse> all = fetchAllDrivers();
        List<AlertCandidate> candidates = new ArrayList<>();
        for (DriverSummaryResponse driver : all) {
            if (driver.licenseExpiresAt() == null || !driver.licenseExpiresAt().isBefore(today)) {
                continue;
            }
            String message = "La licencia de " + driver.fullName()
                    + " caducó el " + driver.licenseExpiresAt();
            candidates.add(new AlertCandidate(
                    AlertType.LICENSE_EXPIRED,
                    null,
                    driver.id(),
                    AlertDedupKeyBuilder.build(AlertType.LICENSE_EXPIRED, driver.id()),
                    message,
                    rule.getSeverity()
            ));
        }
        return candidates;
    }

    private List<DriverSummaryResponse> fetchDriversExpiring(int daysBefore) {
        return paginate(page -> driverClient.listDrivers(daysBefore, page, PAGE_SIZE));
    }

    private List<DriverSummaryResponse> fetchAllDrivers() {
        return paginate(page -> driverClient.listDrivers(null, page, PAGE_SIZE));
    }

    private List<DriverSummaryResponse> paginate(java.util.function.Function<Integer, PageResponse<DriverSummaryResponse>> fetcher) {
        List<DriverSummaryResponse> all = new ArrayList<>();
        int page = 0;
        while (true) {
            PageResponse<DriverSummaryResponse> response = fetcher.apply(page);
            all.addAll(response.content());
            if (response.content().isEmpty() || page >= response.totalPages() - 1) {
                break;
            }
            page++;
        }
        return all;
    }
}