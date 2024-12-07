FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/API-1.0-SNAPSHOT.jar /app/api.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/api.jar"]

