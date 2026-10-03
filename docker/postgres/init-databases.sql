-- Bases de datos de FleetControl (una por microservicio con persistencia).
-- ms-dashboard no tiene base de datos: solo cache Caffeine en memoria.

CREATE DATABASE auth_db;
CREATE DATABASE vehicles_db;
CREATE DATABASE drivers_db;
CREATE DATABASE routes_db;
CREATE DATABASE maintenance_db;
CREATE DATABASE fuel_db;
CREATE DATABASE alerts_db;