# FleetControl — Plataforma de Gestión de Flotas

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.1.3-6DB33F?logo=spring&logoColor=white)
![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Container-Docker-2496ED?logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL_15-4169E1?logo=postgresql&logoColor=white)

**Equipo:** Equipo 03  
**Arquitectura:** Microservicios autónomos (API Gateway + 8 servicios, una base de datos por servicio)  
**Recurso externo:** Ninguno. Los datos de demostración los genera un seeder propio (Datafaker con semilla fija)

---

## Descripción

FleetControl es un backend de gestión de flotas compuesto por **9 microservicios** que se comunican por HTTP y cubren el
ciclo completo de un vehículo: alta, asignación a rutas, repostajes, mantenimiento, alertas y análisis.

Permite a un gestor de flota autenticarse, gestionar vehículos y conductores, planificar y ejecutar rutas, registrar
repostajes y mantenimientos, recibir alertas automáticas (revisiones, licencias y consumo anormal) y consultar un
dashboard con series temporales.

---

## Microservicios

| Servicio         | Puerto | Base de datos            | Responsabilidad                                         |
|------------------|--------|--------------------------|---------------------------------------------------------|
| `ms-gateway`     | 8080   | — (Redis)                | Punto de entrada único, enrutamiento y rate limiting    |
| `ms-auth`        | 8081   | `auth_db`                | Registro, login y emisión de tokens JWT                 |
| `ms-vehicles`    | 8082   | `vehicles_db`            | Inventario de vehículos, estado y odómetro              |
| `ms-drivers`     | 8083   | `drivers_db`             | Conductores, licencias y estado                         |
| `ms-routes`      | 8085   | `routes_db`              | Planificación, ejecución e historial de rutas           |
| `ms-maintenance` | 8086   | `maintenance_db`         | Planes de mantenimiento programado y órdenes de trabajo |
| `ms-fuel`        | 8087   | `fuel_db`                | Repostajes y consumo de combustible                     |
| `ms-alerts`      | 8088   | `alerts_db`              | Reglas y alertas de revisión, licencia y consumo        |
| `ms-dashboard`   | 8089   | — (caché Caffeine, 60 s) | Resumen ejecutivo y series temporales agregadas         |

Las 7 bases de datos viven en una única instancia de PostgreSQL durante el desarrollo. El gateway usa además una
instancia de Redis 7 para el rate limiting.

---

## Arquitectura

```
Cliente
  │
  ▼
ms-gateway (:8080)
  │  Enrutamiento por prefijo /api/**
  │  Rate limiting global: 60 req/min por IP (Redis)
  │  Genera X-Request-Id y elimina X-Internal-Key de las peticiones externas
  │
  ├── ms-auth (:8081)
  ├── ms-vehicles (:8082)
  ├── ms-drivers (:8083)
  ├── ms-routes (:8085)
  ├── ms-maintenance (:8086)
  ├── ms-fuel (:8087)
  ├── ms-alerts (:8088)
  └── ms-dashboard (:8089)
```

### Dependencias entre servicios

Las llamadas solo van hacia abajo: no hay dependencias circulares.

```
ms-dashboard   ──► ms-vehicles, ms-routes, ms-fuel, ms-maintenance, ms-alerts
ms-alerts      ──► ms-maintenance, ms-fuel, ms-drivers, ms-vehicles
ms-routes      ──► ms-vehicles, ms-drivers
ms-maintenance ──► ms-vehicles
ms-fuel        ──► ms-vehicles
ms-auth, ms-vehicles, ms-drivers ──► (ninguna)
```

---

## Estado de implementación

| Servicio         | Estado |
|------------------|--------|
| `ms-gateway`     | ✅     |
| `ms-auth`        | ✅     |
| `ms-vehicles`    | 🔧     |
| `ms-drivers`     | 🔧     |
| `ms-routes`      | ⏳     |
| `ms-maintenance` | 🔧     |
| `ms-fuel`        | ⏳     |
| `ms-alerts`      | ⏳     |
| `ms-dashboard`   | ⏳     |

> Leyenda: ⏳ Pendiente, 🔧 Parcial, ✅ Completo

---

## Stack tecnológico

