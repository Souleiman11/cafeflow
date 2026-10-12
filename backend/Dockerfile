# syntax=docker/dockerfile:1
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# BuildKit cache mount keeps ~/.m2 between builds. Tests run in CI, not in the image build.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
    && mkdir -p target/extracted \
    && java -Djarmode=tools -jar target/cafeflow-backend-*.jar extract --layers --destination target/extracted \
    && mv target/extracted/application/cafeflow-backend-*.jar target/extracted/application/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root runtime user
RUN addgroup -S -g 10001 cafeflow && adduser -S -u 10001 -G cafeflow cafeflow
USER 10001

COPY --from=build --chown=10001:10001 /build/target/extracted/dependencies/ ./
COPY --from=build --chown=10001:10001 /build/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=10001:10001 /build/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /build/target/extracted/application/ ./

EXPOSE 8080

# Container-aware memory limits are the default on Java 21.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
  CMD wget -q -O - http://127.0.0.1:8080/actuator/health/readiness | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
