# ---- Build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew build --no-daemon -x test

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-alpine
LABEL authors="jfessler"
RUN addgroup -S docker && adduser -S docker -G docker

COPY --from=build --chown=docker:docker /workspace/build/libs/*.jar app.jar
USER docker:docker
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
