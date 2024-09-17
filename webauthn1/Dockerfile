# Build Stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /home/app
COPY src ./src
COPY pom.xml .
RUN mvn clean package -DskipTests
# Package stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/app/target/*.jar webAuthn.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","webAuthn.jar"]