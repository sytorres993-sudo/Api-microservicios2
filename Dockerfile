FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY productos-service/pom.xml productos-service/pom.xml
COPY clientes-service/pom.xml clientes-service/pom.xml
COPY pedidos-service/pom.xml pedidos-service/pom.xml
COPY common/src common/src
COPY productos-service/src productos-service/src
COPY clientes-service/src clientes-service/src
COPY pedidos-service/src pedidos-service/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp verify

FROM build AS artifact
ARG SERVICE
RUN mkdir /output && cp "${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar" /output/app.jar

FROM eclipse-temurin:17-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/data \
    && chown -R app:app /app
WORKDIR /app
COPY --from=artifact --chown=app:app /output/app.jar app.jar
USER app
ENV PORT=8081
EXPOSE 8081 8082 8083
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 \
    CMD curl --fail --silent --show-error "http://localhost:${PORT}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
