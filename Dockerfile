# Multi-Stage Production Dockerfile for Invoicely
# Stage 1: Build & Package
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Cache Maven wrapper and dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and compile jar
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Extract layered Spring Boot jar for optimized container caching
WORKDIR /workspace/target
RUN java -Djarmode=layertools -jar invoicely-0.0.1-SNAPSHOT.jar extract

# Stage 2: Minimal Distroless / Hardened Alpine Runtime
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Create non-root system group and user
RUN addgroup -S invoicely && adduser -S invoicely -G invoicely
USER invoicely:invoicely

# Copy layers from builder stage
COPY --from=builder --chown=invoicely:invoicely /workspace/target/dependencies/ ./
COPY --from=builder --chown=invoicely:invoicely /workspace/target/spring-boot-loader/ ./
COPY --from=builder --chown=invoicely:invoicely /workspace/target/snapshot-dependencies/ ./
COPY --from=builder --chown=invoicely:invoicely /workspace/target/application/ ./

# Expose HTTP port
EXPOSE 8080

# Configure JVM memory, GC, and container awareness
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Container health probe
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
