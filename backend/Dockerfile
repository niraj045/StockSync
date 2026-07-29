FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
RUN groupadd --system stocksync \
    && useradd --system --gid stocksync --home-dir /app stocksync \
    && mkdir -p /app/storage \
    && chown -R stocksync:stocksync /app
COPY --from=build --chown=stocksync:stocksync /workspace/target/*.jar app.jar
USER stocksync
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
