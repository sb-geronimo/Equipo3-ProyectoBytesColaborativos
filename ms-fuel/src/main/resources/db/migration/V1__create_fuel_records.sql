-- Esquema inicial de ms-fuel: modelo FuelRecord de CONTEXT.md
CREATE TABLE fuel_records (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    refueled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    fuel_type VARCHAR(255) NOT NULL,
    liters NUMERIC(10, 2) NOT NULL,
    price_per_liter NUMERIC(10, 2) NOT NULL,
    total_cost NUMERIC(10, 2) NOT NULL,
    odometer_km INTEGER NOT NULL,
    full_tank BOOLEAN NOT NULL,
    station VARCHAR(255),
    consumption_l100_km NUMERIC(10, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
