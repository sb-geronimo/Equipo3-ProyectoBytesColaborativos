<!-- Título: [US-XX][ms-servicio] Descripción corta · Ej: [US-06][ms-vehicles] Alta y consulta de vehículos -->

## Qué hace este PR
<!-- 1-3 líneas -->

## Historia y servicio
- **US:** US-XX
- **Servicio(s):** <!-- ms-auth, ms-gateway, ms-vehicles, ms-drivers, ms-routes, ms-maintenance, ms-fuel, ms-alerts, ms-dashboard, docker -->
- Closes #

## Cómo probarlo
<!-- Endpoint + ejemplo de petición (vía gateway: http://localhost:8080/api/...) o el test que lo cubre -->

## Checklist (Definición de Done)
- [ ] Cumple los criterios de aceptación de la US
- [ ] Tests unitarios de las reglas nuevas pasan (`./mvnw test` en el servicio)
- [ ] Endpoints documentados en Swagger con ejemplos
- [ ] Funciona con `docker compose up --build` desde cero
- [ ] Sin secretos en el código (usa `.env`; actualicé `.env.example` si agregué variables)
- [ ] Errores con el formato común (`error`, `message`, `timestamp`)
- [ ] Pedí revisión a al menos 1 compañero

## Notas para el revisor
<!-- Opcional: dudas, decisiones, dependencias con otros PR -->
