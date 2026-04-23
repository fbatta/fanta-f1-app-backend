# Stage 1: Build
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy gradle wrapper and config files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Grant execution rights on the gradlew script
RUN chmod +x gradlew

# Download dependencies (caching)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the fat JAR
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create a non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy the JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Dspring.profiles.active=production", "-jar", "app.jar"]
