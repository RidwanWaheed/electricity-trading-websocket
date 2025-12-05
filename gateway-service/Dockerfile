# =============================================================================
# Multi-stage Dockerfile for Gateway Service
# =============================================================================
# Stage 1: Build the application
# Stage 2: Create a minimal runtime image
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

# Copy gateway-service module
COPY gateway-service/pom.xml gateway-service/
COPY gateway-service/src gateway-service/src

# Create stub pom.xml for modules we don't need (Maven requires them to exist)
RUN mkdir -p order-service mock-m7-service && \
    echo '<project><modelVersion>4.0.0</modelVersion><parent><groupId>com.trading</groupId><artifactId>electricity-trading</artifactId><version>0.0.1-SNAPSHOT</version></parent><artifactId>order-service</artifactId></project>' > order-service/pom.xml && \
    echo '<project><modelVersion>4.0.0</modelVersion><parent><groupId>com.trading</groupId><artifactId>electricity-trading</artifactId><version>0.0.1-SNAPSHOT</version></parent><artifactId>mock-m7-service</artifactId></project>' > mock-m7-service/pom.xml

# Build common module first, then gateway-service
RUN chmod +x mvnw && ./mvnw package -DskipTests -pl common,gateway-service -am

# -----------------------------------------------------------------------------
# STAGE 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user for security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Copy the JAR from builder stage
COPY --from=builder /app/gateway-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
