# TASK_01_Setup_Backend.txt

# Task Title
Setup Spring Boot Backend and Containerize the Project

---

# Goal
Prepare a minimal working Spring Boot backend project and Dockerize it.
This will be the base to build the Driver Monitoring System.

---

# Why This Task Is Important
Without a correctly working backend and containerization, the entire project will be unstable.
This step ensures:
- Standardized environment (Docker)
- Smooth further development
- Easy deployment and demonstration

---

# Prerequisites
Before starting this task:
- Review `CODING_STANDARDS.txt`.
- Understand basic Spring Boot project structure.

---

# Detailed Instructions

## Step 1: Create Spring Boot Project
- Create a Gradle-based Spring Boot project.
- Use Java 17.
- Add the following dependencies:
  - Spring Web
  - Spring Data JPA
  - H2 Database
  - Lombok
  - Thymeleaf
- Create a main application class named `DriverMonitoringApplication.java`.
- Create a REST controller class `HomeController.java` that serves a simple "Hello World" at `/`.

**Structure:**
```bash
src/main/java/com/driver_monitoring/
|-- DriverMonitoringApplication.java
|-- controller/
    |-- HomeController.java
```

**Example of HomeController.java:**
```java
// What is this file?
// This class serves a basic HTTP endpoint to verify that the backend is running.
// Why is this needed?
// It is needed to confirm that the project builds and runs correctly before adding more complex logic.

package com.driver_monitoring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Hello World from Driver Monitoring System";
    }
}
```

## Step 2: Create Basic application.properties
Under `src/main/resources/application.properties`, add:
```properties
server.port=8080
spring.h2.console.enabled=true
```

## Step 3: Create Dockerfile
At the project root, create a file named `Dockerfile`:

```dockerfile
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

# Build the project
RUN ./gradlew bootJar

# Run the application
ENTRYPOINT ["java", "-jar", "build/libs/driver-monitoring-0.0.1-SNAPSHOT.jar"]
```

## Step 4: Create docker-compose.yml
Also in project root, create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  driver-monitoring-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
```

## Step 5: Build and Run Project
From the project root, run:
```bash
docker-compose up --build
```

Verify:
- Application builds without errors.
- You can open `http://localhost:8080/` and see "Hello World from Driver Monitoring System".

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Simple, clear code
- Proper comments on classes and methods
- Consistent formatting

---

# Success Criteria
- Spring Boot application starts successfully.
- Docker container runs and exposes port 8080.
- "Hello World" page is accessible.
- Code and project structure follow `CODING_STANDARDS.txt`.

---

# References
- [Spring Boot Getting Started Guide](https://spring.io/guides/gs/spring-boot/)
- [Dockerfile reference](https://docs.docker.com/engine/reference/builder/)
- [docker-compose.yml reference](https://docs.docker.com/compose/compose-file/)

---

# End of TASK_01_Setup_Backend.txt

