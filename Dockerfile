# =============================================================================
# Multi-stage Dockerfile for Spring Boot Application
# =============================================================================
# Stage 1: Build the application
# Stage 2: Create a minimal runtime image
# =============================================================================

# -----------------------------------------------------------------------------
# STAGE 1: Builder
# -----------------------------------------------------------------------------
# We use Eclipse Temurin (formerly AdoptOpenJDK) - the community's go-to JDK
# The 21-jdk image includes the full JDK needed to compile Java code
FROM eclipse-temurin:21-jdk AS builder

# Set working directory inside the container
WORKDIR /app

# Copy Maven wrapper files first (these change less frequently)
# Docker caches layers, so unchanged layers don't need rebuilding
COPY mvnw .
COPY .mvn .mvn

# Copy pom.xml separately to cache dependencies
# If pom.xml hasn't changed, Docker reuses the cached dependency layer
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
# -B = batch mode (non-interactive)
# -DskipTests = don't run tests during dependency resolution
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Now copy source code (this layer changes most frequently)
COPY src src

# Build the application
# -DskipTests = we'll run tests separately, not during Docker build
RUN ./mvnw package -DskipTests

# -----------------------------------------------------------------------------
# STAGE 2: Runtime
# -----------------------------------------------------------------------------
# Use JRE (not JDK) for runtime - smaller image, no compiler needed
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a non-root user for security
# Running as root in containers is a security risk
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Copy the JAR from the builder stage
# The --from=builder flag references our first stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to our non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Document the port this container listens on
# This is documentation only - you still need to publish the port when running
EXPOSE 8080

# Health check - Docker will monitor if the app is healthy
# Spring Boot Actuator would be even better, but this works for now
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
# Using exec form (array) is preferred over shell form
# JAVA_OPTS allows runtime configuration of JVM settings
ENTRYPOINT ["java", "-jar", "app.jar"]
