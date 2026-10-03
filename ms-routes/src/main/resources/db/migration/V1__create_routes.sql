-- Esquema inicial de ms-routes: modelo Route de CONTEXT.md
CREATE TABLE routes (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    origin VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    planned_start TIMESTAMP WITH TIME ZONE NOT NULL,
    estimated_duration_min INTEGER NOT NULL,
    planned_distance_km NUMERIC(10, 2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    start_odometer_km INTEGER,
    end_odometer_km INTEGER,
    actual_distance_km NUMERIC(10, 2),
    notes VARCHAR(1000),
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
