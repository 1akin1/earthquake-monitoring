# detection-service (the monolith) image.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -q -DskipTests clean package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/earthquake-monitoring-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
