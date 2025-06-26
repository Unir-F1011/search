
FROM maven:3.9.6-eclipse-temurin-24 AS build

COPY . .

RUN mvn clean package


FROM eclipse-temurin:24-jdk

EXPOSE 8081

COPY --from=build /target/search-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
