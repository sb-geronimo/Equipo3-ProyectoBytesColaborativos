-- Esquema inicial de ms-alerts: modelos Alert y AlertRule de CONTEXT.md
CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    severity VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    vehicle_id UUID,
    driver_id UUID,
    message VARCHAR(1000) NOT NULL,
    dedup_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by UUID,
    comment VARCHAR(1000),
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE alert_rules (
    type VARCHAR(255) PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    severity VARCHAR(255) NOT NULL,
    params JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
