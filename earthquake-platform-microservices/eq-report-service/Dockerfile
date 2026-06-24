FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY eq-events eq-events
COPY eq-report-service eq-report-service
RUN mvn -q -pl eq-report-service -am -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/eq-report-service/target/*.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java","-jar","app.jar"]
