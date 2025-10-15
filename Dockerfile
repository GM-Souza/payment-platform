# Build
FROM maven:3.9.3-eclipse-temurin-20 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run
FROM eclipse-temurin:20-jre
WORKDIR /app
COPY --from=build /app/target/AgiPayMailService-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/certs ./certs  # certificações SSL do Aiven

ENV KAFKA_BOOTSTRAP_SERVERS=kafka-28113646-fabricio36365-1991.c.aivencloud.com:10610
ENV SPRING_KAFKA_SSL_KEYSTORE_PASSWORD=changeit
ENV SPRING_KAFKA_SSL_TRUSTSTORE_PASSWORD=changeit
ENV SPRING_KAFKA_SSL_KEY_PASSWORD=changeit

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]