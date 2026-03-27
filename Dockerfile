FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y docker.io

COPY --from=build /app/target/*.jar app.jar

COPY runner-images /app/runner-images
COPY load-runner-images.sh /app/load-runner-images.sh
RUN chmod +x /app/load-runner-images.sh

ENV BACKEND_PORT=8080
EXPOSE 8080

ENTRYPOINT ["/bin/sh", "-c", "/app/load-runner-images.sh && java -jar app.jar"]