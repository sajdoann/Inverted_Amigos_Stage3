# Usa una imagen base de OpenJDK 17
FROM openjdk:17-jdk-slim

# Crea un directorio dentro del contenedor para tu aplicación
WORKDIR /app

# Copia tu archivo JAR al directorio de trabajo en el contenedor
COPY out/artifacts/Inverted_Amigos_Stage3_jar/Inverted_Amigos_Stage3.jar /app/Inverted_Amigos_Stage3.jar

# Define el comando que se ejecutará cuando se inicie el contenedor
ENTRYPOINT ["java", "-Xms4g", "-Xmx8g", "-jar", "Inverted_Amigos_Stage3.jar"]