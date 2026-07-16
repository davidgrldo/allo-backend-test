# Multi-stage build for Split Bill API
# Builds with Maven on temurin:21 JDK, runs on temurin:21 JRE, exposed on port 4110.

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer cache for dependencies)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 4110

ENTRYPOINT ["java", "-jar", "app.jar"]