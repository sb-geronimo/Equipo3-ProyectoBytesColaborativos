-- Esquema inicial de ms-maintenance: modelos MaintenancePlan y MaintenanceOrder de CONTEXT.md
CREATE TABLE maintenance_plans (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    interval_km INTEGER,
    interval_days INTEGER,
    last_done_at DATE NOT NULL,
    last_done_km INTEGER NOT NULL,
    next_due_at DATE,
    next_due_km INTEGER,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE maintenance_orders (
    id UUID PRIMARY KEY,
    plan_id UUID,
    vehicle_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    scheduled_for DATE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    odometer_km INTEGER,
    cost NUMERIC(10, 2),
    workshop VARCHAR(255),
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Regla: un vehiculo solo puede tener un plan activo por tipo de mantenimiento
CREATE UNIQUE INDEX uq_maintenance_plans_active_type
    ON maintenance_plans (vehicle_id, type)
    WHERE active;
