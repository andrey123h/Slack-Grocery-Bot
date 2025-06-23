# Multi-stage Dockerfile for Slack Grocery Bot

#--- Build Stage --------------------------------------------------------------
FROM maven:3.9.4-eclipse-temurin-21-alpine AS build

# Set working directory inside the container
WORKDIR /app

# Copy only the pom.xml first to leverage layer caching for dependencies
COPY pom.xml .

# Download dependencies for offline build
RUN mvn dependency:go-offline -B

# Copy the rest of the source code
COPY src ./src

# Package the application, skipping tests for speed (CI can run tests separately)
RUN mvn clean package -DskipTests


#--- Runtime Stage ------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Create a non-root user for better security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/slack-grocery-bot-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot app listens on
EXPOSE 8080

# Switch to non-root user
USER appuser

# Default command to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
