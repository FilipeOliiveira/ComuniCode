# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar com Maven + JDK 25 ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Baixa as dependencias primeiro para aproveitar o cache de camadas
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
# Os testes usam H2 e rodam fora do container (mvnw test); aqui so empacota.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/*.jar /app/app.jar

# ---------- Etapa 2: executar com JRE 25 ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl apenas para o healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 spring

COPY --from=build /app/app.jar app.jar
USER spring

EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fs http://127.0.0.1:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
