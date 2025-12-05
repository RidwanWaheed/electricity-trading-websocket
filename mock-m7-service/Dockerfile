# =============================================================================
# Multi-stage Dockerfile for Mock M7 Service
# =============================================================================

# -----------------------------------------------------------------------------
# STAGE 1: Builder
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and parent pom
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy common module (required dependency)
COPY common/pom.xml common/
COPY common/src common/src

# Copy mock-m7-service module
COPY mock-m7-service/pom.xml mock-m7-service/
COPY mock-m7-service/src mock-m7-service/src

# Build common module first, then mock-m7-service
RUN chmod +x mvnw && ./mvnw package -DskipTests -pl common,mock-m7-service -am

# -----------------------------------------------------------------------------
# STAGE 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user for security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Copy the JAR from builder stage
COPY --from=builder /app/mock-m7-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8082

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
