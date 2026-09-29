# Proyecto
<!--
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-{{VERSION_BOOT}}-6DB33F?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-{{VERSION_CLOUD}}-6DB33F?logo=spring&logoColor=white)
![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Container-Docker-2496ED?logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL_{{VERSION_PG}}-4169E1?logo=postgresql&logoColor=white)
-->

**Equipo:** Equipo 03  
**Arquitectura:** -
<!--
**Recurso Externo:** Nombre - Url
-->

---

## Descripción
Descripción proyecto

---

<!--
## Microservicios

| Servicio       | Puerto | Base de datos | Responsabilidad                                        |
|----------------|--------|---------------|--------------------------------------------------------|
| `ms-gateway`   | 8080   |               | Punto de entrada único                                 |
| `ms-auth`      | 8081   | PostgreSQL    | Registro, login y emisión de tokens JWT                |
| `ms-`          |        |               |                                                        |
| `ms-`          |        |               |                                                        |
| `ms-`          |        |               |                                                        |

---

## Arquitectura

```
Cliente
  │
  ▼
ms-gateway (:8080) ──── Redis (rate limiting)
  │  JWT validation + header propagation (X-User-Id, X-User-Role)
  │  Circuit Breaker per route (Resilience4j)
  │
  ├── ms-auth (:8081)
  ├── ms-{{a}} (:{{p}})
  ├── ms-{{b}} (:{{p}})
  └── ms-{{c}} (:{{p}})
```

---

## Estado de implementación

| Servicio       | Estado         | Detalle                                                    |
|----------------|----------------|------------------------------------------------------------|
|       -        |      -         |     -     |

> Leyenda: ⏳ Pendiente, 🔧 Parcial, ✅ Completo

---

## Stack tecnológico

| Capa          | Tecnología                                    |
|---------------|-----------------------------------------------|
| Lenguaje      | Java 17                                       |
| Framework     | Spring Boot {{VERSION_BOOT}}                  |
| Cloud         | Spring Cloud {{VERSION_CLOUD}}                |
| Gateway       | Spring Cloud Gateway (WebFlux reactivo)       |
| Seguridad     | Spring Security + JJWT {{VERSION_JJWT}} (HS256) |
| Resiliencia   | Resilience4j (Circuit Breaker + Time Limiter) |
| Rate Limiting | Redis {{VERSION_REDIS}} + RedisRateLimiter    |
| HTTP client   | OpenFeign                                     |
| Caché         | Caffeine                                      |
| Persistencia  | Spring Data JPA + PostgreSQL {{VERSION_PG}}   |
| Mapeo         | MapStruct {{VERSION_MAPSTRUCT}}               |
| Build         | Maven {{VERSION_MAVEN}} + Maven Wrapper       |
| Contenedores  | Docker (multi-stage, Alpine) + Docker Compose |

---

## Variables de entorno

Copiar `.env.example` a `.env` y completar los valores antes de levantar los servicios:

```bash
cp .env.example .env
```

| Variable                     | Descripción                                                   |
|------------------------------|---------------------------------------------------------------|
| `API_KEY`                    | Clave de acceso a {{API_EXTERNA}}                             |
| `JWT_SECRET`                 | Clave secreta Base64 para firmar JWT (compartida entre todos) |
| `JWT_EXPIRATION`             | Expiración del token en milisegundos (ej. `3600000` = 1h)     |
| `POSTGRES_{{SERVICIO}}_DB`       | Nombre de la BD de {{servicio}}                           |
| `POSTGRES_{{SERVICIO}}_USER`     | Usuario de la BD de {{servicio}}                          |
| `POSTGRES_{{SERVICIO}}_PASSWORD` | Contraseña de la BD de {{servicio}}                       |

> **Generar JWT_SECRET:** `openssl rand -base64 32` - puede usarse git bash.

---

## Puesta en marcha

### Requisitos previos

- Docker y Docker Compose instalados
- Archivo `.env` configurado (ver sección anterior)

### Levantar todos los servicios

```bash
docker-compose up -d --build
```

### Levantar servicios individuales

```bash
# Solo gateway + auth
docker-compose up ms-gateway ms-auth postgres-auth redis

# Gateway + servicios de datos
docker-compose up ms-gateway ms-auth ms-{{a}} ms-{{b}} postgres-auth redis
```

### Parar y limpiar

```bash
docker compose down        # parar contenedores
docker compose down -v     # parar y eliminar volúmenes (borra datos de BD)
```

---

## Endpoints disponibles

El gateway escucha en `http://localhost:8080`. Todas las rutas protegidas (`🔒`) requieren el header
`Authorization: Bearer <token>`.

### Autenticación (públicas)

| Método | Ruta                 | Descripción               |
|--------|----------------------|---------------------------|
| POST   | `/api/auth/register` | Registro de nuevo usuario |
| POST   | `/api/auth/login`    | Login → devuelve JWT      |

---

### {{Recurso A}} `🔒`

| Método | Ruta                  | Params                | Descripción            |
|--------|-----------------------|-----------------------|------------------------|
| GET    | `/api/{{a}}`          | `{{param}}?`          | Lista (filtrable)      |
| GET    | `/api/{{a}}/{{{id}}}` | —                     | Detalle                |

---

### {{Recurso B}} `🔒`

| Método | Ruta                  | Params                | Descripción            |
|--------|-----------------------|-----------------------|------------------------|
| GET    | `/api/{{b}}`          | `{{param}}` (req.)    | {{Descripción}}        |
| POST   | `/api/{{b}}`          | Body: `{{campos}}`    | {{Descripción}}        |

{{Notas sobre valores permitidos, defaults o comportamiento especial.}}

---

## Comunicación entre servicios

Todos los llamados inter-servicio se realizan mediante OpenFeign con `FeignAuthInterceptor`, que propaga el JWT del
contexto de seguridad.

| Consumidor   | Proveedor                | Propósito                        |
|--------------|--------------------------|----------------------------------|
| `ms-{{x}}`   | `ms-{{y}}` (via `{{Client}}`) | {{Para qué se comunican}}   |

---

## Gateway: resiliencia y rate limiting

- **Rate limiting:** {{60}} requests/minuto por IP usando `RedisRateLimiter`
- **Circuit Breaker** (Resilience4j) por ruta:
    - Ventana deslizante: {{10}} llamadas
    - Apertura al {{50}}% de fallos (mínimo {{3}} llamadas)
    - Recuperación automática tras {{10}} segundos
    - Timeout por petición: {{5}} segundos
- **Fallback:** respuesta JSON estructurada con código `SERVICE_UNAVAILABLE` cuando el circuito está abierto

---

## Health check

El gateway expone endpoint de salud de los microservicios:

```
GET http://localhost:8080/health
```

Cada microservicio expone su propio `/api/actuator/health`.

---
-->
## Equipo

| Integrante     | GitHub                                           |
|----------------|--------------------------------------------------|
| Beckan         | [@Beckan](https://github.com/sb-geronimo)        |

<br>

---

<div align="center">

![EQUIPO-03](https://img.shields.io/badge/Bytes_Colaborativos-Equipo_03-4A90D9?style=for-the-badge)

</div>
