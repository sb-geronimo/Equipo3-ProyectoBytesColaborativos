-- Esquema inicial de ms-vehicles: modelo Vehicle de CONTEXT.md
CREATE TABLE vehicles (
    id UUID PRIMARY KEY,
    plate VARCHAR(255) NOT NULL UNIQUE,
    make VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    year INTEGER NOT NULL,
    type VARCHAR(255) NOT NULL,
    fuel_type VARCHAR(255) NOT NULL,
    tank_capacity_l INTEGER NOT NULL,
    odometer_km INTEGER NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
