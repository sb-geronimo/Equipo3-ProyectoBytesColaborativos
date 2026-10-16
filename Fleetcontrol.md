# FleetControl

## FleetControl — Plataforma de Gestión de Flotas

### Información general

FleetControl es un backend de gestión de flotas que se construye en un sprint de 2 semanas y no depende de ninguna API externa: los datos los generan y almacenan los propios servicios.

| Campo               | Detalle                                                          |
| ------------------- | ---------------------------------------------------------------- |
| Nombre del proyecto | FleetControl — Plataforma de Gestión de Flotas                   |
| Tipo                | Backend con arquitectura de microservicios autónomos             |
| API externa         | Ninguna. Los datos de demostración los genera un seeder propio   |
| Duración            | 2 semanas (1 sprint)                                             |
| Metodología         | Scrum                                                            |
| Modalidad           | Cada equipo construye el sistema completo de forma independiente |

### Descripción del sistema

FleetControl es una plataforma de gestión de flotas compuesta por 9 microservicios que se comunican por HTTP y cubren el ciclo completo de un vehículo: alta, asignación a rutas, repostajes, mantenimiento, alertas y análisis.

Cada equipo debe diseñar, implementar, documentar y desplegar el sistema completo en su propio repositorio.

### Microservicios del sistema

| Microservicio  | Puerto | Responsabilidad                                         | Consume a                                                  |
| -------------- | ------ | ------------------------------------------------------- | ---------------------------------------------------------- |
| ms-gateway     | 8080   | Punto de entrada, enrutamiento y rate limiting          | Todos los servicios                                        |
| ms-auth        | 8081   | Registro, login y emisión de tokens JWT                 | —                                                          |
| ms-vehicles    | 8082   | Inventario de vehículos, estado y odómetro              | —                                                          |
| ms-drivers     | 8083   | Conductores, licencias y estado                         | —                                                          |
| ms-routes      | 8085   | Planificación, ejecución e historial de rutas           | ms-vehicles, ms-drivers                                    |
| ms-maintenance | 8086   | Planes de mantenimiento programado y órdenes de trabajo | ms-vehicles                                                |
| ms-fuel        | 8087   | Repostajes y consumo de combustible                     | ms-vehicles                                                |
| ms-alerts      | 8088   | Reglas y alertas de revisión, licencia y consumo        | ms-vehicles, ms-drivers, ms-maintenance, ms-fuel           |
| ms-dashboard   | 8089   | Resumen ejecutivo y series temporales agregadas         | ms-vehicles, ms-routes, ms-fuel, ms-maintenance, ms-alerts |

Todos los servicios de dominio validan el JWT emitido por ms-auth. Las llamadas entre servicios usan una clave interna compartida, descrita en la sección de comunicación.

### Tecnologías y herramientas

El stack es Java 17 con Spring Boot 3.x y PostgreSQL, y se añaden tres piezas pensadas para un sistema sin API externa: migraciones con Flyway, caché local con Caffeine y generación de datos con Datafaker.

| Categoría             | Tecnología                                                                                        |
| --------------------- | ------------------------------------------------------------------------------------------------- |
| Lenguaje              | Java 17                                                                                           |
| Framework             | Spring Boot 3.x                                                                                   |
| Gateway               | Spring Cloud Gateway                                                                              |
| Seguridad             | Spring Security + JWT (HS256)                                                                     |
| Base de datos         | PostgreSQL 15 (una base de datos por microservicio, todas en una instancia durante el desarrollo) |
| Migraciones           | Flyway                                                                                            |
| Comunicación          | OpenFeign                                                                                         |
| Tareas programadas    | Spring @Scheduled                                                                                 |
| Caché                 | Caffeine (en memoria)                                                                             |
| Resiliencia           | Resilience4j (timeout y circuit breaker en llamadas entre servicios)                              |
| Datos de demostración | Datafaker con semilla fija                                                                        |
| Documentación         | Swagger UI / OpenAPI 3.0                                                                          |
| Testing               | JUnit 5 + Mockito + Testcontainers + Postman                                                      |
| Mappers               | MapStruct                                                                                         |
| Utilidades            | Lombok                                                                                            |
| Contenerización       | Docker + Docker Compose                                                                           |
| Build                 | Maven o Gradle                                                                                    |

### Generación de datos y series temporales

Cada servicio de dominio genera su propio histórico de 90 días al arrancar con el perfil demo, de modo que el dashboard muestra series temporales desde la primera ejecución.

#### Reglas del seeder

- Se activa con `SPRING_PROFILES_ACTIVE=demo` y se controla con `DEMO_SEED` (por defecto 42) y `DEMO_DAYS` (por defecto 90).
- Es idempotente: solo inserta datos si las tablas del servicio están vacías.
- Las fechas son relativas al arranque (los últimos `DEMO_DAYS` días), por lo que los datos siempre parecen recientes.
- Los identificadores son UUID deterministas derivados de la semilla, por ejemplo `UUID.nameUUIDFromBytes(...)` sobre el texto `vehicle-7`. Así ms-routes, ms-fuel y ms-maintenance referencian los mismos vehículos y conductores sin llamarse durante el arranque.
- ms-auth crea el usuario `admin@fleetcontrol.com` con rol `ADMIN`. Su contraseña se lee de la variable `DEMO_ADMIN_PASSWORD`.

#### Volumen generado

| Servicio       | Datos generados                                                                                                    | Volumen aproximado         |
| -------------- | ------------------------------------------------------------------------------------------------------------------ | -------------------------- |
| ms-vehicles    | Vehículos de los cuatro tipos y de varios estados                                                                  | 25                         |
| ms-drivers     | Conductores; 3 con licencia próxima a caducar y 1 caducada                                                         | 20                         |
| ms-routes      | Rutas completadas en días laborables y 3 rutas planificadas                                                        | ≈ 1.300                    |
| ms-fuel        | Repostajes con consumo coherente con el tipo de vehículo                                                           | ≈ 300                      |
| ms-maintenance | Planes por vehículo y órdenes completadas; 5 órdenes pendientes con vencimiento en los próximos 7 días y 1 vencida | ≈ 50 planes y ≈ 40 órdenes |
| ms-alerts      | No genera datos. El evaluador crea las alertas a partir de los demás servicios                                     | —                          |

#### Realismo de los datos

| Tipo de vehículo | Consumo base (L/100 km) |
| ---------------- | ----------------------- |
| CAR              | 6,5                     |
| VAN              | 10,5                    |
| TRUCK            | 27                      |
| MOTORCYCLE       | 4,2                     |

Cada repostaje varía un ±8 % sobre el consumo base y el precio por litro oscila entre 1,45 y 1,75 EUR con tendencia semanal.

Dos vehículos tienen en los últimos 30 días un consumo un 30 % superior a su media anterior, para que ms-alerts genere alertas de consumo alto desde el primer arranque.

#### Series temporales

Los servicios que poseen datos con fecha exponen `GET /stats` con los parámetros `from`, `to` y `granularity` (`DAY`, `WEEK` o `MONTH`). ms-dashboard combina esas series. Las reglas son comunes a todos:

- `period` es la fecha de inicio del periodo: el propio día en `DAY`, el lunes en `WEEK` y el día 1 en `MONTH`.
- Los periodos sin datos se devuelven con valor 0, para que la serie no tenga huecos.
- Todas las fechas se calculan en UTC.

| Métrica            | Servicio de origen | Unidad   |
| ------------------ | ------------------ | -------- |
| DISTANCE_KM        | ms-routes          | km       |
| FUEL_LITERS        | ms-fuel            | L        |
| FUEL_COST          | ms-fuel            | EUR      |
| CONSUMPTION_L100KM | ms-fuel            | L/100 km |
| MAINTENANCE_COST   | ms-maintenance     | EUR      |
| ALERTS_CREATED     | ms-alerts          | alertas  |

## ms-gateway (Puerto 8080)

### Descripción

Punto de entrada único al sistema. Redirige cada petición al microservicio correcto y aplica rate limiting global. Además genera la cabecera `X-Request-Id` cuando la petición no la trae y elimina `X-Internal-Key` de todas las peticiones externas, para que ningún cliente pueda hacerse pasar por un servicio interno.

### Tabla de enrutamiento

| Prefijo de ruta       | Destino             |
| --------------------- | ------------------- |
| `/api/auth/**`        | ms-auth:8081        |
| `/api/vehicles/**`    | ms-vehicles:8082    |
| `/api/drivers/**`     | ms-drivers:8083     |
| `/api/routes/**`      | ms-routes:8085      |
| `/api/maintenance/**` | ms-maintenance:8086 |
| `/api/fuel/**`        | ms-fuel:8087        |
| `/api/alerts/**`      | ms-alerts:8088      |
| `/api/dashboard/**`   | ms-dashboard:8089   |

### Endpoints requeridos

#### 1. ANY /api/**

- **Acceso:** Público (reenvía al servicio correspondiente, que valida el JWT)
- **Descripción:** Proxy inverso. Redirige la petición y devuelve la respuesta del servicio destino sin modificarla. Si el destino no responde en 5 segundos, devuelve `503 SERVICE_UNAVAILABLE`.

#### 2. GET /health

- **Acceso:** Público
- **Descripción:** Estado del gateway y de los servicios downstream. Consulta el `/actuator/health` de cada servicio con un timeout de 2 segundos y marca como `DOWN` los que no responden. Siempre devuelve 200.

**Response 200 OK:**

```json
{
  "gateway": "UP",
  "timestamp": "2026-10-05T10:30:00Z",
  "services": {
    "ms-auth": "UP",
    "ms-vehicles": "UP",
    "ms-drivers": "UP",
    "ms-routes": "UP",
    "ms-maintenance": "UP",
    "ms-fuel": "UP",
    "ms-alerts": "UP",
    "ms-dashboard": "DOWN"
  }
}
```

### Rate Limiting

Límite: 60 requests/minuto por IP. Respuesta cuando se excede (429 Too Many Requests, con la cabecera `Retry-After`):

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Demasiadas peticiones. Límite: 60 req/min",
  "retryAfter": 30,
  "timestamp": "2026-10-05T10:30:00Z"
}
```

## ms-auth (Puerto 8081)

### Descripción

Gestiona el registro y la autenticación de usuarios. Emite tokens JWT (HS256, validez de 1 hora) con los claims `sub` (id del usuario), `username` y `role`. Los demás servicios validan el token localmente con la variable `JWT_SECRET`; `POST /api/auth/validate` queda disponible para comprobaciones remotas.

### Roles

| Rol     | Permisos                                                                                                                                              |
| ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| MANAGER | Gestiona vehículos, conductores, rutas, repostajes y mantenimientos, consulta el dashboard y reconoce alertas. Es el rol que se asigna al registrarse |
| ADMIN   | Todo lo de MANAGER, además de modificar las reglas de alertas y lanzar evaluaciones manuales. Lo crea el seeder del perfil demo                       |

### Endpoints requeridos

#### 1. POST /api/auth/register

- **Acceso:** Público
- **Descripción:** Registra un nuevo usuario con el rol `MANAGER`.

**Body:**

```json
{
  "username": "carlos_ruiz",
  "email": "carlos@fleetcontrol.com",
  "password": "FleetPass123!"
}
```

**Response 201 Created:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "carlos_ruiz",
  "email": "carlos@fleetcontrol.com",
  "role": "MANAGER",
  "createdAt": "2026-10-05T10:30:00Z"
}
```

**Validaciones:**

- Email único y con formato válido
- Password de mínimo 8 caracteres, con una mayúscula y un número
- Username único y sin espacios

**Response 409 Conflict:**

```json
{
  "error": "USER_ALREADY_EXISTS",
  "message": "Ya existe un usuario con ese email",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

#### 2. POST /api/auth/login

- **Acceso:** Público
- **Descripción:** Autentica al usuario y devuelve un token JWT.

**Body:**

```json
{
  "email": "carlos@fleetcontrol.com",
  "password": "FleetPass123!"
}
```

**Response 200 OK:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "MANAGER"
}
```

**Response 401 Unauthorized:**

