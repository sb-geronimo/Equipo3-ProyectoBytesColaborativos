# ms-dashboard

Servicio agregador de la flota. Expone tres endpoints que combinan los datos de
los demás microservicios (`ms-vehicles`, `ms-routes`, `ms-fuel`,
`ms-maintenance`, `ms-alerts`) en respuestas pensadas para una capa de
visualización.

## Endpoints

| Método | Ruta                                            | Descripción                                                                                                        |
| ------ | ----------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| GET    | `/api/dashboard?days=N`                         | Resumen ejecutivo: flota, KPIs operativos, snapshot de alertas y próximas órdenes de mantenimiento.                |
| GET    | `/api/dashboard/timeseries?metrics=&from=&to=&granularity=` | Series temporales por métrica, en el rango y con la granularidad solicitada.                                       |
| GET    | `/api/dashboard/vehicles/ranking?metric=&days=&limit=&type=` | Ranking de vehículos del periodo (1..365 días), filtrable por tipo y limitado a un máximo de 20 entradas.        |

> Los endpoints están documentados con OpenAPI en `/swagger-ui.html` cuando el
> servicio está levantado.

## Métricas soportadas en `/api/dashboard/timeseries`

| Métrica             | Origen        | Unidad     | Notas                                     |
| ------------------- | ------------- | ---------- | ----------------------------------------- |
| `DISTANCE_KM`       | ms-routes     | `km`       | Distancia recorrida en cada periodo.      |
| `FUEL_LITERS`       | ms-fuel       | `L`        | Litros repostados en cada periodo.        |
| `FUEL_COST`         | ms-fuel       | `EUR`      | Coste total en cada periodo.              |
| `CONSUMPTION_L100KM`| ms-fuel       | `L/100km`  | Consumo medio en cada periodo.            |
| `MAINTENANCE_COST`  | ms-maintenance| `EUR`      | Coste total en cada periodo.              |
| `ALERTS_CREATED`    | ms-alerts     | `alerts`   | Alertas creadas en cada periodo.          |

## Métricas de ranking en `/api/dashboard/vehicles/ranking`

| Métrica       | Unidad     | Descripción                                           |
| ------------- | ---------- | ----------------------------------------------------- |
| `FUEL_COST`   | `EUR`      | Coste total de combustible del periodo.                |
| `CONSUMPTION` | `L/100km`  | Consumo medio de combustible del periodo.             |

## Validación

Todos los endpoints validan sus parámetros de entrada. Cualquier valor fuera de
rango o valor desconocido devuelve `400 VALIDATION_ERROR` con el detalle del
campo:

- `days`: 1..365 (resumen y ranking).
- `limit`: 1..20 (ranking).
- `metrics`: cada valor debe estar en `DashboardMetric` (Spring ya filtra).
- `from`/`to`: rango máximo de 366 días; `to` debe ser ≥ `from` (timeseries).
- `granularity`: `DAY`, `WEEK` o `MONTH` (timeseries).

## Degradación elegante

Las llamadas hacia los servicios origen se hacen en paralelo con
`CompletableFuture`. Si una llamada falla, el bloque afectado se devuelve
`null` y el nombre del servicio (`ms-vehicles`, `ms-routes`, `ms-fuel`,
`ms-maintenance`, `ms-alerts`) se añade al array `unavailable`; cuando hay
algún servicio caído, el flag `partial` se pone a `true`.

## Caché

Cada endpoint tiene su propia caché Caffeine con un TTL configurable (por
defecto 60 s) a través de la propiedad `dashboard.cache.ttl-seconds`. La
clave se construye a partir del endpoint y los parámetros de la query.

## Variables de entorno

| Variable                       | Descripción                                | Valor por defecto                          |
| ------------------------------ | ------------------------------------------ | ------------------------------------------ |
| `JWT_SECRET`                   | Secreto usado por `JwtAuthFilter`.         | (obligatorio)                              |
| `INTERNAL_API_KEY`             | Clave que ms-dashboard envía a los demás.  | (obligatorio)                              |
| `VEHICLES_SERVICE_URL`         | URL base de `ms-vehicles`.                 | `http://localhost:8082`                    |
| `ROUTES_SERVICE_URL`           | URL base de `ms-routes`.                   | `http://localhost:8085`                    |
| `FUEL_SERVICE_URL`             | URL base de `ms-fuel`.                     | `http://localhost:8087`                    |
| `MAINTENANCE_SERVICE_URL`      | URL base de `ms-maintenance`.              | `http://localhost:8086`                    |
| `ALERTS_SERVICE_URL`           | URL base de `ms-alerts`.                   | `http://localhost:8088`                    |
| `DASHBOARD_CACHE_TTL_SECONDS`  | TTL de las cachés del dashboard.            | `60`                                       |
