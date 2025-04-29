# Use OpenJDK 17
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and project files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Give permission to Gradle wrapper
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew bootJar

# Run the application
ENTRYPOINT ["java", "-jar", "build/libs/drowsiness-detection-0.0.1-SNAPSHOT.jar"]