```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Email o contraseña incorrectos",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

#### 3. POST /api/auth/validate

- **Acceso:** Interno (consumido por otros servicios o por herramientas de diagnóstico)
- **Descripción:** Valida un token JWT y devuelve la información del usuario.

**Headers:**

```
Authorization: Bearer <token>
```

**Response 200 OK:**

```json
{
  "valid": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "carlos_ruiz",
  "role": "MANAGER"
}
```

**Response 401 Unauthorized:**

```json
{
  "valid": false,
  "error": "TOKEN_EXPIRED",
  "message": "El token ha expirado"
}
```

### Modelo de datos — ms-auth

#### User

| Campo     | Tipo    | Descripción                  |
| --------- | ------- | ---------------------------- |
| id        | UUID    | Identificador único          |
| username  | String  | Nombre de usuario (único)    |
| email     | String  | Correo electrónico (único)   |
| password  | String  | Hash bcrypt de la contraseña |
| role      | Enum    | Rol del usuario              |
| createdAt | Instant | Fecha de registro            |
| updatedAt | Instant | Fecha de actualización       |

#### Enums — ms-auth

`UserRole`: `MANAGER`, `ADMIN`

## ms-vehicles (Puerto 8082)

### Descripción

Mantiene el inventario de vehículos de la flota con su estado operativo y su odómetro. Es la fuente de verdad que consultan ms-routes, ms-maintenance, ms-fuel, ms-alerts y ms-dashboard, y también recibe de ellos los cambios de estado.

### Endpoints requeridos

#### 1. GET /api/vehicles

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Lista paginada de vehículos, filtrable por estado, tipo o placa.

**Query params:**

- `status` (enum, opcional) — ej: `AVAILABLE`
- `type` (enum, opcional) — ej: `VAN`
- `plate` (string, opcional) — búsqueda parcial
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100)

**Response 200 OK:**

```json
{
  "content": [
    {
      "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "make": "Ford",
      "model": "Transit",
      "year": 2022,
      "type": "VAN",
      "fuelType": "DIESEL",
      "tankCapacityL": 80,
      "odometerKm": 45210,
      "status": "AVAILABLE"
    },
    {
      "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a02",
      "plate": "7305-LMN",
      "make": "Renault",
      "model": "Clio",
      "year": 2021,
      "type": "CAR",
      "fuelType": "GASOLINE",
      "tankCapacityL": 45,
      "odometerKm": 61880,
      "status": "IN_USE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 25,
  "totalPages": 2
}
```

#### 2. GET /api/vehicles/summary

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Recuento de vehículos por estado y por tipo. Lo consume ms-dashboard.

**Response 200 OK:**

```json
{
  "total": 25,
  "byStatus": {
    "AVAILABLE": 14,
    "IN_USE": 8,
    "IN_MAINTENANCE": 2,
    "OUT_OF_SERVICE": 1
  },
  "byType": {
    "CAR": 9,
    "VAN": 10,
    "TRUCK": 4,
    "MOTORCYCLE": 2
  }
}
```

#### 3. GET /api/vehicles/{vehicleId}

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Detalle de un vehículo por su ID.

**Response 200 OK:**

```json
{
  "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "plate": "4821-KDF",
  "make": "Ford",
  "model": "Transit",
  "year": 2022,
  "type": "VAN",
  "fuelType": "DIESEL",
  "tankCapacityL": 80,
  "odometerKm": 45210,
  "status": "AVAILABLE",
  "createdAt": "2026-07-01T08:00:00Z",
  "updatedAt": "2026-10-05T07:45:00Z"
}
```

**Response 404 Not Found:**

```json
{
  "error": "VEHICLE_NOT_FOUND",
  "message": "No existe un vehículo con el ID proporcionado",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

#### 4. POST /api/vehicles

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Da de alta un vehículo. Siempre nace en estado `AVAILABLE`.

**Body:**

```json
{
  "plate": "9012-PQR",
  "make": "Mercedes-Benz",
  "model": "Actros",
  "year": 2023,
  "type": "TRUCK",
  "fuelType": "DIESEL",
  "tankCapacityL": 400,
  "odometerKm": 12000
}
```

**Response 201 Created:** el vehículo creado, con la misma estructura que el detalle.

**Validaciones:**

- Placa obligatoria y única
- Año entre 1990 y el año siguiente al actual
- `tankCapacityL` mayor que 0 y `odometerKm` mayor o igual que 0

**Errores:** 409 `VEHICLE_ALREADY_EXISTS` si la placa ya existe; 400 `VALIDATION_ERROR` si algún campo no cumple.

#### 5. PUT /api/vehicles/{vehicleId}

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Actualiza los datos descriptivos del vehículo: `make`, `model`, `year`, `type`, `fuelType` y `tankCapacityL`. No modifica la placa, el estado ni el odómetro.

**Response 200 OK:** el vehículo actualizado.

**Errores:** 404 `VEHICLE_NOT_FOUND`, 400 `VALIDATION_ERROR`.

#### 6. PATCH /api/vehicles/{vehicleId}/status

- **Acceso:** MANAGER, ADMIN y llamadas internas (ms-routes y ms-maintenance)
- **Descripción:** Cambia el estado del vehículo y, de forma opcional, actualiza su odómetro. El odómetro solo puede aumentar.

**Body:**

```json
{
  "status": "AVAILABLE",
  "odometerKm": 45310
}
```

**Response 200 OK:** el vehículo actualizado.

**Transiciones permitidas:**

- `AVAILABLE` → `IN_USE` e `IN_USE` → `AVAILABLE`
- `AVAILABLE` → `IN_MAINTENANCE` e `IN_MAINTENANCE` → `AVAILABLE`
- Cualquier estado → `OUT_OF_SERVICE`
- `OUT_OF_SERVICE` → `AVAILABLE`, solo para ADMIN

**Errores:** 409 `INVALID_STATUS_TRANSITION` si la transición no está permitida (por ejemplo `IN_USE` → `IN_MAINTENANCE`); 409 `INVALID_ODOMETER` si el valor es menor que el actual; 403 `FORBIDDEN` si un MANAGER intenta reactivar un vehículo fuera de servicio.

### Modelo de datos — ms-vehicles

#### Vehicle

| Campo         | Tipo    | Descripción                      |
| ------------- | ------- | -------------------------------- |
| id            | UUID    | Identificador único              |
| plate         | String  | Placa o matrícula (única)        |
| make          | String  | Marca                            |
| model         | String  | Modelo                           |
| year          | Integer | Año de fabricación               |
| type          | Enum    | Tipo de vehículo                 |
| fuelType      | Enum    | Tipo de combustible              |
| tankCapacityL | Integer | Capacidad del depósito en litros |
| odometerKm    | Integer | Kilometraje acumulado            |
| status        | Enum    | Estado operativo                 |
| createdAt     | Instant | Fecha de alta                    |
| updatedAt     | Instant | Fecha de actualización           |

#### Enums — ms-vehicles

- `VehicleType`: `CAR`, `VAN`, `TRUCK`, `MOTORCYCLE`
- `FuelType`: `DIESEL`, `GASOLINE`, `LPG`, `HYBRID`
- `VehicleStatus`: `AVAILABLE`, `IN_USE`, `IN_MAINTENANCE`, `OUT_OF_SERVICE`

## ms-drivers (Puerto 8083)

### Descripción

Gestiona los conductores de la flota, su licencia de conducir y su estado. ms-routes lo consulta para comprobar que un conductor puede asignarse a una ruta, y ms-alerts para detectar licencias próximas a caducar.

### Endpoints requeridos

#### 1. GET /api/drivers

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Lista paginada de conductores, filtrable por estado o por caducidad de la licencia.

**Query params:**

- `status` (enum, opcional) — ej: `ACTIVE`
- `licenseExpiringInDays` (int, opcional) — devuelve los conductores cuya licencia caduca en ese número de días o menos, incluidas las ya caducadas
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100)

**Response 200 OK:**

```json
{
  "content": [
    {
      "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
      "fullName": "Laura Fernández",
      "email": "laura.fernandez@fleetcontrol.com",
      "phone": "+34 600 123 456",
      "licenseNumber": "B-4471923",
      "licenseCategory": "B",
      "licenseExpiresAt": "2027-03-14",
      "status": "ACTIVE"
    },
    {
      "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a02",
      "fullName": "Miguel Torres",
      "email": "miguel.torres@fleetcontrol.com",
      "phone": "+34 611 987 654",
      "licenseNumber": "C-2038841",
      "licenseCategory": "C",
      "licenseExpiresAt": "2026-10-20",
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 20,
  "totalPages": 1
}
```

#### 2. GET /api/drivers/{driverId}

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Detalle de un conductor por su ID. Devuelve los mismos campos que el listado más `createdAt` y `updatedAt`.

**Response 404 Not Found:**

```json
{
  "error": "DRIVER_NOT_FOUND",
  "message": "No existe un conductor con el ID proporcionado",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

#### 3. POST /api/drivers

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Da de alta un conductor. Siempre nace en estado `ACTIVE`.

**Body:**

```json
{
  "fullName": "Sara Ibáñez",
  "email": "sara.ibanez@fleetcontrol.com",
  "phone": "+34 622 456 789",
  "licenseNumber": "B-5120377",
  "licenseCategory": "B",
  "licenseExpiresAt": "2029-06-30"
}
```

**Response 201 Created:** el conductor creado, con `id`, `status`, `createdAt` y `updatedAt`.

**Validaciones:**

- Número de licencia obligatorio y único
- Email con formato válido
- `licenseExpiresAt` posterior a la fecha actual

**Errores:** 409 `DRIVER_ALREADY_EXISTS` si el número de licencia ya existe; 400 `VALIDATION_ERROR` si algún campo no cumple.

#### 4. PUT /api/drivers/{driverId}

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Actualiza todos los datos del conductor, incluido su estado (`ACTIVE`, `ON_LEAVE`, `SUSPENDED`). Permite renovar la licencia con una nueva fecha de caducidad.

**Response 200 OK:** el conductor actualizado.

**Errores:** 404 `DRIVER_NOT_FOUND`, 409 `DRIVER_ALREADY_EXISTS`, 400 `VALIDATION_ERROR`.

### Modelo de datos — ms-drivers

#### Driver

| Campo            | Tipo      | Descripción                       |
| ---------------- | --------- | --------------------------------- |
| id               | UUID      | Identificador único               |
| fullName         | String    | Nombre completo                   |
| email            | String    | Correo electrónico                |
| phone            | String    | Teléfono de contacto              |
| licenseNumber    | String    | Número de licencia (único)        |
| licenseCategory  | Enum      | Categoría de la licencia          |
| licenseExpiresAt | LocalDate | Fecha de caducidad de la licencia |
| status           | Enum      | Estado del conductor              |
| createdAt        | Instant   | Fecha de alta                     |
| updatedAt        | Instant   | Fecha de actualización            |

#### Enums — ms-drivers

- `LicenseCategory`: `A`, `B`, `C`
- `DriverStatus`: `ACTIVE`, `ON_LEAVE`, `SUSPENDED`

## ms-routes (Puerto 8085)

### Descripción

Gestiona las rutas de la flota: planificación, ejecución e historial. Consume ms-vehicles y ms-drivers para validar la asignación y para actualizar el estado y el odómetro del vehículo cuando una ruta empieza o termina. Es el origen de la métrica de kilómetros recorridos.

### Reglas de negocio

- El vehículo debe existir y no estar `OUT_OF_SERVICE`.
- El conductor debe existir, estar `ACTIVE` y tener la licencia vigente en la fecha de inicio de la ruta.
- La categoría de la licencia debe ser compatible con el tipo de vehículo: `A` habilita `MOTORCYCLE`, `B` habilita `CAR` y `VAN`, y `C` habilita `CAR`, `VAN` y `TRUCK`.
- No puede haber dos rutas `PLANNED` o `IN_PROGRESS` que se solapen en el tiempo para el mismo vehículo o para el mismo conductor. El intervalo de una ruta va de `plannedStart` a `plannedStart` más `estimatedDurationMin`.

### Endpoints requeridos

#### 1. POST /api/routes

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Planifica una ruta asignándole un vehículo y un conductor. Valida las reglas de negocio llamando a ms-vehicles y ms-drivers.

**Body:**

```json
{
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
  "origin": "Madrid - Centro Logístico",
  "destination": "Valencia - Puerto",
  "plannedStart": "2026-10-06T07:00:00Z",
  "estimatedDurationMin": 240,
  "plannedDistanceKm": 355.0
}
```

**Response 201 Created:** la ruta con `status` igual a `PLANNED` y los campos del modelo que ya tienen valor.

**Errores:** 404 `VEHICLE_NOT_FOUND` o 404 `DRIVER_NOT_FOUND` si el recurso no existe; 409 `VEHICLE_NOT_AVAILABLE` si el vehículo está fuera de servicio; 409 `DRIVER_NOT_ELIGIBLE` si el conductor no está activo, tiene la licencia caducada o su categoría no es compatible; 409 `ROUTE_OVERLAP` si hay solapamiento; 503 `SERVICE_UNAVAILABLE` si ms-vehicles o ms-drivers no responden.

#### 2. GET /api/routes

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Lista paginada de rutas, ordenada por `plannedStart` descendente.

**Query params:**

- `vehicle` (UUID, opcional) y `driver` (UUID, opcional)
- `status` (enum, opcional) — ej: `COMPLETED`
- `from` y `to` (fecha `YYYY-MM-DD`, opcionales) — filtran por `plannedStart`
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100)

**Response 200 OK:** página con la misma estructura que el listado de ms-vehicles (`content`, `page`, `size`, `totalElements`, `totalPages`), donde cada elemento es una ruta.

#### 3. GET /api/routes/{routeId}

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Detalle de una ruta por su ID.

**Errores:** 404 `ROUTE_NOT_FOUND`.

#### 4. POST /api/routes/{routeId}/start

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Inicia una ruta planificada. No lleva body. El orden de las operaciones es:
  1. Comprobar que la ruta está en `PLANNED`.
  2. Consultar el vehículo en ms-vehicles y comprobar que está `AVAILABLE`.
  3. Cambiar el vehículo a `IN_USE` en ms-vehicles.
  4. Guardar la ruta como `IN_PROGRESS`, con `startedAt` igual a la hora actual y `startOdometerKm` igual al odómetro del vehículo.

Si falla el paso 4, se devuelve el vehículo a `AVAILABLE` como compensación.

**Response 200 OK:** la ruta con `status` igual a `IN_PROGRESS`.

**Errores:** 409 `INVALID_ROUTE_STATE` si la ruta no está planificada; 409 `VEHICLE_NOT_AVAILABLE` si el vehículo está en uso o en mantenimiento; 503 `SERVICE_UNAVAILABLE`.

#### 5. POST /api/routes/{routeId}/complete

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Finaliza una ruta en curso. Calcula `endOdometerKm` sumando la distancia real, redondeada, a `startOdometerKm`, y deja el vehículo en `AVAILABLE` con el odómetro actualizado. Si ms-vehicles no responde, la ruta permanece `IN_PROGRESS` y la operación puede reintentarse.

**Body:**

```json
{
  "actualDistanceKm": 362.4,
  "notes": "Sin incidencias"
}
```

**Response 200 OK:**

```json
{
  "id": "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
  "origin": "Madrid - Centro Logístico",
  "destination": "Valencia - Puerto",
  "plannedStart": "2026-10-06T07:00:00Z",
  "estimatedDurationMin": 240,
  "plannedDistanceKm": 355.0,
  "status": "COMPLETED",
  "startedAt": "2026-10-06T07:04:12Z",
  "endedAt": "2026-10-06T11:20:45Z",
  "startOdometerKm": 45210,
  "endOdometerKm": 45572,
  "actualDistanceKm": 362.4,
  "notes": "Sin incidencias"
}
```

**Errores:** 409 `INVALID_ROUTE_STATE` si la ruta no está en curso; 400 `VALIDATION_ERROR` si `actualDistanceKm` no es mayor que 0.

#### 6. GET /api/routes/stats

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Serie temporal de rutas completadas, agrupadas por la fecha de finalización. Lo consume ms-dashboard.

**Query params:**

- `from` y `to` (fecha `YYYY-MM-DD`, requeridos)
- `granularity` (enum, opcional, por defecto `DAY`) — `DAY`, `WEEK` o `MONTH`
- `vehicle` (UUID, opcional)

**Response 200 OK:**

```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "totals": { "routes": 302, "distanceKm": 28084.9 },
  "series": [
    {
      "period": "2026-09-07",
      "routes": 97,
      "distanceKm": 9034.1,
      "avgDurationMin": 128
    },
    {
      "period": "2026-09-14",
      "routes": 104,
      "distanceKm": 9710.6,
      "avgDurationMin": 131
    },
    {
      "period": "2026-09-21",
      "routes": 101,
      "distanceKm": 9340.2,
      "avgDurationMin": 126
    }
  ]
}
```

### Modelo de datos — ms-routes

#### Route

| Campo                | Tipo       | Descripción                                  |
| -------------------- | ---------- | -------------------------------------------- |
| id                   | UUID       | Identificador único                          |
| vehicleId            | UUID       | Vehículo asignado (referencia a ms-vehicles) |
| driverId             | UUID       | Conductor asignado (referencia a ms-drivers) |
| origin               | String     | Punto de origen                              |
| destination          | String     | Punto de destino                             |
| plannedStart         | Instant    | Inicio planificado                           |
| estimatedDurationMin | Integer    | Duración estimada en minutos                 |
| plannedDistanceKm    | BigDecimal | Distancia planificada                        |
| status               | Enum       | Estado de la ruta                            |
| startedAt            | Instant    | Inicio real                                  |
| endedAt              | Instant    | Fin real                                     |
| startOdometerKm      | Integer    | Odómetro al empezar                          |
| endOdometerKm        | Integer    | Odómetro al terminar                         |
| actualDistanceKm     | BigDecimal | Distancia recorrida                          |
| notes                | String     | Observaciones                                |
| createdBy            | UUID       | Usuario que planificó la ruta                |
| createdAt            | Instant    | Fecha de creación                            |
| updatedAt            | Instant    | Fecha de actualización                       |

#### Enums — ms-routes

`RouteStatus`: `PLANNED`, `IN_PROGRESS`, `COMPLETED`

## ms-maintenance (Puerto 8086)

### Descripción

Gestiona el mantenimiento programado: planes recurrentes por vehículo y las órdenes de trabajo que se derivan de ellos. Consume ms-vehicles para leer el odómetro y para marcar el vehículo como `IN_MAINTENANCE` mientras dura la intervención.

### Reglas de negocio

- Un plan define un intervalo por kilómetros (`intervalKm`), por días (`intervalDays`) o por ambos. Al menos uno es obligatorio y el plan vence cuando se cumple el primero.
- Un vehículo solo puede tener un plan activo por tipo de mantenimiento.
- Una tarea diaria (`@Scheduled`, 06:00 UTC) revisa los planes activos que no tienen una orden abierta y crea una orden `PENDING` si faltan 7 días o menos para `nextDueAt`, o si el odómetro del vehículo está a 500 km o menos de `nextDueKm`.
- Al completar una orden, el plan recalcula su próximo vencimiento a partir de la fecha y del odómetro de ese día.

### Endpoints requeridos

#### 1. POST /api/maintenance/plans

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Crea un plan de mantenimiento para un vehículo. Si no se indican `lastDoneAt` y `lastDoneKm`, se toman la fecha de hoy y el odómetro actual del vehículo.

**Body:**

```json
{
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "type": "OIL_CHANGE",
  "intervalKm": 15000,
  "intervalDays": 365,
  "lastDoneAt": "2026-03-10",
  "lastDoneKm": 32000
}
```

**Response 201 Created:**

```json
{
  "id": "9d4a6e21-0b3c-4a77-8f15-2c6e7b1d3a01",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "type": "OIL_CHANGE",
  "intervalKm": 15000,
  "intervalDays": 365,
  "lastDoneAt": "2026-03-10",
  "lastDoneKm": 32000,
  "nextDueAt": "2027-03-10",
  "nextDueKm": 47000,
  "active": true
}
```

**Errores:** 404 `VEHICLE_NOT_FOUND`; 409 `PLAN_ALREADY_EXISTS` si el vehículo ya tiene un plan activo de ese tipo; 400 `VALIDATION_ERROR` si no se indica ningún intervalo.

#### 2. GET /api/maintenance/plans

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Lista los planes de mantenimiento.

**Query params:**

- `vehicle` (UUID, opcional)
- `type` (enum, opcional)
- `active` (boolean, opcional)

**Response 200 OK:** lista de planes con la misma estructura que la respuesta anterior.

#### 3. GET /api/maintenance/orders

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Lista paginada de órdenes de trabajo, ordenada por `scheduledFor` ascendente.

**Query params:**

- `vehicle` (UUID, opcional)
- `status` (enum, opcional) — ej: `PENDING`
- `type` (enum, opcional)
- `dueBefore` (fecha `YYYY-MM-DD`, opcional) — órdenes con `scheduledFor` igual o anterior a esa fecha
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100)

**Response 200 OK:**

```json
{
  "content": [
    {
      "id": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
      "planId": "9d4a6e21-0b3c-4a77-8f15-2c6e7b1d3a02",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "type": "OIL_CHANGE",
      "status": "PENDING",
      "scheduledFor": "2026-10-08",
      "cost": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 6,
  "totalPages": 1
}
```

#### 4. POST /api/maintenance/orders/{orderId}/start

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Inicia una orden pendiente. No lleva body. Cambia la orden a `IN_PROGRESS` y el vehículo a `IN_MAINTENANCE` en ms-vehicles.

**Response 200 OK:** la orden con `status` igual a `IN_PROGRESS` y `startedAt` informado.

**Errores:** 404 `ORDER_NOT_FOUND`; 409 `INVALID_ORDER_STATE` si la orden no está pendiente; 409 `VEHICLE_NOT_AVAILABLE` si el vehículo está en uso; 503 `SERVICE_UNAVAILABLE`.

#### 5. POST /api/maintenance/orders/{orderId}/complete

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Completa una orden en curso. Registra el coste, devuelve el vehículo a `AVAILABLE` (actualizando su odómetro si el valor indicado es mayor) y recalcula el plan asociado. `odometerKm` es opcional y, si falta, se usa el odómetro actual del vehículo.

**Body:**

```json
{
  "cost": 185.5,
  "odometerKm": 45210,
  "workshop": "Taller Central Madrid",
  "notes": "Cambio de aceite y filtros"
}
```

**Response 200 OK:**

```json
{
  "id": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "type": "OIL_CHANGE",
  "status": "COMPLETED",
  "completedAt": "2026-10-08T09:40:00Z",
  "cost": 185.5,
  "odometerKm": 45210,
  "workshop": "Taller Central Madrid",
  "plan": {
    "id": "9d4a6e21-0b3c-4a77-8f15-2c6e7b1d3a02",
    "nextDueAt": "2027-10-08",
    "nextDueKm": 60210
  }
}
```

**Errores:** 409 `INVALID_ORDER_STATE` si la orden no está en curso; 400 `VALIDATION_ERROR` si el coste es negativo; 409 `INVALID_ODOMETER` si el odómetro es menor que el del vehículo.

#### 6. GET /api/maintenance/stats

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Serie temporal de órdenes completadas y su coste, con desglose por tipo y recuento de órdenes vencidas o próximas a vencer. Lo consume ms-dashboard.

**Query params:**

- `from` y `to` (fecha `YYYY-MM-DD`, requeridos)
- `granularity` (enum, opcional, por defecto `DAY`) — `DAY`, `WEEK` o `MONTH`
- `vehicle` (UUID, opcional)

**Response 200 OK:**

```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "totals": { "orders": 9, "cost": 1480.75 },
  "series": [
    { "period": "2026-09-07", "orders": 3, "cost": 420.0 },
    { "period": "2026-09-14", "orders": 4, "cost": 715.25 },
    { "period": "2026-09-21", "orders": 2, "cost": 345.5 }
  ],
  "byType": [
    { "type": "OIL_CHANGE", "orders": 4, "cost": 742.0 },
    { "type": "BRAKE_CHECK", "orders": 3, "cost": 530.75 },
    { "type": "TIRE_ROTATION", "orders": 2, "cost": 208.0 }
  ],
  "upcoming": { "overdue": 1, "dueIn7Days": 5 }
}
```

### Modelo de datos — ms-maintenance

#### MaintenancePlan

| Campo        | Tipo      | Descripción                         |
| ------------ | --------- | ----------------------------------- |
| id           | UUID      | Identificador único                 |
| vehicleId    | UUID      | Vehículo (referencia a ms-vehicles) |
| type         | Enum      | Tipo de mantenimiento               |
| intervalKm   | Integer   | Intervalo en kilómetros (opcional)  |
| intervalDays | Integer   | Intervalo en días (opcional)        |
| lastDoneAt   | LocalDate | Fecha de la última intervención     |
| lastDoneKm   | Integer   | Odómetro en la última intervención  |
| nextDueAt    | LocalDate | Fecha del próximo vencimiento       |
| nextDueKm    | Integer   | Odómetro del próximo vencimiento    |
| active       | Boolean   | Indica si el plan está vigente      |
| createdAt    | Instant   | Fecha de creación                   |

#### MaintenanceOrder

| Campo        | Tipo       | Descripción                         |
| ------------ | ---------- | ----------------------------------- |
| id           | UUID       | Identificador único                 |
| planId       | UUID       | Plan que la originó                 |
| vehicleId    | UUID       | Vehículo (referencia a ms-vehicles) |
| type         | Enum       | Tipo de mantenimiento               |
| status       | Enum       | Estado de la orden                  |
| scheduledFor | LocalDate  | Fecha prevista                      |
| startedAt    | Instant    | Inicio de la intervención           |
| completedAt  | Instant    | Fin de la intervención              |
| odometerKm   | Integer    | Odómetro al completar               |
| cost         | BigDecimal | Coste en EUR                        |
| workshop     | String     | Taller que realizó la intervención  |
| notes        | String     | Observaciones                       |
| createdAt    | Instant    | Fecha de creación                   |

#### Enums — ms-maintenance

- `MaintenanceType`: `OIL_CHANGE`, `TIRE_ROTATION`, `BRAKE_CHECK`, `GENERAL_INSPECTION`, `LEGAL_INSPECTION`
- `MaintenanceOrderStatus`: `PENDING`, `IN_PROGRESS`, `COMPLETED`

## ms-fuel (Puerto 8087)

### Descripción

Registra los repostajes de la flota y calcula el consumo de cada vehículo. Consume ms-vehicles para validar el vehículo, conocer su tipo de combustible y la capacidad de su depósito, y completar las respuestas con la placa. Es el origen de las métricas de litros, coste y consumo.

### Reglas de negocio

- `fuelType` se toma del vehículo; no se envía en el body.
- `liters` no puede superar la capacidad del depósito del vehículo.
- `odometerKm` no puede ser menor que el del último repostaje del vehículo.
- `totalCost` es `liters` por `pricePerLiter`, redondeado a 2 decimales.
- El consumo se calcula entre dos repostajes con depósito lleno consecutivos (`fullTank` igual a `true`), sumando los litros de los repostajes parciales intermedios:

$$
\frac{L_{\text{desde el lleno anterior}}}{\text{km}_{\text{entre llenos}}} \times 100
$$

Por ejemplo, con un lleno a 45.000 km, un repostaje parcial de 20 L y un nuevo lleno de 38 L a 45.500 km, el consumo es `(20 + 38) / 500 × 100 = 11,6 L/100 km`. Si no existe un lleno anterior, `consumptionL100km` es null.

### Endpoints requeridos

#### 1. POST /api/fuel/refuels

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Registra un repostaje.

**Body:**

```json
{
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "refueledAt": "2026-10-06T12:15:00Z",
  "liters": 58.4,
  "pricePerLiter": 1.62,
  "odometerKm": 45572,
  "fullTank": true,
  "station": "Repsol A-3 km 112"
}
```

**Response 201 Created:**

```json
{
  "id": "b7a3f9e2-4c18-4d5a-9e60-3f1d8c2a7b01",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "refueledAt": "2026-10-06T12:15:00Z",
  "fuelType": "DIESEL",
  "liters": 58.4,
  "pricePerLiter": 1.62,
  "totalCost": 94.61,
  "odometerKm": 45572,
  "fullTank": true,
  "station": "Repsol A-3 km 112",
  "consumptionL100km": 11.3
}
```

**Errores:** 404 `VEHICLE_NOT_FOUND`; 400 `VALIDATION_ERROR` si `liters` es 0 o negativo o supera la capacidad del depósito; 409 `INVALID_ODOMETER` si el odómetro es menor que el del repostaje anterior; 503 `SERVICE_UNAVAILABLE` si ms-vehicles no responde.

#### 2. GET /api/fuel/refuels

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Lista paginada de repostajes, ordenada por `refueledAt` descendente.

**Query params:**

- `vehicle` (UUID, opcional)
- `from` y `to` (fecha `YYYY-MM-DD`, opcionales)
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100)

**Response 200 OK:** página con la estructura habitual (`content`, `page`, `size`, `totalElements`, `totalPages`), donde cada elemento tiene los mismos campos que la respuesta de creación.

#### 3. GET /api/fuel/consumption

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Consumo agregado por vehículo en un periodo, ordenado por `cost` descendente. La distancia se calcula con la diferencia de odómetro entre el primer y el último repostaje del periodo. La placa y el tipo se obtienen de ms-vehicles. Lo consumen ms-dashboard y ms-alerts.

**Query params:**

- `from` y `to` (fecha `YYYY-MM-DD`, requeridos)

**Response 200 OK:**

```json
{
  "from": "2026-09-01",
  "to": "2026-09-30",
  "vehicles": [
    {
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a03",
      "plate": "9012-PQR",
      "type": "TRUCK",
      "liters": 1845.2,
      "cost": 2951.96,
      "distanceKm": 6720,
      "avgL100km": 27.5
    },
    {
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "type": "VAN",
      "liters": 690.3,
      "cost": 1118.29,
      "distanceKm": 6310,
      "avgL100km": 10.9
    }
  ]
}
```

#### 4. GET /api/fuel/stats

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Serie temporal de litros, coste y consumo medio de la flota o de un vehículo. Lo consume ms-dashboard.

**Query params:**

- `from` y `to` (fecha `YYYY-MM-DD`, requeridos)
- `granularity` (enum, opcional, por defecto `DAY`) — `DAY`, `WEEK` o `MONTH`
- `vehicle` (UUID, opcional)

**Response 200 OK:**

```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "totals": {
    "liters": 3090.0,
    "cost": 5005.8,
    "avgPricePerLiter": 1.62,
    "avgL100km": 11.0
  },
  "series": [
    {
      "period": "2026-09-07",
      "liters": 984.7,
      "cost": 1595.21,
      "avgPricePerLiter": 1.62,
      "avgL100km": 10.9
    },
    {
      "period": "2026-09-14",
      "liters": 1077.9,
      "cost": 1746.2,
      "avgPricePerLiter": 1.62,
      "avgL100km": 11.1
    },
    {
      "period": "2026-09-21",
      "liters": 1027.4,
      "cost": 1664.39,
      "avgPricePerLiter": 1.62,
      "avgL100km": 11.0
    }
  ]
}
```

### Modelo de datos — ms-fuel

#### FuelRecord

| Campo             | Tipo       | Descripción                                       |
| ----------------- | ---------- | ------------------------------------------------- |
| id                | UUID       | Identificador único                               |
| vehicleId         | UUID       | Vehículo (referencia a ms-vehicles)               |
| refueledAt        | Instant    | Fecha y hora del repostaje                        |
| fuelType          | Enum       | Tipo de combustible, tomado del vehículo          |
| liters            | BigDecimal | Litros repostados                                 |
| pricePerLiter     | BigDecimal | Precio por litro en EUR                           |
| totalCost         | BigDecimal | Coste total en EUR                                |
| odometerKm        | Integer    | Odómetro en el momento del repostaje              |
| fullTank          | Boolean    | Indica si se llenó el depósito                    |
| station           | String     | Estación de servicio                              |
| consumptionL100km | BigDecimal | Consumo calculado (nulo si no hay lleno anterior) |
| createdAt         | Instant    | Fecha de registro                                 |

#### Enums — ms-fuel

`FuelType`: `DIESEL`, `GASOLINE`, `LPG`, `HYBRID` // mismos valores que en ms-vehicles

## ms-alerts (Puerto 8088)

### Descripción

Detecta situaciones que requieren atención y las convierte en alertas: mantenimientos próximos o vencidos, licencias de conducir próximas a caducar o caducadas y consumo de combustible anormal. Un evaluador programado consulta ms-maintenance, ms-drivers y ms-fuel, y usa ms-vehicles para completar los mensajes con la placa.

### Reglas de evaluación

| Tipo de alerta      | Condición                                                                                          | Severidad por defecto | Parámetros           |
| ------------------- | -------------------------------------------------------------------------------------------------- | --------------------- | -------------------- |
| MAINTENANCE_DUE     | Orden `PENDING` con `scheduledFor` entre hoy y los próximos `daysBefore` días                      | WARNING               | daysBefore: 7        |
| MAINTENANCE_OVERDUE | Orden `PENDING` con `scheduledFor` anterior a hoy                                                  | CRITICAL              | —                    |
| LICENSE_EXPIRING    | Licencia que caduca en `daysBefore` días o menos                                                   | WARNING               | daysBefore: 30       |
| LICENSE_EXPIRED     | Licencia con fecha de caducidad anterior a hoy                                                     | CRITICAL              | —                    |
| HIGH_CONSUMPTION    | Consumo medio de los últimos 30 días superior en `thresholdPercent` % al de los 60 días anteriores | WARNING               | thresholdPercent: 25 |

**Funcionamiento del evaluador:**

- Se ejecuta cada 15 minutos (`alerts.evaluation.interval-ms`, por defecto 900000), con un retraso inicial de 60 segundos para esperar a que arranquen los demás servicios.
- Cada alerta tiene una `dedupKey` única, por ejemplo `MAINTENANCE_DUE:<orderId>`, de modo que una misma condición no genera alertas duplicadas.
- Si la condición deja de cumplirse, la alerta pasa a `RESOLVED` en la siguiente evaluación.
- Si una dependencia no responde, el evaluador omite esa regla en esa ronda y continúa con las demás.

### Endpoints requeridos

#### 1. GET /api/alerts

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Lista paginada de alertas, ordenada por severidad descendente y fecha de creación descendente.

**Query params:**

- `status` (enum, opcional) — ej: `OPEN`
- `severity` (enum, opcional) y `type` (enum, opcional)
- `vehicle` (UUID, opcional)
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100)

**Response 200 OK:**

```json
{
  "content": [
    {
      "id": "d1a5c7e9-3f42-4b8a-a6d0-9e2b4c8f1a01",
      "type": "MAINTENANCE_OVERDUE",
      "severity": "CRITICAL",
      "status": "OPEN",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a05",
      "driverId": null,
      "message": "Mantenimiento OIL_CHANGE vencido desde el 2026-09-28 en el vehículo 3342-HJK",
      "createdAt": "2026-10-05T06:15:00Z",
      "acknowledgedAt": null
    },
    {
      "id": "d1a5c7e9-3f42-4b8a-a6d0-9e2b4c8f1a02",
      "type": "LICENSE_EXPIRING",
      "severity": "WARNING",
      "status": "ACKNOWLEDGED",
      "vehicleId": null,
      "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a02",
      "message": "La licencia de Miguel Torres caduca el 2026-10-20",
      "createdAt": "2026-10-04T06:15:00Z",
      "acknowledgedAt": "2026-10-04T09:12:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 12,
  "totalPages": 1
}
```

#### 2. POST /api/alerts/{alertId}/acknowledge

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Marca una alerta abierta como reconocida. El body es opcional.

**Body:**

```json
{
  "comment": "Cita pedida con el taller para el jueves"
}
```

**Response 200 OK:** la alerta con `status` igual a `ACKNOWLEDGED`, y `acknowledgedAt` y `acknowledgedBy` informados.

**Errores:** 404 `ALERT_NOT_FOUND`; 409 `INVALID_ALERT_STATE` si la alerta no está en `OPEN`.

#### 3. GET /api/alerts/rules

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Lista la configuración de las reglas de evaluación.

**Response 200 OK:**

```json
[
  {
    "type": "MAINTENANCE_DUE",
    "enabled": true,
    "severity": "WARNING",
    "params": { "daysBefore": 7 }
  },
  {
    "type": "MAINTENANCE_OVERDUE",
    "enabled": true,
    "severity": "CRITICAL",
    "params": {}
  },
  {
    "type": "LICENSE_EXPIRING",
    "enabled": true,
    "severity": "WARNING",
    "params": { "daysBefore": 30 }
  },
  {
    "type": "LICENSE_EXPIRED",
    "enabled": true,
    "severity": "CRITICAL",
    "params": {}
  },
  {
    "type": "HIGH_CONSUMPTION",
    "enabled": true,
    "severity": "WARNING",
    "params": { "thresholdPercent": 25 }
  }
]
```

#### 4. PUT /api/alerts/rules/{type}

- **Acceso:** ADMIN
- **Descripción:** Modifica una regla: activarla o desactivarla, cambiar su severidad o ajustar sus parámetros. El cambio se aplica en la siguiente evaluación.

**Body:**

```json
{
  "enabled": true,
  "severity": "CRITICAL",
  "params": { "daysBefore": 14 }
}
```

**Response 200 OK:** la regla actualizada.

**Errores:** 404 `RULE_NOT_FOUND`; 400 `VALIDATION_ERROR` si un parámetro no es válido para ese tipo de regla; 403 `FORBIDDEN` si el usuario es MANAGER.

#### 5. POST /api/alerts/evaluate

- **Acceso:** ADMIN
- **Descripción:** Lanza una evaluación inmediata, sin esperar al evaluador programado. Resulta útil para probar las reglas.

**Response 200 OK:**

```json
{
  "created": 3,
  "resolved": 1,
  "skipped": ["HIGH_CONSUMPTION"],
  "durationMs": 842
}
```

`skipped` lista los tipos de regla que no se evaluaron porque una dependencia no respondió.

#### 6. GET /api/alerts/stats

- **Acceso:** MANAGER, ADMIN y llamadas internas
- **Descripción:** Serie temporal de alertas creadas, con desglose por severidad, y recuento de alertas abiertas. Lo consume ms-dashboard.

**Query params:**

- `from` y `to` (fecha `YYYY-MM-DD`, requeridos)
- `granularity` (enum, opcional, por defecto `DAY`) — `DAY`, `WEEK` o `MONTH`

**Response 200 OK:**

```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "totals": { "created": 11 },
  "series": [
    {
      "period": "2026-09-07",
      "created": 3,
      "bySeverity": { "INFO": 0, "WARNING": 2, "CRITICAL": 1 }
    },
    {
      "period": "2026-09-14",
      "created": 5,
      "bySeverity": { "INFO": 0, "WARNING": 3, "CRITICAL": 2 }
    },
    {
      "period": "2026-09-21",
      "created": 3,
      "bySeverity": { "INFO": 0, "WARNING": 2, "CRITICAL": 1 }
    }
  ],
  "open": { "INFO": 0, "WARNING": 9, "CRITICAL": 2 }
}
```

### Modelo de datos — ms-alerts

#### Alert

| Campo          | Tipo    | Descripción                                |
| -------------- | ------- | ------------------------------------------ |
| id             | UUID    | Identificador único                        |
| type           | Enum    | Tipo de alerta                             |
| severity       | Enum    | Severidad                                  |
| status         | Enum    | Estado de la alerta                        |
| vehicleId      | UUID    | Vehículo afectado (opcional)               |
| driverId       | UUID    | Conductor afectado (opcional)              |
| message        | String  | Mensaje legible                            |
| dedupKey       | String  | Clave única de la condición que la originó |
| createdAt      | Instant | Fecha de creación                          |
| acknowledgedAt | Instant | Fecha de reconocimiento                    |
| acknowledgedBy | UUID    | Usuario que la reconoció                   |
| comment        | String  | Comentario del reconocimiento              |
| resolvedAt     | Instant | Fecha de resolución                        |

#### AlertRule

| Campo     | Tipo    | Descripción                                     |
| --------- | ------- | ----------------------------------------------- |
| type      | Enum    | Tipo de alerta a la que aplica (clave primaria) |
| enabled   | Boolean | Indica si la regla está activa                  |
| severity  | Enum    | Severidad que se asigna a las alertas           |
| params    | JSON    | Parámetros de la regla                          |
| updatedAt | Instant | Fecha de la última modificación                 |

#### Enums — ms-alerts

- `AlertType`: `MAINTENANCE_DUE`, `MAINTENANCE_OVERDUE`, `LICENSE_EXPIRING`, `LICENSE_EXPIRED`, `HIGH_CONSUMPTION`
- `AlertSeverity`: `INFO`, `WARNING`, `CRITICAL`
- `AlertStatus`: `OPEN`, `ACKNOWLEDGED`, `RESOLVED`

## ms-dashboard (Puerto 8089)

### Descripción

Agrega datos de los demás microservicios en un resumen ejecutivo de la flota y en series temporales listas para representarse en gráficos. No tiene base de datos: guarda las respuestas en una caché Caffeine con una validez de 60 segundos.

### Reglas de agregación

- Las llamadas a los servicios de origen se lanzan en paralelo.
- Si un servicio de origen no responde, el dashboard devuelve los datos disponibles con `partial` igual a `true` y la lista `unavailable` con los servicios afectados. Los valores que dependían de ese servicio se devuelven como `null`.
- La caché usa como clave el endpoint y todos sus parámetros.

### Endpoints requeridos

#### 1. GET /api/dashboard

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Resumen principal de la flota para los últimos días: estado de los vehículos, indicadores clave, alertas abiertas y próximos mantenimientos.

**Query params:**

- `days` (int, opcional, por defecto 30, entre 1 y 365)

**Response 200 OK:**

```json
{
  "generatedAt": "2026-10-05T10:30:00Z",
  "period": { "from": "2026-09-05", "to": "2026-10-05" },
  "partial": false,
  "unavailable": [],
  "fleet": {
    "total": 25,
    "available": 14,
    "inUse": 8,
    "inMaintenance": 2,
    "outOfService": 1
  },
  "kpis": {
    "routesCompleted": 412,
    "distanceKm": 38420.5,
    "fuelLiters": 4226.3,
    "fuelCost": 6846.61,
    "avgConsumptionL100km": 11.0,
    "maintenanceCost": 2140.25
  },
  "alerts": { "open": 11, "critical": 2, "warning": 9, "info": 0 },
  "upcomingMaintenance": [
    {
      "orderId": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "type": "OIL_CHANGE",
      "scheduledFor": "2026-10-08"
    }
  ]
}
```

Origen de cada bloque: `fleet` de ms-vehicles, `kpis` de las estadísticas de ms-routes, ms-fuel y ms-maintenance, `alerts` de ms-alerts y `upcomingMaintenance` de las 5 próximas órdenes pendientes de ms-maintenance.

#### 2. GET /api/dashboard/timeseries

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Series temporales de una o varias métricas con la misma granularidad, listas para un gráfico de líneas o de barras.

**Query params:**

- `metrics` (lista separada por comas, requerido) — valores: `DISTANCE_KM`, `FUEL_LITERS`, `FUEL_COST`, `CONSUMPTION_L100KM`, `MAINTENANCE_COST`, `ALERTS_CREATED`
- `from` y `to` (fecha `YYYY-MM-DD`, requeridos). El rango máximo es de 366 días
- `granularity` (enum, opcional, por defecto `DAY`) — `DAY`, `WEEK` o `MONTH`

**Response 200 OK:**

```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "partial": false,
  "unavailable": [],
  "series": [
    {
      "metric": "DISTANCE_KM",
      "unit": "km",
      "points": [
        { "period": "2026-09-07", "value": 9034.1 },
        { "period": "2026-09-14", "value": 9710.6 },
        { "period": "2026-09-21", "value": 9340.2 }
      ]
    },
    {
      "metric": "FUEL_COST",
      "unit": "EUR",
      "points": [
        { "period": "2026-09-07", "value": 1595.21 },
        { "period": "2026-09-14", "value": 1746.2 },
        { "period": "2026-09-21", "value": 1664.39 }
      ]
    }
  ]
}
```

**Errores:** 400 `VALIDATION_ERROR` si alguna métrica no existe, si falta un parámetro requerido o si el rango supera los 366 días.

#### 3. GET /api/dashboard/vehicles/ranking

- **Acceso:** MANAGER, ADMIN
- **Descripción:** Clasificación de vehículos por coste de combustible o por consumo, a partir de `GET /api/fuel/consumption`. El resultado va ordenado de mayor a menor valor.

**Query params:**

- `metric` (enum, requerido) — `FUEL_COST` o `CONSUMPTION`
- `days` (int, opcional, por defecto 30)
- `limit` (int, opcional, por defecto 5, máximo 20)
- `type` (enum, opcional) — limita el ranking a un tipo de vehículo, útil para comparar consumos de vehículos equivalentes

**Response 200 OK:**

```json
{
  "metric": "CONSUMPTION",
  "unit": "L/100 km",
  "period": { "from": "2026-09-05", "to": "2026-10-05" },
  "ranking": [
    {
      "rank": 1,
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a03",
      "plate": "9012-PQR",
      "type": "TRUCK",
      "value": 27.5
    },
    {
      "rank": 2,
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a04",
      "plate": "1188-BCD",
      "type": "TRUCK",
      "value": 26.8
    },
    {
      "rank": 3,
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "type": "VAN",
      "value": 10.9
    }
  ]
}
```

### Modelo de datos — ms-dashboard

Este servicio no tiene persistencia. Solo mantiene la caché en memoria descrita arriba.

## Comunicación entre microservicios

La comunicación es síncrona por HTTP con OpenFeign. Cada servicio de dominio valida el JWT por su cuenta, y las llamadas entre servicios se autentican con una clave interna compartida.

### Autenticación

- **Peticiones externas:** cabecera `Authorization: Bearer <token>`. Cada servicio verifica la firma HS256 con `JWT_SECRET`, lee los claims `sub`, `username` y `role`, y usa el rol para decidir el acceso.
- **Endpoints públicos:** `POST /api/auth/register`, `POST /api/auth/login`, `GET /health` del gateway, `/actuator/health` y Swagger UI.
- **Llamadas entre servicios:** cabecera `X-Internal-Key` con el valor de `INTERNAL_API_KEY`. Se usa también en las tareas programadas, que no tienen un token de usuario. El servicio que la recibe correcta trata la llamada con los permisos de un MANAGER. Los endpoints marcados como llamadas internas aceptan el JWT o esta clave.
- **Protección:** el gateway elimina `X-Internal-Key` de las peticiones externas.

### Mapa de dependencias

Las llamadas solo van hacia abajo, así que no hay dependencias circulares.

| Servicio que llama               | Servicios a los que llama                                  |
| -------------------------------- | ---------------------------------------------------------- |
| ms-gateway                       | Todos (enrutamiento)                                       |
| ms-dashboard                     | ms-vehicles, ms-routes, ms-fuel, ms-maintenance, ms-alerts |
| ms-alerts                        | ms-maintenance, ms-fuel, ms-drivers, ms-vehicles           |
| ms-routes                        | ms-vehicles, ms-drivers                                    |
| ms-maintenance                   | ms-vehicles                                                |
| ms-fuel                          | ms-vehicles                                                |
| ms-auth, ms-vehicles, ms-drivers | —                                                          |

### Llamadas entre servicios

| Acción                               | Servicio que la inicia | Llamadas que desencadena                                                                         |
| ------------------------------------ | ---------------------- | ------------------------------------------------------------------------------------------------ |
| Planificar una ruta                  | ms-routes              | ms-vehicles: consultar el vehículo. ms-drivers: consultar el conductor                           |
| Iniciar una ruta                     | ms-routes              | ms-vehicles: consultar el vehículo y cambiarlo a `IN_USE`                                        |
| Completar una ruta                   | ms-routes              | ms-vehicles: cambiarlo a `AVAILABLE` y actualizar el odómetro                                    |
| Registrar un repostaje               | ms-fuel                | ms-vehicles: consultar el tipo de combustible y la capacidad del depósito                        |
| Crear un plan de mantenimiento       | ms-maintenance         | ms-vehicles: comprobar que existe y leer el odómetro                                             |
| Iniciar una orden de mantenimiento   | ms-maintenance         | ms-vehicles: cambiarlo a `IN_MAINTENANCE`                                                        |
| Completar una orden de mantenimiento | ms-maintenance         | ms-vehicles: cambiarlo a `AVAILABLE` y actualizar el odómetro                                    |
| Tarea diaria de mantenimiento        | ms-maintenance         | ms-vehicles: leer el odómetro de cada vehículo con plan                                          |
| Evaluación de alertas                | ms-alerts              | ms-maintenance: órdenes pendientes. ms-drivers: licencias. ms-fuel: consumo. ms-vehicles: placas |
| Consulta del dashboard               | ms-dashboard           | ms-vehicles, ms-routes, ms-fuel, ms-maintenance y ms-alerts: resúmenes y estadísticas            |

### Clientes Feign

Ejemplo en ms-routes consumiendo ms-vehicles:

```java
@FeignClient(name = "ms-vehicles", url = "${vehicles.service.url}",
             configuration = InternalFeignConfig.class)
public interface VehicleClient {
    @GetMapping("/api/vehicles/{vehicleId}")
    VehicleResponse getVehicle(@PathVariable UUID vehicleId);

    @PatchMapping("/api/vehicles/{vehicleId}/status")
    VehicleResponse updateStatus(@PathVariable UUID vehicleId,
                                 @RequestBody VehicleStatusRequest request);
}
```

Configuración común que añade la clave interna a cada llamada:

```java
public class InternalFeignConfig {
    @Bean
    public RequestInterceptor internalKeyInterceptor(
        @Value("${internal.api-key}") String apiKey) {
        return template -> template.header("X-Internal-Key", apiKey);
    }
}
```

Ejemplo en ms-dashboard consumiendo ms-fuel:

```java
@FeignClient(name = "ms-fuel", url = "${fuel.service.url}",
             configuration = InternalFeignConfig.class)
public interface FuelClient {
    @GetMapping("/api/fuel/stats")
    FuelStatsResponse getStats(@RequestParam LocalDate from,
                               @RequestParam LocalDate to,
                               @RequestParam String granularity);
}
```

### Resiliencia

- **Timeouts:** 2 segundos de conexión y 5 de lectura en todos los clientes Feign.
- **Reintentos:** un reintento, solo en peticiones GET.
- **Circuit breaker (Resilience4j):** uno por cliente. Se abre cuando falla el 50 % de las últimas 10 llamadas y espera 30 segundos antes de volver a probar.
- **Dependencia caída:** el servicio responde `503 SERVICE_UNAVAILABLE`, salvo ms-dashboard y ms-alerts, que continúan con los datos disponibles como se describe en cada sección.

## Contrato de errores común

Todos los servicios devuelven los errores con esta estructura:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "La petición contiene campos no válidos",
  "details": [{ "field": "year", "reason": "debe estar entre 1990 y 2027" }],
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`details` solo aparece en `VALIDATION_ERROR`. En `SERVICE_UNAVAILABLE` se añade el campo `service` con el nombre del servicio que no responde.

| HTTP | Código                                                                                                 | Cuándo                                               |
| ---- | ------------------------------------------------------------------------------------------------------ | ---------------------------------------------------- |
| 400  | VALIDATION_ERROR                                                                                       | Algún campo no cumple las validaciones               |
| 401  | UNAUTHORIZED, TOKEN_EXPIRED                                                                            | Falta el token, no es válido o ha caducado           |
| 403  | FORBIDDEN                                                                                              | El rol del usuario no tiene permiso                  |
| 404  | VEHICLE_NOT_FOUND, DRIVER_NOT_FOUND, ROUTE_NOT_FOUND, ORDER_NOT_FOUND, ALERT_NOT_FOUND, RULE_NOT_FOUND | El recurso no existe                                 |
| 409  | Códigos de negocio de cada servicio                                                                    | La operación entra en conflicto con el estado actual |
| 429  | RATE_LIMIT_EXCEEDED                                                                                    | Se supera el límite del gateway                      |
| 503  | SERVICE_UNAVAILABLE                                                                                    | Una dependencia no responde                          |

## Convenciones comunes

- Los identificadores son UUID.
- Los instantes usan ISO-8601 en UTC (`2026-10-05T10:30:00Z`) y las fechas sin hora usan `YYYY-MM-DD`.
- La paginación usa `page` desde 0 y `size` con valor por defecto 20 y máximo 100. La respuesta incluye `content`, `page`, `size`, `totalElements` y `totalPages`.
- Los importes van en EUR con 2 decimales, las distancias en km, los volúmenes en litros y el consumo en L/100 km.
- Cada servicio expone `/actuator/health` y Swagger UI en `/swagger-ui.html`.

## Docker Compose

Todo el sistema se levanta con `docker compose up --build`. Una sola instancia de PostgreSQL aloja una base de datos por microservicio, creadas por un script de inicialización, y cada servicio aplica sus migraciones de Flyway al arrancar.

### Variables de entorno

Crear un archivo `.env` en la raíz con los valores reales y añadirlo al `.gitignore`. En el repositorio se sube un `.env.example` con los mismos nombres y valores de ejemplo. `JWT_SECRET` debe tener al menos 32 caracteres para HS256.

```
POSTGRES_USER=fleet
POSTGRES_PASSWORD=cambia_esta_clave
JWT_SECRET=cambia_este_secreto_de_al_menos_32_caracteres
INTERNAL_API_KEY=cambia_esta_clave_interna
DEMO_ADMIN_PASSWORD=Cambia_Admin123
SPRING_PROFILES_ACTIVE=demo
DEMO_SEED=42
DEMO_DAYS=90
```

### docker/postgres/init-databases.sql

```sql
CREATE DATABASE auth_db;
CREATE DATABASE vehicles_db;
CREATE DATABASE drivers_db;
CREATE DATABASE routes_db;
CREATE DATABASE maintenance_db;
CREATE DATABASE fuel_db;
CREATE DATABASE alerts_db;
```

### docker-compose.yml

```yaml
x-domain-env: &domain-env
  JWT_SECRET: ${JWT_SECRET}
  INTERNAL_API_KEY: ${INTERNAL_API_KEY}
  SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-demo}
  DEMO_SEED: ${DEMO_SEED:-42}
  DEMO_DAYS: ${DEMO_DAYS:-90}
  SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-fleet}
  SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-fleet}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - ./docker/postgres/init-databases.sql:/docker-entrypoint-initdb.d/init-databases.sql
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5433:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-fleet}"]
      interval: 5s
      timeout: 3s
      retries: 10

  ms-gateway:
    build: ./ms-gateway
    ports:
      - "8080:8080"
    depends_on:
      - ms-auth
      - ms-vehicles
      - ms-drivers
      - ms-routes
      - ms-maintenance
      - ms-fuel
      - ms-alerts
      - ms-dashboard
    environment:
      AUTH_SERVICE_URL: http://ms-auth:8081
      VEHICLES_SERVICE_URL: http://ms-vehicles:8082
      DRIVERS_SERVICE_URL: http://ms-drivers:8083
      ROUTES_SERVICE_URL: http://ms-routes:8085
      MAINTENANCE_SERVICE_URL: http://ms-maintenance:8086
      FUEL_SERVICE_URL: http://ms-fuel:8087
      ALERTS_SERVICE_URL: http://ms-alerts:8088
      DASHBOARD_SERVICE_URL: http://ms-dashboard:8089

  ms-auth:
    build: ./ms-auth
    ports:
      - "8081:8081"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      <<: *domain-env
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/auth_db
      JWT_EXPIRATION: 3600000
      DEMO_ADMIN_PASSWORD: ${DEMO_ADMIN_PASSWORD}

  ms-vehicles:
    build: ./ms-vehicles
    ports:
      - "8082:8082"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      <<: *domain-env
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/vehicles_db

  ms-drivers:
    build: ./ms-drivers
    ports:
      - "8083:8083"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      <<: *domain-env
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/drivers_db

  ms-routes:
    build: ./ms-routes
    ports:
      - "8085:8085"
    depends_on:
      postgres:
        condition: service_healthy
      ms-vehicles:
        condition: service_started
      ms-drivers:
        condition: service_started
    environment:
      <<: *domain-env
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/routes_db
      VEHICLES_SERVICE_URL: http://ms-vehicles:8082
      DRIVERS_SERVICE_URL: http://ms-drivers:8083

  ms-maintenance:
    build: ./ms-maintenance
    ports:
      - "8086:8086"
    depends_on:
      postgres:
        condition: service_healthy
      ms-vehicles:
        condition: service_started
    environment:
      <<: *domain-env
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/maintenance_db
      VEHICLES_SERVICE_URL: http://ms-vehicles:8082

  ms-fuel:
    build: ./ms-fuel
    ports:
      - "8087:8087"
    depends_on:
      postgres:
        condition: service_healthy
      ms-vehicles:
        condition: service_started
    environment:
      <<: *domain-env
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/fuel_db
      VEHICLES_SERVICE_URL: http://ms-vehicles:8082

  ms-alerts:
    build: ./ms-alerts
    ports:
      - "8088:8088"
    depends_on:
      postgres:
        condition: service_healthy
      ms-vehicles:
        condition: service_started
      ms-drivers:
        condition: service_started
      ms-maintenance:
        condition: service_started
      ms-fuel:
        condition: service_started
    environment:
      <<: *domain-env
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/alerts_db
      VEHICLES_SERVICE_URL: http://ms-vehicles:8082
      DRIVERS_SERVICE_URL: http://ms-drivers:8083
      MAINTENANCE_SERVICE_URL: http://ms-maintenance:8086
      FUEL_SERVICE_URL: http://ms-fuel:8087
      ALERTS_EVALUATION_INTERVAL_MS: 900000

  ms-dashboard:
    build: ./ms-dashboard
    ports:
      - "8089:8089"
    depends_on:
      - ms-vehicles
      - ms-routes
      - ms-fuel
      - ms-maintenance
      - ms-alerts
    environment:
      <<: *domain-env
      VEHICLES_SERVICE_URL: http://ms-vehicles:8082
      ROUTES_SERVICE_URL: http://ms-routes:8085
      FUEL_SERVICE_URL: http://ms-fuel:8087
      MAINTENANCE_SERVICE_URL: http://ms-maintenance:8086
      ALERTS_SERVICE_URL: http://ms-alerts:8088

