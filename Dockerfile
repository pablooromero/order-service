# Etapa 1: Construcción del JAR con Maven
FROM maven:3.8.8-eclipse-temurin-17 AS build

# Crear un directorio de trabajo
WORKDIR /app

# Copiar los archivos pom.xml y descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar el resto del código fuente
COPY src ./src

# Construir el proyecto y generar el archivo JAR
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de ejecución
FROM openjdk:17-jdk-slim

# Crear un directorio para la aplicación
WORKDIR /app

# Copiar el JAR generado desde la etapa de construcción
COPY --from=build /app/target/order-service-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto
EXPOSE 8083

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
