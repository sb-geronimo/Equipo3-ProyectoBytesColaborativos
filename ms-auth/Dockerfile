# syntax=docker/dockerfile:1

# ---- Etapa 1: build del jar con Maven ----
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -Dmaven.test.skip=true

# ---- Etapa 2: extracción de capas (jarmode tools, Spring Boot 4.1+) ----
FROM eclipse-temurin:17-jre-alpine AS layers
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---- Etapa 3: imagen final, mínima y sin privilegios ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=layers /app/extracted/dependencies/ ./
COPY --from=layers /app/extracted/spring-boot-loader/ ./
COPY --from=layers /app/extracted/snapshot-dependencies/ ./
COPY --from=layers /app/extracted/application/ ./
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]