volumes:
  pgdata:
```

> **Nota:** el seeder de cada servicio solo inserta datos si sus tablas están vacías. Para regenerar el histórico desde cero, ejecutar `docker compose down -v` y volver a levantar el sistema.

## Épica e historias de usuario del sprint

La épica EP-01 se divide en 22 historias de usuario que suman 88 puntos de historia para un sprint de 2 semanas: 71 puntos son compromiso firme y 17 entran si el ritmo del equipo lo permite. Otras 2 historias quedan fuera del compromiso como ampliación.

### Épica

**EP-01 — Plataforma FleetControl.** Como gestor de una flota de vehículos, quiero una plataforma de microservicios que centralice vehículos, conductores, rutas, mantenimiento, combustible y alertas, para controlar los costes de la flota y evitar incidencias por revisiones o licencias vencidas.

**Objetivo del sprint:** al terminar, un solo `docker compose up --build` levanta los 9 servicios con datos de demostración, y un gestor puede autenticarse, gestionar vehículos y conductores, planificar y ejecutar rutas, registrar repostajes y mantenimientos, recibir alertas y consultar un dashboard con series temporales.

### Supuestos del sprint

- Equipo de 4 personas, 10 días laborables y una capacidad de unos 88 puntos de historia, con escala Fibonacci (1, 2, 3, 5, 8). Los puntos son relativos al equipo: si cambia el tamaño o la experiencia, hay que reestimar.
- Prioridad con MoSCoW: Must es compromiso firme, Should entra si el ritmo lo permite y Could queda fuera del compromiso.
- La semana 1 corresponde a los días D1 a D5 y la semana 2 a los días D6 a D10.

### Backlog del sprint

| ID    | Historia                                         | Servicio                                      | Puntos | Prioridad | Semana | Depende de          |
| ----- | ------------------------------------------------ | --------------------------------------------- | ------ | --------- | ------ | ------------------- |
| US-01 | Infraestructura base y Docker Compose            | Todos                                         | 5      | Must      | 1      | —                   |
| US-02 | Registro e inicio de sesión                      | ms-auth                                       | 3      | Must      | 1      | US-01               |
| US-03 | Validación de JWT, roles y clave interna         | Todos                                         | 5      | Must      | 1      | US-02               |
| US-04 | Gateway: enrutamiento y health                   | ms-gateway                                    | 3      | Must      | 1      | US-01               |
| US-05 | Gateway: rate limiting                           | ms-gateway                                    | 2      | Should    | 1      | US-04               |
| US-06 | Alta, consulta y edición de vehículos            | ms-vehicles                                   | 3      | Must      | 1      | US-03               |
| US-07 | Estado y odómetro de vehículos                   | ms-vehicles                                   | 3      | Must      | 1      | US-06               |
| US-08 | Gestión de conductores                           | ms-drivers                                    | 3      | Must      | 1      | US-03               |
| US-09 | Planificación de rutas                           | ms-routes                                     | 5      | Must      | 1      | US-07, US-08        |
| US-10 | Inicio y finalización de rutas                   | ms-routes                                     | 5      | Must      | 2      | US-09               |
| US-11 | Repostajes y consumo                             | ms-fuel                                       | 5      | Must      | 1      | US-06               |
| US-12 | Planes de mantenimiento programado               | ms-maintenance                                | 3      | Must      | 1      | US-06               |
| US-13 | Órdenes de mantenimiento: generación y ejecución | ms-maintenance                                | 5      | Must      | 2      | US-07, US-12        |
| US-14 | Evaluador de alertas                             | ms-alerts                                     | 8      | Must      | 2      | US-08, US-11, US-13 |
| US-15 | Gestión y configuración de alertas               | ms-alerts                                     | 2      | Should    | 2      | US-14               |
| US-16 | Generador de datos demo                          | Servicios de dominio                          | 5      | Must      | 2      | US-09, US-11, US-12 |
| US-17 | Endpoints de estadísticas                        | ms-routes, ms-fuel, ms-maintenance, ms-alerts | 5      | Must      | 2      | US-10, US-13, US-14 |
| US-18 | Dashboard: resumen de flota                      | ms-dashboard, ms-vehicles                     | 5      | Must      | 2      | US-17               |
| US-19 | Dashboard: series temporales y ranking           | ms-dashboard                                  | 3      | Should    | 2      | US-17               |
| US-20 | Resiliencia en llamadas entre servicios          | Todos                                         | 3      | Should    | 2      | US-10               |
| US-21 | Pruebas y colección Postman del flujo completo   | Todos                                         | 5      | Should    | 2      | US-18               |
| US-22 | Documentación y README                           | Todos                                         | 2      | Should    | 2      | —                   |
| US-23 | Notificación de alertas críticas por webhook     | ms-alerts                                     | 3      | Could     | —      | US-14               |
| US-24 | Exportación de repostajes y rutas a CSV          | ms-fuel, ms-routes                            | 2      | Could     | —      | US-10, US-11        |

### Historias de usuario

#### US-01 — Infraestructura base y Docker Compose

**Servicio:** todos · **Puntos:** 5 · **Prioridad:** Must · **Semana:** 1

Como equipo de desarrollo, quiero un repositorio con los 9 servicios, una instancia de PostgreSQL y un Docker Compose, para levantar todo el sistema con un solo comando.

**Criterios de aceptación:**

- Dado un `.env` con las variables requeridas, cuando se ejecuta `docker compose up --build`, entonces arrancan los 9 servicios y PostgreSQL sin errores.
- Cada servicio responde en `/actuator/health` y publica su Swagger UI en `/swagger-ui.html`.
- Existen las 7 bases de datos y Flyway aplica las migraciones iniciales de cada servicio.
- Cada servicio usa el puerto indicado en la tabla de microservicios.
- `.env` está en el `.gitignore` y el repositorio incluye un `.env.example`.

#### US-02 — Registro e inicio de sesión

**Servicio:** ms-auth · **Puntos:** 3 · **Prioridad:** Must · **Semana:** 1

Como gestor de flota, quiero registrarme e iniciar sesión, para obtener un token con el que usar la plataforma.

**Criterios de aceptación:**

- Dado un email y una contraseña válidos, cuando se llama a `POST /api/auth/register`, entonces se crea un usuario `MANAGER` y la contraseña se guarda con bcrypt.
- Dado un email que ya existe, cuando se intenta registrar de nuevo, entonces se devuelve 409 `USER_ALREADY_EXISTS`.
- Dada una contraseña de menos de 8 caracteres o sin mayúscula o sin número, entonces se devuelve 400 `VALIDATION_ERROR`.
- Dadas unas credenciales correctas, cuando se llama a `POST /api/auth/login`, entonces se devuelve un JWT con validez de 3600 segundos. Con credenciales incorrectas se devuelve 401 `INVALID_CREDENTIALS`.
- Con el perfil demo existe el usuario `ADMIN`.

#### US-03 — Validación de JWT, roles y clave interna

**Servicio:** todos los servicios de dominio · **Puntos:** 5 · **Prioridad:** Must · **Semana:** 1

Como equipo de desarrollo, quiero un componente de seguridad común, para que todos los servicios apliquen las mismas reglas de acceso.

**Criterios de aceptación:**

- Sin token en un endpoint protegido se devuelve 401 `UNAUTHORIZED`, y con un token caducado 401 `TOKEN_EXPIRED`.
- Un `MANAGER` que llama a un endpoint reservado a `ADMIN` recibe 403 `FORBIDDEN`.
- Una petición con la cabecera `X-Internal-Key` correcta se acepta en los endpoints de llamadas internas, y con una clave incorrecta se devuelve 401.
- `POST /api/auth/validate` devuelve los datos del usuario cuando el token es válido.
- Todos los errores siguen el contrato común con `error`, `message` y `timestamp`.

#### US-04 — Gateway: enrutamiento y health

**Servicio:** ms-gateway · **Puntos:** 3 · **Prioridad:** Must · **Semana:** 1

Como cliente de la API, quiero un único punto de entrada, para no tener que conocer los puertos de cada servicio.

**Criterios de aceptación:**

- Dada una petición con cualquiera de los prefijos de la tabla de enrutamiento, entonces se reenvía al servicio correcto y se devuelve su respuesta sin modificar.
- Si el servicio destino no responde en 5 segundos, se devuelve 503 `SERVICE_UNAVAILABLE`.
- `GET /health` devuelve el estado de los 8 servicios y marca como `DOWN` los que no responden.
- El gateway añade `X-Request-Id` cuando falta y elimina `X-Internal-Key` de las peticiones externas.

#### US-05 — Gateway: rate limiting

**Servicio:** ms-gateway · **Puntos:** 2 · **Prioridad:** Should · **Semana:** 1

Como responsable de la plataforma, quiero limitar las peticiones por IP, para proteger el sistema de un uso excesivo.

**Criterios de aceptación:**

- Dadas más de 60 peticiones en un minuto desde la misma IP, entonces se devuelve 429 `RATE_LIMIT_EXCEEDED` con `retryAfter` y la cabecera `Retry-After`.
- Pasado el minuto, el contador se reinicia y la IP puede volver a llamar.
- El límite se configura con una variable de entorno.

#### US-06 — Alta, consulta y edición de vehículos

**Servicio:** ms-vehicles · **Puntos:** 3 · **Prioridad:** Must · **Semana:** 1

Como gestor, quiero dar de alta, consultar y editar vehículos, para mantener el inventario de la flota.

**Criterios de aceptación:**

- `POST /api/vehicles` crea el vehículo en estado `AVAILABLE`, y una placa duplicada devuelve 409 `VEHICLE_ALREADY_EXISTS`.
- `GET /api/vehicles` es paginado y filtra por `status`, `type` y `plate`.
- `GET /api/vehicles/{vehicleId}` devuelve el detalle, o 404 `VEHICLE_NOT_FOUND` si no existe.
- `PUT /api/vehicles/{vehicleId}` actualiza los datos descriptivos sin tocar la placa, el estado ni el odómetro.
- Los datos inválidos, como un año fuera de rango o un depósito menor o igual que 0, devuelven 400 `VALIDATION_ERROR` con `details`.

#### US-07 — Estado y odómetro de vehículos

**Servicio:** ms-vehicles · **Puntos:** 3 · **Prioridad:** Must · **Semana:** 1

Como gestor, quiero cambiar el estado de un vehículo con reglas de transición, para que la flota refleje su situación real.

**Criterios de aceptación:**

- Solo se permiten las transiciones descritas en la sección de ms-vehicles, y cualquier otra devuelve 409 `INVALID_STATUS_TRANSITION`.
- El odómetro enviado se guarda solo si es mayor o igual que el actual, y si es menor se devuelve 409 `INVALID_ODOMETER`.
- Solo un `ADMIN` puede pasar un vehículo de `OUT_OF_SERVICE` a `AVAILABLE`, y un `MANAGER` recibe 403 `FORBIDDEN`.
- El endpoint acepta la clave interna, para que lo usen ms-routes y ms-maintenance.

#### US-08 — Gestión de conductores

**Servicio:** ms-drivers · **Puntos:** 3 · **Prioridad:** Must · **Semana:** 1

Como gestor, quiero registrar y mantener conductores con su licencia, para asignarlos a rutas con garantías.

**Criterios de aceptación:**

- `POST /api/drivers` crea el conductor en estado `ACTIVE`. Una licencia duplicada devuelve 409 `DRIVER_ALREADY_EXISTS` y una fecha de caducidad pasada devuelve 400 `VALIDATION_ERROR`.
- `GET /api/drivers` es paginado y filtra por `status` y por `licenseExpiringInDays`, incluyendo las licencias ya caducadas.
- `PUT /api/drivers/{driverId}` actualiza los datos y el estado, y permite renovar la licencia con una nueva fecha de caducidad.
- `GET /api/drivers/{driverId}` devuelve el detalle, o 404 `DRIVER_NOT_FOUND` si no existe.

#### US-09 — Planificación de rutas

**Servicio:** ms-routes · **Puntos:** 5 · **Prioridad:** Must · **Semana:** 1

Como gestor, quiero planificar rutas asignando un vehículo y un conductor, para organizar la operación con asignaciones válidas.

**Criterios de aceptación:**

- Dado un vehículo y un conductor válidos, cuando se llama a `POST /api/routes`, entonces se crea la ruta en estado `PLANNED`.
- Con un vehículo `OUT_OF_SERVICE` se devuelve 409 `VEHICLE_NOT_AVAILABLE`.
- Con un conductor inactivo, con la licencia caducada en la fecha de inicio o con una categoría incompatible, se devuelve 409 `DRIVER_NOT_ELIGIBLE`.
- Si el vehículo o el conductor se solapan con otra ruta planificada o en curso, se devuelve 409 `ROUTE_OVERLAP`.
- Si ms-vehicles o ms-drivers no responden, se devuelve 503 `SERVICE_UNAVAILABLE` y no se guarda nada.
- `GET /api/routes` y `GET /api/routes/{routeId}` funcionan con filtros y paginación.

#### US-10 — Inicio y finalización de rutas

**Servicio:** ms-routes · **Puntos:** 5 · **Prioridad:** Must · **Semana:** 2

Como gestor, quiero iniciar y finalizar una ruta, para que el estado y el odómetro del vehículo se actualicen automáticamente.

**Criterios de aceptación:**

- Con una ruta `PLANNED` y un vehículo `AVAILABLE`, `start` deja la ruta en `IN_PROGRESS` y el vehículo en `IN_USE`, y guarda `startedAt` y `startOdometerKm`.
- Con un vehículo en uso o en mantenimiento, `start` devuelve 409 `VEHICLE_NOT_AVAILABLE`. Con una ruta que no está planificada devuelve 409 `INVALID_ROUTE_STATE`.
- Si falla el guardado de la ruta después de cambiar el vehículo, el vehículo vuelve a `AVAILABLE`.
- `complete` calcula `endOdometerKm`, deja el vehículo en `AVAILABLE` con el odómetro actualizado y la ruta en `COMPLETED`.
- Si ms-vehicles no responde durante `complete`, la ruta sigue `IN_PROGRESS` y la operación se puede reintentar.

#### US-11 — Repostajes y consumo

**Servicio:** ms-fuel · **Puntos:** 5 · **Prioridad:** Must · **Semana:** 1

Como gestor, quiero registrar repostajes y consultar el consumo por vehículo, para detectar gastos anómalos.

**Criterios de aceptación:**

- `POST /api/fuel/refuels` toma el combustible del vehículo, calcula `totalCost` con 2 decimales y devuelve `consumptionL100km` entre depósitos llenos, o `null` si no hay un lleno anterior.
- Con litros por encima de la capacidad del depósito se devuelve 400 `VALIDATION_ERROR`, y con un odómetro menor que el del repostaje anterior 409 `INVALID_ODOMETER`.
- `GET /api/fuel/refuels` es paginado y filtra por vehículo y fechas.
- `GET /api/fuel/consumption` devuelve por vehículo los litros, el coste, la distancia y el consumo medio, con la placa obtenida de ms-vehicles.
- El cálculo del consumo tiene tests unitarios que incluyen el ejemplo de la sección de ms-fuel: `(20 + 38) / 500 × 100 = 11,6 L/100 km`.

#### US-12 — Planes de mantenimiento programado

**Servicio:** ms-maintenance · **Puntos:** 3 · **Prioridad:** Must · **Semana:** 1

Como gestor, quiero definir planes de mantenimiento por kilómetros o por días, para no olvidar ninguna revisión.

**Criterios de aceptación:**

- `POST /api/maintenance/plans` crea el plan y calcula `nextDueAt` y `nextDueKm`.
- Sin ningún intervalo se devuelve 400 `VALIDATION_ERROR`, y con un segundo plan activo del mismo tipo para el vehículo 409 `PLAN_ALREADY_EXISTS`.
- Si faltan `lastDoneAt` y `lastDoneKm`, se usan la fecha de hoy y el odómetro actual del vehículo.
- `GET /api/maintenance/plans` filtra por vehículo, tipo y `active`.

#### US-13 — Órdenes de mantenimiento: generación y ejecución

**Servicio:** ms-maintenance · **Puntos:** 5 · **Prioridad:** Must · **Semana:** 2

Como gestor, quiero que el sistema genere las órdenes de trabajo cuando se acerca un vencimiento y poder ejecutarlas, para mantener los vehículos al día.

**Criterios de aceptación:**

- La tarea diaria crea una orden `PENDING` cuando faltan 7 días o menos, o 500 km o menos, para el vencimiento de un plan, y no duplica la orden si ya hay una abierta.
- `GET /api/maintenance/orders` filtra por vehículo, estado, tipo y `dueBefore`.
- `start` deja la orden en `IN_PROGRESS` y el vehículo en `IN_MAINTENANCE`. Con el vehículo en uso devuelve 409 `VEHICLE_NOT_AVAILABLE`.
- `complete` registra el coste, devuelve el vehículo a `AVAILABLE` y recalcula `nextDueAt` y `nextDueKm` del plan.
- Una orden en un estado que no corresponde devuelve 409 `INVALID_ORDER_STATE`.

#### US-14 — Evaluador de alertas

**Servicio:** ms-alerts · **Puntos:** 8 · **Prioridad:** Must · **Semana:** 2

Como gestor, quiero recibir alertas automáticas de mantenimiento, licencias y consumo, para actuar antes de que se produzcan incidencias.

**Criterios de aceptación:**

- El evaluador se ejecuta cada 15 minutos y genera alertas para las 5 reglas de la sección de ms-alerts.
- Una misma condición no genera alertas duplicadas gracias a `dedupKey`.
- Cuando la condición desaparece, la alerta pasa a `RESOLVED` en la siguiente evaluación.
- Si una dependencia no responde, se omite esa regla, se informa en `skipped` y las demás reglas se evalúan igualmente.
- `GET /api/alerts` es paginado, filtra por estado, severidad, tipo y vehículo, y ordena por severidad.
- `POST /api/alerts/evaluate`, solo para `ADMIN`, lanza la evaluación y devuelve `created`, `resolved`, `skipped` y `durationMs`.

#### US-15 — Gestión y configuración de alertas

**Servicio:** ms-alerts · **Puntos:** 2 · **Prioridad:** Should · **Semana:** 2

Como gestor, quiero reconocer las alertas y ajustar sus umbrales, para que el sistema avise con la antelación que necesita mi operación.

**Criterios de aceptación:**

- `POST /api/alerts/{alertId}/acknowledge` pasa la alerta de `OPEN` a `ACKNOWLEDGED` y guarda usuario, fecha y comentario. Con otro estado devuelve 409 `INVALID_ALERT_STATE`.
- `GET /api/alerts/rules` lista las 5 reglas con su configuración.
- `PUT /api/alerts/rules/{type}` solo lo puede usar un `ADMIN`, valida los parámetros y se aplica en la siguiente evaluación.

#### US-16 — Generador de datos demo

**Servicio:** ms-auth, ms-vehicles, ms-drivers, ms-routes, ms-fuel y ms-maintenance · **Puntos:** 5 · **Prioridad:** Must · **Semana:** 2

Como equipo, quiero que el sistema genere un histórico de 90 días al arrancar, para probar el dashboard sin cargar datos a mano.

**Criterios de aceptación:**

- Con el perfil demo, cada servicio inserta sus datos al arrancar si sus tablas están vacías. Sin ese perfil, el seeder no se ejecuta.
- Con la misma `DEMO_SEED` el resultado es idéntico. Los UUID son deterministas y las referencias entre servicios son coherentes.
- Los volúmenes se acercan a los de la tabla de la sección de datos demo, y el consumo respeta el valor base de cada tipo de vehículo con una variación de ±8 %.
- Hay 2 vehículos con un consumo reciente un 30 % superior a su media, 3 licencias próximas a caducar, 1 caducada y órdenes de mantenimiento pendientes y vencidas.

#### US-17 — Endpoints de estadísticas

**Servicio:** ms-routes, ms-fuel, ms-maintenance y ms-alerts · **Puntos:** 5 · **Prioridad:** Must · **Semana:** 2

Como desarrollador del dashboard, quiero series temporales por servicio, para construir gráficos sin procesar datos en bruto.

**Criterios de aceptación:**

- Cada servicio expone `GET /stats` con `from`, `to` y `granularity` (`DAY`, `WEEK` o `MONTH`).
- `period` es la fecha de inicio del periodo, en UTC: el lunes en `WEEK` y el día 1 en `MONTH`.
- Los periodos sin datos devuelven 0, y los totales coinciden con la suma de la serie.
- Hay tests con casos límite: cambio de mes, semana incompleta y rango sin datos.

#### US-18 — Dashboard: resumen de flota

**Servicio:** ms-dashboard y ms-vehicles · **Puntos:** 5 · **Prioridad:** Must · **Semana:** 2

Como gestor, quiero un resumen de la flota, para ver su estado y sus costes de un vistazo.

**Criterios de aceptación:**

- `GET /api/vehicles/summary` devuelve el recuento de vehículos por estado y por tipo.
- `GET /api/dashboard` devuelve `fleet`, `kpis`, `alerts` y `upcomingMaintenance` para los últimos `days` días.
- Las llamadas a los servicios de origen se lanzan en paralelo y la respuesta se guarda en caché durante 60 segundos.
- Si un servicio de origen no responde, el endpoint devuelve 200 con `partial` igual a `true`, la lista `unavailable` y los valores afectados a `null`.
- Con los datos demo, la respuesta tarda menos de 2 segundos sin caché.

#### US-19 — Dashboard: series temporales y ranking

**Servicio:** ms-dashboard · **Puntos:** 3 · **Prioridad:** Should · **Semana:** 2

Como gestor, quiero ver la evolución de los indicadores y los vehículos que más gastan, para detectar tendencias y desviaciones.

**Criterios de aceptación:**

- `GET /api/dashboard/timeseries` acepta `metrics`, `from`, `to` y `granularity`, y devuelve una serie por cada métrica pedida.
- Una métrica desconocida o un rango superior a 366 días devuelve 400 `VALIDATION_ERROR`.
- `GET /api/dashboard/vehicles/ranking` ordena de mayor a menor por `FUEL_COST` o `CONSUMPTION`, y acepta `days`, `limit` y `type`.
- Si un servicio de origen no responde, solo se omiten sus métricas y se informa en `unavailable`.

#### US-20 — Resiliencia en llamadas entre servicios

**Servicio:** todos los que usan Feign · **Puntos:** 3 · **Prioridad:** Should · **Semana:** 2

Como gestor, quiero que la caída de un servicio no tumbe a los demás, para seguir operando con funcionalidad reducida.

**Criterios de aceptación:**

- Todos los clientes Feign usan 2 segundos de timeout de conexión y 5 de lectura, con un reintento solo en peticiones GET.
- El circuit breaker se abre cuando falla el 50 % de las últimas 10 llamadas y espera 30 segundos antes de volver a probar.
- Un test demuestra el comportamiento deteniendo ms-vehicles: ms-routes devuelve 503 `SERVICE_UNAVAILABLE` con el campo `service` informado.

#### US-21 — Pruebas y colección Postman del flujo completo

**Servicio:** todos · **Puntos:** 5 · **Prioridad:** Should · **Semana:** 2

Como equipo, quiero pruebas automáticas y una colección Postman del flujo completo, para comprobar que el sistema funciona de principio a fin.

**Criterios de aceptación:**

- Hay tests unitarios con JUnit 5 y Mockito de las reglas de negocio: transiciones de estado, elegibilidad del conductor, solapamiento de rutas, cálculo de consumo, vencimiento de planes y reglas de alertas.
- Hay al menos un test de integración con Testcontainers en cada servicio con base de datos.
- La colección Postman recorre el flujo: registro, login, alta de vehículo y conductor, ruta planificada, iniciada y completada, repostaje, plan y orden de mantenimiento, alertas y dashboard.
- La colección se ejecuta sin errores sobre un sistema recién levantado.

#### US-22 — Documentación y README

**Servicio:** todos · **Puntos:** 2 · **Prioridad:** Should · **Semana:** 2

Como persona que se incorpora al proyecto, quiero documentación clara, para entender la arquitectura y arrancar el sistema sin ayuda.

**Criterios de aceptación:**

- El README describe la arquitectura, los puertos, las variables de entorno y los pasos de arranque.
- Swagger UI documenta todos los endpoints con ejemplos y códigos de error.
- El README incluye el diagrama de dependencias entre servicios.

#### US-23 — Notificación de alertas críticas por webhook

**Servicio:** ms-alerts · **Puntos:** 3 · **Prioridad:** Could · **Semana:** fuera del compromiso

Como gestor, quiero que las alertas críticas se envíen a una URL configurable, para recibirlas fuera de la plataforma. Esta historia no forma parte de los endpoints requeridos.

**Criterios de aceptación:**

- Con `WEBHOOK_URL` configurada, cada alerta `CRITICAL` nueva se envía en un POST con el JSON de la alerta.
- Si el envío falla, se reintenta 3 veces y se registra el error, sin bloquear el evaluador.

#### US-24 — Exportación de repostajes y rutas a CSV

**Servicio:** ms-fuel y ms-routes · **Puntos:** 2 · **Prioridad:** Could · **Semana:** fuera del compromiso

Como gestor, quiero exportar repostajes y rutas a CSV, para analizarlos en una hoja de cálculo. Esta historia no forma parte de los endpoints requeridos.

**Criterios de aceptación:**

- `GET /api/fuel/refuels/export` y `GET /api/routes/export` aceptan los mismos filtros que los listados y devuelven un CSV en UTF-8 con cabecera.
- Cada exportación devuelve como máximo 10.000 filas.

## Plan del sprint

| Semana | Días   | Historias                    | Puntos | Hito de integración                                                                                         |
| ------ | ------ | ---------------------------- | ------ | ----------------------------------------------------------------------------------------------------------- |
| 1      | D1–D5  | US-01 a US-09, US-11 y US-12 | 40     | D5: el flujo registro, login, alta de vehículo y conductor y ruta planificada funciona a través del gateway |
| 2      | D6–D10 | US-10 y US-13 a US-22        | 48     | D8: alertas y datos demo. D9: dashboard con series. D10: revisión y retrospectiva                           |

### Línea de tiempo del sprint

| Días   | Trabajo previsto                                                                    |
| ------ | ----------------------------------------------------------------------------------- |
| D1–D2  | US-01, US-02, US-03, US-04 (base, auth, gateway)                                    |
| D3–D5  | US-05 a US-09, US-11, US-12 (vehículos, conductores, rutas, combustible, planes)    |
| D6–D8  | US-10, US-13, US-14, US-15, US-16 (rutas en curso, órdenes, alertas, datos demo)    |
| D9     | US-17, US-18, US-19 (estadísticas y dashboard)                                      |
| D9–D10 | US-20, US-21, US-22 (resiliencia, pruebas, documentación), revisión y retrospectiva |

### Ceremonias

| Ceremonia       | Cuándo   | Duración | Objetivo                                                              |
| --------------- | -------- | -------- | --------------------------------------------------------------------- |
| Sprint Planning | D1       | 2 h      | Acordar el objetivo del sprint y el compromiso con las historias Must |
| Daily Scrum     | Cada día | 15 min   | Avance, plan del día y bloqueos                                       |
| Refinamiento    | D6       | 1 h      | Revisar los criterios de aceptación de las historias de la semana 2   |
| Sprint Review   | D10      | 1 h      | Demostrar el flujo completo con los datos demo                        |
| Retrospectiva   | D10      | 1 h      | Acordar mejoras para el siguiente sprint                              |

### Definición de Ready

Una historia entra en el sprint cuando:

- Tiene rol, objetivo y beneficio.
- Sus criterios de aceptación están escritos y el equipo los entiende.
- Sus dependencias están identificadas y no la bloquean.
- El equipo la ha estimado.

### Definición de Done

Una historia está terminada cuando:

- El código se ha revisado en una pull request por al menos otra persona.
- Los tests unitarios de las reglas de negocio nuevas pasan.
- Los endpoints están documentados en Swagger con ejemplos.
- Funciona al levantar el sistema desde cero con `docker compose up --build`.
- No hay secretos en el repositorio.
- Los criterios de aceptación se han verificado en la demo.

### Riesgos

| Riesgo                                                                        | Mitigación                                                                                   |
| ----------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| ms-vehicles bloquea a ms-routes, ms-maintenance y ms-fuel si se retrasa tanto | Terminar US-06 y US-07 entre D1 y D3 y usar un stub con datos fijos mientras                 |
| La integración entre servicios llega tarde                                    | Hito de integración en D5 y colección Postman desde esa fecha                                |
| El generador de datos demo (US-16) es más largo de lo previsto                | Empezar por vehículos, conductores y repostajes, y dejar rutas y mantenimiento para el final |
| Las tareas programadas son difíciles de probar                                | Intervalo configurable y endpoint `POST /api/alerts/evaluate` para lanzarlas a demanda       |
