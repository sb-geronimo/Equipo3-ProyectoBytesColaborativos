package es.bytescolab.msalerts.controller;

import es.bytescolab.msalerts.dto.request.AcknowledgeAlertRequest;
import es.bytescolab.msalerts.dto.request.UpdateAlertRuleRequest;
import es.bytescolab.msalerts.dto.response.AlertResponse;
import es.bytescolab.msalerts.dto.response.AlertRuleResponse;
import es.bytescolab.msalerts.dto.response.EvaluationSummaryResponse;
import es.bytescolab.msalerts.dto.response.ErrorResponse;
import es.bytescolab.msalerts.dto.response.PageResponse;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.service.AlertEvaluationService;
import es.bytescolab.msalerts.service.AlertQueryService;
import es.bytescolab.msalerts.service.AlertRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Alerts", description = "Listado, reconocimiento, evaluación y configuración de alertas")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts")
public class AlertController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_PAGE_SIZE = "20";

    private final AlertQueryService alertQueryService;
    private final AlertRuleService alertRuleService;
    private final AlertEvaluationService alertEvaluationService;

    @Operation(
            summary = "Listar alertas paginadas",
            description = "Devuelve la página de alertas filtrada por estado, severidad, tipo y/o vehículo, "
                    + "ordenada por severidad descendente (CRITICAL → WARNING → INFO) y fecha de creación descendente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de alertas",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<AlertResponse>> findAll(
            @Parameter(description = "Filtro por estado de la alerta", example = "OPEN")
            @RequestParam(value = "status", required = false) AlertStatus status,

            @Parameter(description = "Filtro por severidad", example = "CRITICAL")
            @RequestParam(value = "severity", required = false) AlertSeverity severity,

            @Parameter(description = "Filtro por tipo de alerta", example = "MAINTENANCE_OVERDUE")
            @RequestParam(value = "type", required = false) AlertType type,

            @Parameter(description = "Filtro por vehículo afectado")
            @RequestParam(value = "vehicle", required = false) UUID vehicle,

            @Parameter(description = "Número de página (0 en adelante)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página (por defecto 20, máximo 100)", example = "20")
            @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        log.info("Petición GET /api/alerts — status={}, severity={}, type={}, vehicle={}, page={}, size={}",
                status, severity, type, vehicle, page, safeSize);
        PageResponse<AlertResponse> response = alertQueryService.findAll(status, severity, type, vehicle, page, safeSize);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reconocer una alerta",
            description = "Transiciona la alerta de OPEN a ACKNOWLEDGED guardando el usuario, la fecha y el comentario opcional."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta reconocida",
                    content = @Content(schema = @Schema(implementation = AlertResponse.class))),
            @ApiResponse(responseCode = "404", description = "Alerta no encontrada (ALERT_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Estado no permitido (INVALID_ALERT_STATE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledge(
            @Parameter(description = "UUID de la alerta")
            @PathVariable UUID alertId,

            @RequestBody(required = false) @Valid AcknowledgeAlertRequest request,
            Authentication authentication
    ) {
        String comment = request == null ? null : request.comment();
        UUID currentUserId = parseUserId(authentication);
        log.info("Petición POST /api/alerts/{}/acknowledge — usuario={}", alertId, currentUserId);
        AlertResponse response = alertQueryService.acknowledge(alertId, comment, currentUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Listar reglas de evaluación",
            description = "Devuelve la configuración actual de las 5 reglas de evaluación de alertas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de reglas",
                    content = @Content(schema = @Schema(implementation = AlertRuleResponse.class)))
    })
    @GetMapping("/rules")
    public ResponseEntity<List<AlertRuleResponse>> listRules() {
        log.info("Petición GET /api/alerts/rules");
        List<AlertRuleResponse> response = alertRuleService.listRules();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Actualizar una regla de evaluación",
            description = "Solo accesible para ADMIN. Modifica enabled, severity y params de la regla. "
                    + "El cambio se aplica en la siguiente ronda de evaluación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Regla actualizada",
                    content = @Content(schema = @Schema(implementation = AlertRuleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Regla no encontrada (RULE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/rules/{type}")
    public ResponseEntity<AlertRuleResponse> updateRule(
            @Parameter(description = "Tipo de regla", example = "MAINTENANCE_DUE")
            @PathVariable AlertType type,

            @Valid @RequestBody UpdateAlertRuleRequest request
    ) {
        log.info("Petición PUT /api/alerts/rules/{} — enabled={}, severity={}, params={}",
                type, request.enabled(), request.severity(), request.params());
        AlertRuleResponse response = alertRuleService.updateRule(type, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Lanzar una evaluación inmediata",
            description = "Solo accesible para ADMIN. Ejecuta una ronda del evaluador sin esperar al scheduler."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluación ejecutada",
                    content = @Content(schema = @Schema(implementation = EvaluationSummaryResponse.class))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationSummaryResponse> evaluate() {
        log.info("Petición POST /api/alerts/evaluate — evaluación manual solicitada");
        EvaluationSummaryResponse response = alertEvaluationService.runEvaluation();
        return ResponseEntity.ok(response);
    }

    private static UUID parseUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        try {
            return UUID.fromString(authentication.getPrincipal().toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}