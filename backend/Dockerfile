# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Cache dependencies first (faster rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the JAR (skip tests for faster deploy)
COPY src ./src
RUN mvn package -DskipTests -B

# ─── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=builder /app/target/backend-1.0.0.jar app.jar

# Render sets PORT env var; Spring Boot reads it automatically
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
