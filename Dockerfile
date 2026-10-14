# syntax=docker/dockerfile:1
# =============================================================================
# GaiaOps ESG API - imagem Docker
#
# Build em dois estagios (multi-stage):
#   1. "build"   - imagem com Maven + JDK 21: compila e empacota o .jar
#   2. "runtime" - imagem final so com o JRE 21 (Alpine): roda o .jar
#
# Assim a imagem publicada nao carrega Maven, JDK, codigo-fonte nem o
# repositorio local de dependencias - fica menor e com menos superficie de
# ataque. Os testes NAO rodam aqui: eles ja rodaram na etapa de testes do
# pipeline, antes de a imagem ser construida.
# =============================================================================

# -----------------------------------------------------------------------------
# Estagio 1: build
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Primeiro so o pom.xml: o download das dependencias vira uma camada propria,
# reaproveitada do cache enquanto o pom nao mudar. Alterar so o codigo-fonte
# nao baixa tudo de novo.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests

# -----------------------------------------------------------------------------
# Estagio 2: runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.title="GaiaOps ESG API" \
      org.opencontainers.image.description="API de gestao ESG para Cidades Inteligentes"

# Usuario sem privilegios (UID fixo 10001): a aplicacao nao roda como root.
RUN addgroup -S -g 10001 gaiaops && adduser -S -u 10001 -G gaiaops gaiaops

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/gaiaops-esg.jar app.jar

USER 10001:10001
EXPOSE 8080

# A JVM le JAVA_TOOL_OPTIONS automaticamente: respeita o limite de memoria do
# container (75% para o heap) e encerra em caso de OutOfMemoryError, para o
# Docker reinicia-la limpa. Pode ser sobrescrita no docker-compose.yml.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# "healthy" so quando a aplicacao esta pronta E o MongoDB responde
# (grupo readiness do Actuator). O Compose usa esse estado para o --wait.
# O "|| exit 1" normaliza o codigo de erro do wget para o que o Docker espera.
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
  CMD ["sh", "-c", "wget -q -O /dev/null http://localhost:8080/actuator/health/readiness || exit 1"]

# Forma JSON (exec): a JVM e o PID 1 e recebe o SIGTERM do "docker stop"
# diretamente, permitindo o encerramento gracioso configurado no Spring.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
