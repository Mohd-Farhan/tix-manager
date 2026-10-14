# ==============================================================================
# TixManager — Multi-Agent Enterprise Customer Support Platform
# Production Multi-Stage Dockerfile (Spring Boot 3.x + Java 21)
# ==============================================================================
#
# WHY MULTI-STAGE BUILD?
# In traditional single-stage builds, the final Docker image contains the entire JDK,
# Maven build tools, source code, and cached dependencies (often > 800 MB).
#
# A Multi-Stage build splits the process into two distinct stages:
#   Stage 1 (builder): Downloads Maven and JDK to compile code and produce the JAR.
#   Stage 2 (runtime): Copies ONLY the compiled JAR into a minimal JRE image (~150 MB).
# This gives you a smaller attack surface, faster downloads, and hardened security.
# ==============================================================================

# ------------------------------------------------------------------------------
# STAGE 1: Builder
# ------------------------------------------------------------------------------
# Uses a full JDK 21 and Maven on lightweight Alpine Linux to compile the application.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# Set the working directory inside the builder container.
WORKDIR /build

# OPTIMIZATION: Layer Caching for Maven Dependencies.
# Docker caches layers based on file checksums. By copying ONLY pom.xml first,
# Docker will reuse the downloaded dependencies layer unless pom.xml changes.
# If you edit your Java code, Docker skips re-downloading all dependencies!
COPY pom.xml .

# Download dependencies in offline mode (cached in this layer).
RUN mvn dependency:go-offline -B

# Now copy the actual application source code.
COPY src ./src

# Compile and package the Spring Boot executable JAR file.
# -DskipTests skips running unit/integration tests during image build to speed up packaging.
# (Tests are run in CI/CD pipeline before building production container images).
RUN mvn clean package -DskipTests

# ------------------------------------------------------------------------------
# STAGE 2: Runtime Environment
# ------------------------------------------------------------------------------
# Uses a lightweight Eclipse Temurin JRE 21 on Alpine Linux (~180 MB).
# We do NOT need the full JDK or Maven to run an already-compiled JAR.
FROM eclipse-temurin:21-jre-alpine AS runtime

# Set metadata labels for container identification and maintenance.
LABEL maintainer="TixManager Team"
LABEL description="TixManager Enterprise Backend Service"
LABEL version="1.0.0"

# Set application home directory inside the container.
WORKDIR /app

# SECURITY: Principle of Least Privilege.
# Running containers as 'root' is a major security vulnerability.
# Here we create a dedicated non-privileged system user and group ('tixuser').
RUN addgroup -S tixgroup && adduser -S tixuser -G tixgroup

# Create directory for persistent file attachments/uploads and grant permissions to 'tixuser'.
RUN mkdir -p /app/uploads/attachments && chown -R tixuser:tixgroup /app

# Copy the packaged Spring Boot executable JAR from Stage 1 (builder).
# We rename it to 'app.jar' for consistent execution regardless of project version name.
COPY --from=builder --chown=tixuser:tixgroup /build/target/*.jar app.jar

# Switch from root to our non-root user. All subsequent commands run as 'tixuser'.
USER tixuser

# Expose the default Spring Boot HTTP port to allow container traffic.
EXPOSE 8080

# Configure production-ready JVM memory and garbage collection options:
# - XX:+UseContainerSupport: Detects container memory/CPU limits accurately.
# - XX:MaxRAMPercentage=75.0: Allocates up to 75% of container RAM to the JVM heap.
# - Djava.security.egd: Uses non-blocking random number source for faster startup.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# CONTAINER HEALTH CHECK:
# Periodically queries Spring Boot Actuator's health endpoint.
# If the backend is hanging or crashing, Docker/Kubernetes/Render marks it 'unhealthy'.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Launch the Spring Boot application using exec form (receives OS SIGTERM for graceful shutdown).
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
