FROM maven:3.9.7-eclipse-temurin-17 AS build
WORKDIR /app
COPY apps/stream-aggregator/pom.xml ./pom.xml
COPY apps/stream-aggregator/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/stream-aggregator-1.0.0-shaded.jar /app/stream-aggregator.jar
CMD ["java", "-jar", "/app/stream-aggregator.jar"]
