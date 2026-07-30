# syntax=docker/dockerfile:1

# Stage 1: build the JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src src

RUN mvn -q -DskipTests package

# Stage 2: run the JAR
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/inventory-reporting-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