| Capa               | Tecnología                                                 |
|--------------------|------------------------------------------------------------|
| Lenguaje           | Java 17                                                    |
| Framework          | Spring Boot 4.1                                            |
| Cloud              | Spring Cloud 2025.1.3                                      |
| Gateway            | Spring Cloud Gateway (WebFlux reactivo)                    |
| Seguridad          | Spring Security + JJWT 0.13.0 (HS256)                      |
| Resiliencia        | Resilience4j en llamadas Feign (timeout + circuit breaker) |
| Rate limiting      | RedisRateLimiter (token bucket sobre Redis, global por IP) |
| Estado compartido  | Redis 7 (contadores del rate limiting del gateway)         |
| HTTP client        | OpenFeign                                                  |
| Caché              | Caffeine (en memoria)                                      |
| Persistencia       | Spring Data JPA + PostgreSQL 15                            |
| Migraciones        | Flyway                                                     |
| Tareas programadas | Spring `@Scheduled`                                        |
| Datos demo         | Datafaker con semilla fija                                 |
| Mapeo              | MapStruct 1.6.3 + Lombok                                   |
| Documentación      | Swagger UI / OpenAPI 3.0                                   |
| Testing            | JUnit 5 + Mockito + Testcontainers + Postman               |
| Build              | Maven 3.9.16 + Maven Wrapper                               |
| Contenedores       | Docker + Docker Compose                                    |

---

## Variables de entorno

Copiar `.env.example` a `.env` y completar los valores antes de levantar los servicios.

```bash
cp .env.example .env
```

| Variable                        | Descripción                                                                                                          | Ejemplo / por defecto                  |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------|----------------------------------------|
| `POSTGRES_USER`                 | Usuario de PostgreSQL                                                                                                | `fleet`                                |
| `POSTGRES_PASSWORD`             | Contraseña de PostgreSQL                                                                                             | —                                      |
| `JWT_SECRET`                    | Clave secreta para firmar y validar JWT (HS256), compartida por los servicios. Mínimo 32 caracteres                  | —                                      |
| `JWT_EXPIRATION`                | Validez del token en milisegundos (solo `ms-auth`)                                                                   | `3600000` (1 h)                        |
| `INTERNAL_API_KEY`              | Clave interna para llamadas entre servicios (cabecera `X-Internal-Key`)                                              | —                                      |
| `DEMO_ADMIN_PASSWORD`           | Contraseña del usuario `admin@fleetcontrol.com` creado por el seeder                                                 | —                                      |
| `SPRING_PROFILES_ACTIVE`        | Perfil activo. Con `demo` se ejecutan los seeders                                                                    | `demo`                                 |
| `DEMO_SEED`                     | Semilla del generador de datos                                                                                       | `42`                                   |
| `DEMO_DAYS`                     | Días de histórico a generar                                                                                          | `90`                                   |
| `RATE_LIMIT_PER_MINUTE`         | Peticiones por minuto y por IP en el gateway                                                                         | `60`                                   |
| `REDIS_HOST` / `REDIS_PORT`     | Host y puerto de Redis para el rate limiting (definidos en el `docker-compose.yml`)                                  | `redis` / `6379`                       |
| `ALERTS_EVALUATION_INTERVAL_MS` | Intervalo del evaluador de alertas                                                                                   | `900000` (15 min)                      |
| `*_SERVICE_URL`                 | URL de cada servicio (`AUTH_`, `VEHICLES_`, `DRIVERS_`, `ROUTES_`, `MAINTENANCE_`, `FUEL_`, `ALERTS_`, `DASHBOARD_`) | `http://ms-vehicles:8082`              |
| `SPRING_DATASOURCE_URL`         | URL JDBC de cada servicio (definida en el `docker-compose.yml`)                                                      | `jdbc:postgresql://postgres:5432/<db>` |

> **Generar `JWT_SECRET`:** `openssl rand -base64 32` (puede ejecutarse desde Git Bash).

---

## Puesta en marcha

### Requisitos previos

- Docker y Docker Compose instalados
- Archivo `.env` configurado (ver sección anterior)
- Puertos libres: 8080–8089 (excepto 8084) y 5433

### Levantar todo el sistema

```bash
docker compose up --build
```

### Levantar servicios individuales

