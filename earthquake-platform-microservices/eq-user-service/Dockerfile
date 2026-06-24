FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY eq-events eq-events
COPY eq-user-service eq-user-service
RUN mvn -q -pl eq-user-service -am -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/eq-user-service/target/*.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java","-jar","app.jar"]
