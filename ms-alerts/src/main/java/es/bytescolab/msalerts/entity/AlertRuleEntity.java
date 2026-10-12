package es.bytescolab.msalerts.entity;

import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

@Entity
@Table(name = "alert_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 255)
    private AlertType type;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 255)
    private AlertSeverity severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", nullable = false, columnDefinition = "jsonb")
    private JsonNode params;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}