```bash
# Solo base de datos + auth + gateway
docker compose up postgres ms-auth ms-gateway

# Flujo de rutas (vehículos y conductores son dependencias)
docker compose up postgres ms-vehicles ms-drivers ms-routes ms-gateway
```

### Parar y limpiar

```bash
docker compose down        # parar contenedores
docker compose down -v     # parar y eliminar volúmenes (borra datos y regenera el histórico demo)
```

### Accesos útiles

| Recurso               | URL                                                            |
|-----------------------|----------------------------------------------------------------|
| API (gateway)         | `http://localhost:8080`                                        |
| Estado del sistema    | `http://localhost:8080/health`                                 |
| Swagger UI (servicio) | `http://localhost:<puerto>/swagger-ui.html`                    |
| Health de un servicio | `http://localhost:<puerto>/actuator/health`                    |
| Usuario demo ADMIN    | `admin@fleetcontrol.com` (contraseña en `DEMO_ADMIN_PASSWORD`) |

---

## Endpoints disponibles

El gateway escucha en `http://localhost:8080`.  
Las rutas protegidas (`🔒`) requieren la cabecera `Authorization: Bearer <token>`.  
Los endpoints "interno" también aceptan `X-Internal-Key`.

### Roles

| Rol       | Permisos                                                                                                                                              |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MANAGER` | Gestiona vehículos, conductores, rutas, repostajes y mantenimientos, consulta el dashboard y reconoce alertas. Es el rol que se asigna al registrarse |
| `ADMIN`   | Todo lo de MANAGER, además de modificar reglas de alertas, lanzar evaluaciones y reactivar vehículos fuera de servicio. Lo crea el seeder             |

### Gateway

| Método | Ruta      | Acceso  | Descripción                                                    |
|--------|-----------|---------|----------------------------------------------------------------|
| GET    | `/health` | Público | Estado del gateway y de los 8 servicios (siempre devuelve 200) |
| ANY    | `/api/**` | Público | Proxy inverso hacia el servicio correspondiente                |

### Autenticación — `ms-auth`

| Método | Ruta                 | Acceso  | Descripción                                      |
|--------|----------------------|---------|--------------------------------------------------|
| POST   | `/api/auth/register` | Público | Registra un usuario con rol MANAGER              |
| POST   | `/api/auth/login`    | Público | Login, devuelve un JWT de 1 hora                 |
| POST   | `/api/auth/validate` | Interno | Valida un token y devuelve los datos del usuario |

### Vehículos `🔒` — `ms-vehicles`

| Método | Ruta                               | Descripción                                       |
|--------|------------------------------------|---------------------------------------------------|
| GET    | `/api/vehicles`                    | Lista paginada (`status`, `type`, `plate`)        |
| GET    | `/api/vehicles/summary`            | Recuento por estado y por tipo                    |
| GET    | `/api/vehicles/{vehicleId}`        | Detalle                                           |
| POST   | `/api/vehicles`                    | Alta (nace `AVAILABLE`)                           |
| PUT    | `/api/vehicles/{vehicleId}`        | Actualiza datos descriptivos                      |
| PATCH  | `/api/vehicles/{vehicleId}/status` | Cambia estado y odómetro (transiciones validadas) |

### Conductores `🔒` — `ms-drivers`

| Método | Ruta                      | Descripción                                           |
|--------|---------------------------|-------------------------------------------------------|
| GET    | `/api/drivers`            | Lista paginada (`status`, `licenseExpiringInDays`)    |
| GET    | `/api/drivers/{driverId}` | Detalle                                               |
| POST   | `/api/drivers`            | Alta (nace `ACTIVE`)                                  |
| PUT    | `/api/drivers/{driverId}` | Actualiza datos y estado, permite renovar la licencia |

### Rutas `🔒` — `ms-routes`

| Método | Ruta                             | Descripción                                                  |
|--------|----------------------------------|--------------------------------------------------------------|
| POST   | `/api/routes`                    | Planifica una ruta (vehículo + conductor)                    |
| GET    | `/api/routes`                    | Lista paginada (`vehicle`, `driver`, `status`, `from`, `to`) |
| GET    | `/api/routes/{routeId}`          | Detalle                                                      |
| POST   | `/api/routes/{routeId}/start`    | Inicia la ruta, el vehículo pasa a `IN_USE`                  |
| POST   | `/api/routes/{routeId}/complete` | Finaliza la ruta, actualiza el odómetro                      |
| GET    | `/api/routes/stats`              | Serie temporal de rutas completadas                          |

### Mantenimiento `🔒` — `ms-maintenance`

| Método | Ruta                                         | Descripción                                                                                             |
|--------|----------------------------------------------|---------------------------------------------------------------------------------------------------------|
| POST   | `/api/maintenance/plans`                     | Crea un plan de mantenimiento                                                                           |
| GET    | `/api/maintenance/plans`                     | Lista planes (`vehicle`, `type`, `active`)                                                              |
| GET    | `/api/maintenance/orders`                    | Lista paginada, orden `scheduledFor` ASC (`vehicle`, `status`, `type`, `dueBefore`, `page`, `size`)     |
| POST   | `/api/maintenance/orders/{orderId}/start`    | Inicia la orden `PENDING→IN_PROGRESS`, vehículo a `IN_MAINTENANCE`                                      |
| POST   | `/api/maintenance/orders/{orderId}/complete` | Completa `IN_PROGRESS→COMPLETED`, vehículo a `AVAILABLE` con odómetro, recalcula `nextDueAt/nextDueKm`. |
| GET    | `/api/maintenance/stats`                     | Serie temporal de órdenes y costes (pendiente US-17)                                                    |

> Tarea diaria `06:00 UTC`: crea `PENDING` si faltan ≤7 días o ≤500 km para el vencimiento, sin duplicar abiertas.

### Combustible `🔒` — `ms-fuel`

| Método | Ruta                    | Descripción                                 |
|--------|-------------------------|---------------------------------------------|
| POST   | `/api/fuel/refuels`     | Registra un repostaje y calcula el consumo  |
| GET    | `/api/fuel/refuels`     | Lista paginada (`vehicle`, `from`, `to`)    |
| GET    | `/api/fuel/consumption` | Consumo agregado por vehículo en un periodo |
| GET    | `/api/fuel/stats`       | Serie temporal de litros, coste y consumo   |

### Alertas `🔒` — `ms-alerts`

| Método | Ruta                                | Acceso         | Descripción                                              |
|--------|-------------------------------------|----------------|----------------------------------------------------------|
| GET    | `/api/alerts`                       | MANAGER, ADMIN | Lista paginada (`status`, `severity`, `type`, `vehicle`) |
| POST   | `/api/alerts/{alertId}/acknowledge` | MANAGER, ADMIN | Reconoce una alerta abierta                              |
| GET    | `/api/alerts/rules`                 | MANAGER, ADMIN | Lista la configuración de reglas                         |
| PUT    | `/api/alerts/rules/{type}`          | ADMIN          | Modifica una regla                                       |
| POST   | `/api/alerts/evaluate`              | ADMIN          | Lanza una evaluación inmediata                           |
| GET    | `/api/alerts/stats`                 | MANAGER, ADMIN | Serie temporal de alertas creadas                        |

### Dashboard `🔒` — `ms-dashboard`

| Método | Ruta                              | Descripción                                                              |
|--------|-----------------------------------|--------------------------------------------------------------------------|
| GET    | `/api/dashboard`                  | Resumen de flota, KPIs, alertas y próximos mantenimientos (`days`)       |
| GET    | `/api/dashboard/timeseries`       | Series de una o varias métricas (`metrics`, `from`, `to`, `granularity`) |
| GET    | `/api/dashboard/vehicles/ranking` | Ranking por `FUEL_COST` o `CONSUMPTION` (`days`, `limit`, `type`)        |

### Series temporales (`/stats`)

Los servicios con datos fechados aceptan `from`, `to` y `granularity` (`DAY`, `WEEK` o `MONTH`). Reglas comunes:

- `period` es la fecha de inicio del periodo: el propio día en `DAY`, el lunes en `WEEK` y el día 1 en `MONTH`.
- Los periodos sin datos se devuelven con valor 0.
- Todas las fechas se calculan en UTC.

---

## Seguridad y comunicación entre servicios

### Autenticación

| Tipo                | Mecanismo                                                                                                                         |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| Peticiones externas | `Authorization: Bearer <token>`. Cada servicio valida la firma HS256 con `JWT_SECRET` y lee los claims `sub`, `username` y `role` |
| Llamadas internas   | Cabecera `X-Internal-Key` con el valor de `INTERNAL_API_KEY`. Se trata como un MANAGER. Se usa también en tareas programadas      |
| Endpoints públicos  | `POST /api/auth/register`, `POST /api/auth/login`, `GET /health`, `/actuator/health` y Swagger UI                                 |

El gateway elimina `X-Internal-Key` de toda petición externa para que ningún cliente pueda hacerse pasar por un servicio
interno.

### Llamadas entre servicios

Todas se hacen con **OpenFeign** y un interceptor (`InternalFeignConfig`) que añade `X-Internal-Key`.

| Consumidor       | Proveedor                                                            | Propósito                                              |
|------------------|----------------------------------------------------------------------|--------------------------------------------------------|
| `ms-routes`      | `ms-vehicles`, `ms-drivers`                                          | Validar asignaciones y actualizar estado y odómetro    |
| `ms-maintenance` | `ms-vehicles`                                                        | Leer odómetro y marcar el vehículo `IN_MAINTENANCE`    |
| `ms-fuel`        | `ms-vehicles`                                                        | Validar vehículo, combustible y capacidad del depósito |
| `ms-alerts`      | `ms-maintenance`, `ms-fuel`, `ms-drivers`, `ms-vehicles`             | Evaluar reglas y completar mensajes con la placa       |
| `ms-dashboard`   | `ms-vehicles`, `ms-routes`, `ms-fuel`, `ms-maintenance`, `ms-alerts` | Agregar resúmenes y estadísticas                       |

---

### Resiliencia en llamadas entre servicios (Feign + Resilience4j)

| Aspecto           | Configuración                                                                                                                              |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| Timeouts          | 2 s de conexión y 5 s de lectura en todos los clientes                                                                                     |
| Reintentos        | Un reintento, solo en peticiones `GET`                                                                                                     |
| Circuit breaker   | Uno por cliente. Se abre con 50 % de fallos en las últimas 10 llamadas y espera 30 s antes de probar de nuevo                              |
| Dependencia caída | `503 SERVICE_UNAVAILABLE` con el campo `service`. `ms-dashboard` y `ms-alerts` continúan con los datos disponibles (`partial` / `skipped`) |

---

## Health check

El gateway expone el estado del sistema:

```
GET http://localhost:8080/health
```

Consulta el `/actuator/health` de cada servicio con un timeout de 2 segundos y marca como `DOWN` los que no responden.
Siempre devuelve `200`. Cada microservicio expone también su propio `/actuator/health`.

---

## Contrato de errores

Todos los servicios devuelven los errores con la misma estructura:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "La petición contiene campos no válidos",
  "details": [
    {
      "field": "year",
      "reason": "debe estar entre 1990 y 2027"
    }
  ],
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`details` solo aparece en `VALIDATION_ERROR`. En `SERVICE_UNAVAILABLE` se añade el campo `service`.

| HTTP | Código                              | Cuándo                                     |
|------|-------------------------------------|--------------------------------------------|
| 400  | `VALIDATION_ERROR`                  | Algún campo no cumple las validaciones     |
| 401  | `UNAUTHORIZED`, `TOKEN_EXPIRED`     | Falta el token, no es válido o ha caducado |
| 403  | `FORBIDDEN`                         | El rol no tiene permiso                    |
| 404  | `*_NOT_FOUND`                       | El recurso no existe                       |
| 409  | Códigos de negocio de cada servicio | Conflicto con el estado actual             |
| 429  | `RATE_LIMIT_EXCEEDED`               | Se supera el límite del gateway            |
| 503  | `SERVICE_UNAVAILABLE`               | Una dependencia no responde                |

---

## Equipo

| Integrante       | GitHub                                                   |
|------------------|----------------------------------------------------------|
| Beckan Geronimo  | [@Beckan](https://github.com/sb-geronimo)                |
| Alberto Cruz     | [@alberto-cruz-mtz](https://github.com/alberto-cruz-mtz) |
| {{Integrante 3}} | [@{{usuario}}](https://github.com/{{usuario}})           |

<br>

---

<div align="center">

![EQUIPO-03](https://img.shields.io/badge/Bytes_Colaborativos-Equipo_03-4A90D9?style=for-the-badge)

</div>