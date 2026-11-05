# ---------- Etapa 1: compilación ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Se copian primero las dependencias para aprovechar la caché de capas
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=postgres \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
