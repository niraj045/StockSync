# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw \
    && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy AS runtime
ENV TZ=Asia/Kolkata \
    JAVA_TOOL_OPTIONS="-Xms256m -Xmx1024m"

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 stocksync \
    && useradd --system --uid 10001 --gid stocksync --home-dir /app --shell /usr/sbin/nologin stocksync \
    && install -d -o stocksync -g stocksync /app /data /data/uploads /data/documents /data/reports

WORKDIR /app
COPY --from=build --chown=stocksync:stocksync /workspace/target/*.jar /app/app.jar

USER stocksync
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
