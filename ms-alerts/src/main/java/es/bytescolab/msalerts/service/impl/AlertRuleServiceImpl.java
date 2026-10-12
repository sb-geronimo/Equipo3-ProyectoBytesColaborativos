package es.bytescolab.msalerts.service.impl;

import es.bytescolab.msalerts.dto.request.UpdateAlertRuleRequest;
import es.bytescolab.msalerts.dto.response.AlertRuleResponse;
import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.exception.AlertRuleNotFoundException;
import es.bytescolab.msalerts.exception.InvalidAlertRuleParamsException;
import es.bytescolab.msalerts.mapper.AlertRuleMapper;
import es.bytescolab.msalerts.repository.AlertRuleRepository;
import es.bytescolab.msalerts.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleServiceImpl implements AlertRuleService {

    private static final int MIN_DAYS_BEFORE = 1;
    private static final int MAX_DAYS_BEFORE = 365;
    private static final int MIN_THRESHOLD_PERCENT = 1;
    private static final int MAX_THRESHOLD_PERCENT = 200;

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRuleMapper alertRuleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AlertRuleResponse> listRules() {
        return alertRuleRepository.findAll().stream()
                .sorted(Comparator.comparing(AlertRuleEntity::getType))
                .map(alertRuleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AlertRuleResponse updateRule(AlertType type, UpdateAlertRuleRequest request) {
        AlertRuleEntity rule = alertRuleRepository.findById(type)
                .orElseThrow(() -> new AlertRuleNotFoundException(type));

        validateParams(type, request.params());

        alertRuleMapper.updateEntity(rule, request);
        rule.setUpdatedAt(Instant.now());
        AlertRuleEntity saved = alertRuleRepository.save(rule);
        log.info("Regla {} actualizada: enabled={}, severity={}, params={}",
                type, saved.getEnabled(), saved.getSeverity(), saved.getParams());
        return alertRuleMapper.toResponse(saved);
    }

    private void validateParams(AlertType type, Map<String, Object> params) {
        Map<String, Object> effective = params == null ? Map.of() : params;
        switch (type) {
            case MAINTENANCE_DUE, LICENSE_EXPIRING -> requireDaysBefore(type, effective);
            case HIGH_CONSUMPTION -> requireThresholdPercent(type, effective);
            case MAINTENANCE_OVERDUE, LICENSE_EXPIRED -> rejectParamsIfNotEmpty(type, effective);
        }
    }

    private void requireDaysBefore(AlertType type, Map<String, Object> params) {
        Object value = params.get("daysBefore");
        if (value == null) {
            throw new InvalidAlertRuleParamsException(type, "falta el campo 'daysBefore'");
        }
        if (!(value instanceof Number number)) {
            throw new InvalidAlertRuleParamsException(type, "'daysBefore' debe ser numérico");
        }
        int days = number.intValue();
        if (days < MIN_DAYS_BEFORE || days > MAX_DAYS_BEFORE) {
            throw new InvalidAlertRuleParamsException(type,
                    "'daysBefore' debe estar entre " + MIN_DAYS_BEFORE + " y " + MAX_DAYS_BEFORE);
        }
    }

    private void requireThresholdPercent(AlertType type, Map<String, Object> params) {
        Object value = params.get("thresholdPercent");
        if (value == null) {
            throw new InvalidAlertRuleParamsException(type, "falta el campo 'thresholdPercent'");
        }
        if (!(value instanceof Number number)) {
            throw new InvalidAlertRuleParamsException(type, "'thresholdPercent' debe ser numérico");
        }
        int percent = number.intValue();
        if (percent < MIN_THRESHOLD_PERCENT || percent > MAX_THRESHOLD_PERCENT) {
            throw new InvalidAlertRuleParamsException(type,
                    "'thresholdPercent' debe estar entre " + MIN_THRESHOLD_PERCENT + " y " + MAX_THRESHOLD_PERCENT);
        }
    }

    private void rejectParamsIfNotEmpty(AlertType type, Map<String, Object> params) {
        if (!params.isEmpty()) {
            throw new InvalidAlertRuleParamsException(type,
                    "esta regla no admite parámetros");
        }
    }
}