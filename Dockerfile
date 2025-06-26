# Etapa 1: Build con Maven y JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build

COPY . .

RUN mvn clean package

# Etapa 2: Imagen runtime con OpenJDK 21 JRE
FROM openjdk:21

EXPOSE 8081

COPY --from=build /target/search-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
