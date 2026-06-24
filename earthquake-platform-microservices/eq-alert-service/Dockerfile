# Multi-stage build from the platform root (context must be the repo root).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY eq-events eq-events
COPY eq-alert-service eq-alert-service
RUN mvn -q -pl eq-alert-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/eq-alert-service/target/*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java","-jar","app.jar"]
