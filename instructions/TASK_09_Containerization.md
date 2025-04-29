# TASK_09_Containerization.txt

# Task Title
Finalize Containerization: Dockerfile and Docker-Compose

---

# Goal
Prepare a fully working Docker setup so that the entire application can be built and run with one simple command, ensuring proper access to camera devices and providing data persistence.

---

# Why This Task Is Important
- Ensures consistent environment across development and production
- Simplifies deployment and demonstration
- Helps avoid "works on my machine" problems
- Enables reliable access to webcam for face detection
- Ensures database persistence between restarts

---

# Prerequisites
Before starting this task:
- Complete all other critical tasks (TASK_01 through TASK_07)
- Review `CODING_STANDARDS.txt`
- Understand basic Docker concepts

---

# Detailed Instructions

## Step 1: Create Enhanced Dockerfile
- File: `Dockerfile`

```dockerfile
# What is this file?
# Dockerfile for building and running the Driver Monitoring System.
# Why is this needed?
# It defines the container environment with proper dependencies for AI and webcam access.

# Use Java 17 base image with proper GPU support
FROM eclipse-temurin:17-jdk-alpine AS build

# Set working directory
WORKDIR /app

# Install necessary tools for building
RUN apk add --no-cache maven git

# Copy project files
COPY . .

# Build the application (skip tests to speed up build)
RUN ./gradlew clean bootJar --no-daemon -x test

# Create smaller runtime image
FROM eclipse-temurin:17-jre-alpine

# Install dependencies for OpenCV and webcam access
RUN apk add --no-cache \
    libstdc++ \
    libc6-compat \
    libx11 \
    libxext \
    libxrender \
    libxtst \
    libxi \
    libxrandr \
    libxfixes \
    libxcursor \
    v4l-utils \
    && mkdir -p /app/data /app/logs

# Set working directory
WORKDIR /app

# Set environment variables
ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/driver-monitoring-db;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE
ENV JAVA_OPTS="-Xms256m -Xmx1g -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

# Copy application from build stage
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Create volumes for data persistence
VOLUME ["/app/data", "/app/logs"]

# Expose the port
EXPOSE 8080

# Set health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application with memory limits
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

## Step 2: Create Comprehensive docker-compose.yml
- File: `docker-compose.yml`

```yaml
# What is this file?
# Docker Compose configuration for the Driver Monitoring System.
# Why is this needed?
# It orchestrates the application container with proper device access and volume mounts.

version: '3.8'

services:
  driver-monitoring-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: driver-monitoring
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - TZ=Asia/Almaty # Set timezone for Kazakhstan
      - JAVA_OPTS=-Xms256m -Xmx1g -XX:+ExitOnOutOfMemoryError
      # Add option to use mock camera if needed
      - USE_MOCK_CAMERA=${USE_MOCK_CAMERA:-false}
    volumes:
      - ./data:/app/data     # Persist H2 database
      - ./logs:/app/logs     # Store application logs
      - ./models:/app/models # Store AI models
    # Camera device mapping with options for different setups
    devices:
      - ${CAMERA_DEVICE:-/dev/video0}:${CAMERA_DEVICE:-/dev/video0}
    restart: unless-stopped
    # Proper resource limits
    deploy:
      resources:
        limits:
          cpus: '1.5'
          memory: 1.5G
        reservations:
          cpus: '0.5'
          memory: 512M
```

## Step 3: Create Docker Profile Application Properties
- File: `src/main/resources/application-docker.properties`

```properties
# Server settings
server.port=8080

# H2 Database settings - file-based for persistence
spring.datasource.url=jdbc:h2:file:/app/data/driver-monitoring-db;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

# JPA settings
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Logging configuration
logging.file.path=/app/logs
logging.file.name=/app/logs/driver-monitoring.log
logging.level.root=INFO
logging.level.com.driver_monitoring=DEBUG
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Resources configuration
spring.web.resources.static-locations=classpath:/static/,file:/app/models/

# Actuator for health checking
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always

# Camera settings
app.camera.use-mock=${USE_MOCK_CAMERA:false}
app.camera.device-index=0
app.camera.fallback-device-index=1
```

## Step 4: Create a Script to Configure Camera Environment Variables
- File: `.env`

```
# Environment variables for Docker Compose
# Change these values according to your system setup

# Set to 'true' to use mock camera generator instead of real webcam
USE_MOCK_CAMERA=false

# Path to your webcam device - change if needed
# Common values:
# - Linux: /dev/video0 or /dev/video1
# - macOS: this won't work directly as Docker for Mac doesn't support device passthrough
# - Windows: this works through WSL2 if your camera is recognized
CAMERA_DEVICE=/dev/video0
```

## Step 5: Create a Script to Build and Run
- File: `run.sh`

```bash
#!/bin/bash

# What is this file?
# Script to build and run the Driver Monitoring System.
# Why is this needed?
# It simplifies the startup process and ensures everything is set up correctly.

# Make sure models directory exists
mkdir -p models/face_detection
mkdir -p models/landmarks

# Define model URLs
SSD_CAFFEMODEL_URL="https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000.caffemodel"
SSD_PROTOTXT_URL="https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/deploy.prototxt"
DLIB_LANDMARKS_URL="http://dlib.net/files/shape_predictor_68_face_landmarks.dat.bz2"

# Check if model files exist, download if not
if [ ! -f "models/face_detection/deploy.prototxt" ]; then
  echo "Downloading face detection model files..."
  if ! wget -q -O models/face_detection/deploy.prototxt "$SSD_PROTOTXT_URL"; then
    echo "Failed to download deploy.prototxt, creating placeholder..."
    echo "# This is a placeholder file. Replace with actual deploy.prototxt" > models/face_detection/deploy.prototxt
  fi
  
  if ! wget -q -O models/face_detection/res10_300x300_ssd_iter_140000.caffemodel "$SSD_CAFFEMODEL_URL"; then
    echo "Failed to download caffemodel, creating placeholder..."
    # Create empty file as placeholder
    touch models/face_detection/res10_300x300_ssd_iter_140000.caffemodel
  fi
fi

if [ ! -f "models/landmarks/shape_predictor_68_face_landmarks.dat" ]; then
  echo "Downloading facial landmarks model..."
  if ! wget -q -O models/landmarks/shape_predictor_68_face_landmarks.dat.bz2 "$DLIB_LANDMARKS_URL"; then
    echo "Failed to download landmarks model, creating placeholder..."
    # Create empty file as placeholder
    touch models/landmarks/shape_predictor_68_face_landmarks.dat
  else
    echo "Extracting landmarks model..."
    bzip2 -d models/landmarks/shape_predictor_68_face_landmarks.dat.bz2
  fi
fi

# Create data and logs directories
mkdir -p data
mkdir -p logs

# Check for Docker and Docker Compose
if ! command -v docker &> /dev/null; then
  echo "Docker is not installed. Please install Docker first."
  exit 1
fi

# Check for different docker-compose commands (could be docker-compose or docker compose)
DOCKER_COMPOSE_CMD="docker-compose"
if ! command -v docker-compose &> /dev/null; then
  if ! docker compose version &> /dev/null; then
    echo "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
  else
    DOCKER_COMPOSE_CMD="docker compose"
  fi
fi

# Detect webcam device
detect_webcam() {
  local webcam_device=""
  
  # Linux
  if [ -e "/dev/video0" ]; then
    webcam_device="/dev/video0"
  elif [ -e "/dev/video1" ]; then
    webcam_device="/dev/video1"
  else
    # Try to list video devices
    if command -v v4l2-ctl &> /dev/null; then
      echo "Available video devices:"
      v4l2-ctl --list-devices
    fi
  fi
  
  # If no webcam detected, suggest mock camera
  if [ -z "$webcam_device" ]; then
    echo "No webcam detected. Setting USE_MOCK_CAMERA=true in .env file."
    sed -i 's/USE_MOCK_CAMERA=false/USE_MOCK_CAMERA=true/' .env
  else
    echo "Detected webcam at: $webcam_device"
    echo "Setting CAMERA_DEVICE=$webcam_device in .env file."
    sed -i "s|CAMERA_DEVICE=.*|CAMERA_DEVICE=$webcam_device|" .env
  fi
}

# Detect and set webcam
detect_webcam

# Start services
echo "Building and starting services..."
$DOCKER_COMPOSE_CMD up --build -d

# Check if the service started successfully
echo "Waiting for service to start..."
sleep 10

# Check if the container is running
if docker ps -q -f name=driver-monitoring; then
  echo "Driver Monitoring System is running!"
  echo "You can access it at http://localhost:8080"
  echo "To view logs, run: $DOCKER_COMPOSE_CMD logs -f"
  echo "To stop, run: $DOCKER_COMPOSE_CMD down"
else
  echo "Failed to start Driver Monitoring System. Check logs with: $DOCKER_COMPOSE_CMD logs"
fi
```

## Step 6: Make the script executable
```bash
chmod +x run.sh
```

## Step 7: Create a Camera Access Test Script
- File: `test-camera.sh`

```bash
#!/bin/bash

# What is this file?
# Script to test camera access from inside a Docker container.
# Why is this needed?
# It helps verify that the container has proper access to the webcam device.

echo "Testing camera access in Docker container..."

# Default device
DEVICE="/dev/video0"

# If specified, use device from argument
if [ $# -eq 1 ]; then
  DEVICE="$1"
fi

# First, check if the device exists on the host
if [ ! -e "$DEVICE" ]; then
  echo "❌ Error: Device $DEVICE does not exist on the host."
  echo "Available video devices on the host:"
  
  if command -v v4l2-ctl &> /dev/null; then
    v4l2-ctl --list-devices
  elif command -v ls &> /dev/null; then
    ls -la /dev/video*
  fi
  
  echo "Try again with a valid device: ./test-camera.sh /dev/videoX"
  exit 1
fi

# Check device permissions
if [ ! -r "$DEVICE" ] || [ ! -w "$DEVICE" ]; then
  echo "⚠️ Warning: Insufficient permissions for $DEVICE"
  echo "Current permissions:"
  ls -la $DEVICE
  echo ""
  echo "Try fixing permissions with: sudo chmod 666 $DEVICE"
fi

# Create a simple test container
echo "Creating test container to check camera access..."
docker run --rm \
  --device=$DEVICE:$DEVICE \
  alpine:latest \
  sh -c "apk add --no-cache v4l-utils && v4l2-ctl --list-devices"

if [ $? -eq 0 ]; then
  echo "✅ Camera device access test successful!"
  echo "Device $DEVICE can be accessed from inside a container."
else
  echo "❌ Camera device access test failed!"
  echo "Please try the following troubleshooting steps:"
  echo "1. Set proper permissions: sudo chmod 666 $DEVICE"
  echo "2. Try a different device (e.g., /dev/video1): ./test-camera.sh /dev/video1"
  echo "3. If no camera is available, edit .env and set USE_MOCK_CAMERA=true"
fi

# Check if mock camera option is needed
echo ""
echo "If your camera cannot be accessed, you can use a simulated camera by:"
echo "1. Edit the .env file"
echo "2. Set USE_MOCK_CAMERA=true"
echo "3. Run ./run.sh again"
```

## Step 8: Create a Docker Configuration Guide
- File: `docker-guide.md`

```markdown
# Docker Configuration Guide for Driver Monitoring System

This guide explains how to set up and troubleshoot Docker for the Driver Monitoring System, particularly focusing on camera access which can be tricky.

## Basic Usage

The simplest way to start the application is:

```bash
./run.sh
```

This script will:
1. Create necessary directories
2. Download AI models if needed
3. Detect your webcam device
4. Configure environment variables
5. Build and start the Docker container

## Camera Configuration Options

### Option 1: Use a Physical Webcam

1. Make sure your webcam is properly connected
2. Verify it appears as a device (usually `/dev/video0` on Linux)
3. Set proper permissions:
   ```bash
   sudo chmod 666 /dev/video0
   ```
4. Edit `.env` file to specify your webcam:
   ```
   USE_MOCK_CAMERA=false
   CAMERA_DEVICE=/dev/video0
   ```

### Option 2: Use Mock Camera

If you don't have a webcam or can't access it from Docker:

1. Edit `.env` file:
   ```
   USE_MOCK_CAMERA=true
   CAMERA_DEVICE=/dev/video0  # This value doesn't matter when using mock camera
   ```
2. Run the application normally with `./run.sh`

## Troubleshooting Camera Access

### Test Camera Access

Run the test script to verify Docker can access your camera:

```bash
./test-camera.sh
```

or test a specific device:

```bash
./test-camera.sh /dev/video1
```

### Common Issues and Solutions

#### 1. Permission Denied

```
Error: cannot open device /dev/video0: Permission denied
```

**Solution**: Update device permissions
```bash
sudo chmod 666 /dev/video0
```

#### 2. Device Not Found

```
Error: No such file or directory
```

**Solution**: Check available devices and use the correct one
```bash
ls -la /dev/video*
# Then specify the correct device in .env
```

#### 3. macOS Camera Access

Docker on macOS doesn't support direct device passthrough.

**Solution**: Use mock camera option
```
USE_MOCK_CAMERA=true
```

#### 4. Windows / WSL2 Camera Access

For Windows running Docker through WSL2:

1. Make sure your camera is recognized in Windows
2. Enable camera sharing in WSL2 configuration
3. If still not working, use mock camera option

## Managing the Application

- **View logs**: `docker-compose logs -f`
- **Stop application**: `docker-compose down`
- **Reset data**: `rm -rf ./data/*`
- **Access web interface**: Open `http://localhost:8080` in your browser

## Using Docker Without Scripts

If you prefer to manage Docker manually:

```bash
# Build and start
docker-compose up --build -d

# View logs
docker-compose logs -f

# Stop containers
docker-compose down
```

## Advanced Configuration

For advanced configurations, you can modify:

- `Dockerfile` - Container setup
- `docker-compose.yml` - Service orchestration
- `application-docker.properties` - Spring Boot configuration
- `.env` - Environment variables
```

## Step 9: Update Camera Detection in Application

Create a configuration class to handle the mock camera option:

- Package: `com.driver_monitoring.config`
- File: `CameraConfig.java`

```java
// What is this file?
// Configuration for camera access with fallback to mock camera.
// Why is this needed?
// It provides a way to run the system even without a webcam.

package com.driver_monitoring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CameraConfig {

    private static final Logger logger = LoggerFactory.getLogger(CameraConfig.class);

    @Value("${app.camera.use-mock:false}")
    private boolean useMockCamera;
    
    @Value("${app.camera.device-index:0}")
    private int deviceIndex;
    
    @Value("${app.camera.fallback-device-index:1}")
    private int fallbackDeviceIndex;
    
    public boolean isUseMockCamera() {
        return useMockCamera;
    }
    
    public int getDeviceIndex() {
        return deviceIndex;
    }
    
    public int getFallbackDeviceIndex() {
        return fallbackDeviceIndex;
    }
    
    public void logCameraConfig() {
        logger.info("Camera configuration:");
        logger.info(" - Using mock camera: {}", useMockCamera);
        if (!useMockCamera) {
            logger.info(" - Primary device index: {}", deviceIndex);
            logger.info(" - Fallback device index: {}", fallbackDeviceIndex);
        }
    }
}
```

Then update the WebCamService to use this configuration:

```java
@Autowired
private CameraConfig cameraConfig;

public boolean startCamera() {
    if (isRunning.get()) {
        logger.info("Camera is already running");
        return true;
    }
    
    // Log camera configuration
    cameraConfig.logCameraConfig();
    
    // Check if mock camera should be used
    if (cameraConfig.isUseMockCamera()) {
        logger.info("Using mock camera generator");
        startMockCamera();
        return true;
    }
    
    try {
        // Try primary device index
        videoCapture = new VideoCapture(cameraConfig.getDeviceIndex());
        
        if (!videoCapture.isOpened()) {
            logger.warn("Failed to open camera at index {}. Trying fallback index {}.", 
                      cameraConfig.getDeviceIndex(), cameraConfig.getFallbackDeviceIndex());
            
            videoCapture = new VideoCapture(cameraConfig.getFallbackDeviceIndex());
            
            if (!videoCapture.isOpened()) {
                logger.error("Failed to open camera with fallback index too. Falling back to mock camera.");
                startMockCamera();
                return true;
            }
        }
        
        // Set resolution
        videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
        videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
        
        isRunning.set(true);
        
        // Start processing in a separate thread
        executor.scheduleAtFixedRate(this::processFrame, 0, 100, TimeUnit.MILLISECONDS);
        
        logger.info("Real camera started successfully");
        return true;
    } catch (Exception e) {
        logger.error("Error starting camera: {}", e.getMessage(), e);
        logger.info("Falling back to mock camera");
        startMockCamera();
        return true;
    }
}

private void startMockCamera() {
    isRunning.set(true);
    logger.info("Starting mock camera simulation");
    executor.scheduleAtFixedRate(this::processMockFrame, 0, 100, TimeUnit.MILLISECONDS);
}

private void processMockFrame() {
    try {
        // Generate a mock frame
        Mat mockFrame = MockFrameGenerator.generateMockFrame();
        
        // Get driverId from session
        String driverId = getCurrentDriverId();
        if (driverId == null || driverId.isEmpty()) {
            logger.warn("No driver ID found in session");
            return;
        }
        
        // Process mock frame with AI
        DriverState state = faceDetectionService.analyzeFrame(mockFrame, driverId);
        
        // Only update UI if state changed
        if (state != currentState) {
            currentState = state;
            updateUI(state);
        }
        
        // Release resources
        if (mockFrame != null && !mockFrame.empty()) {
            mockFrame.release();
        }
    } catch (Exception e) {
        logger.error("Error processing mock frame: {}", e.getMessage(), e);
    }
}
```

---

# Preventing Common Docker Issues

## Camera Access Problems
- **Device permissions**: Ensure the host's webcam has proper permissions (`chmod 666 /dev/video0`)
- **Device mapping**: Map the device correctly in `docker-compose.yml` with `devices` section
- **Test camera access**: Use the provided test script to verify camera accessibility
- **Mock camera fallback**: Enable mock camera option when real camera cannot be accessed

## Memory Management
- **Set proper Java heap limits**: Use `-Xms` and `-Xmx` flags
- **Add memory monitoring**: Include heap dumps on OOM errors
- **Set container resource limits**: Use `deploy.resources.limits` in compose file

## Data Persistence
- **Mount volumes** for database, logs, and models
- **Set proper database URL** to use the mounted volume
- **Use AUTO_SERVER=TRUE** for file-based H2 database to allow multiple connections

## Image Optimization
- **Use multi-stage builds** to create smaller runtime images
- **Install only necessary dependencies** in the runtime image
- **Use Alpine-based images** to minimize container size

## Container Health and Reliability
- **Add healthcheck** to monitor container status
- **Configure proper restart policy** in case of failures
- **Set environment variables** based on container context

---

# Platform-Specific Issues

## Linux
- Camera devices are usually at `/dev/video0`, `/dev/video1`, etc.
- Device permissions must be set: `chmod 666 /dev/video0`
- Check available devices with: `v4l2-ctl --list-devices`

## macOS
- Docker for Mac does not support device passthrough (use mock camera)
- If needed, explore third-party solutions like:
  - Using VirtualBox instead of Docker Desktop
  - Streaming camera over network to container

## Windows with WSL2
- Make sure camera is enabled in WSL configuration
- May need to install Windows USB device support for WSL
- Test camera availability in WSL environment before Docker

---

# Troubleshooting Tips
1. **Camera Not Found**:
   - Check if the camera is accessible on the host: `ls -la /dev/video*`
   - Try setting device permissions: `sudo chmod 666 /dev/video0`
   - Run the test-camera.sh script to check container access
   - If all else fails, enable mock camera option in .env

2. **Out of Memory Errors**:
   - Increase container memory limits in docker-compose.yml
   - Adjust Java heap settings in JAVA_OPTS environment variable
   - Scale down image resolution in application settings

3. **Slow Performance**:
   - Check container CPU usage: `docker stats`
   - Consider enabling GPU access for AI processing
   - Ensure adequate CPU allocation in docker-compose.yml

4. **Database Issues**:
   - Check if data volume is properly mounted: `docker-compose exec driver-monitoring ls -la /app/data`
   - Verify database connection settings in application-docker.properties
   - Check application logs for database errors: `docker-compose logs driver-monitoring`

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Add a comment at the top of Dockerfile and docker-compose.yml explaining "What is this and why"
- Clear, clean, minimal Docker syntax
- Proper organization of Docker stages and layers

---

# Success Criteria
- `./run.sh` successfully builds and runs the project
- Application is accessible on localhost:8080
- Webcam access works inside the container (or falls back to mock camera)
- Database data persists between container restarts
- Container startup is reliable and includes proper health checks
- Resources are properly constrained to ensure stability
- Deployment is simple enough for demo purposes

---

# References
- [Dockerfile Reference](https://docs.docker.com/engine/reference/builder/)
- [Docker Compose Basics](https://docs.docker.com/compose/)
- [Docker Device Access](https://docs.docker.com/engine/reference/commandline/run/#add-host-device-to-container---device)
- [Managing Container Resources](https://docs.docker.com/config/containers/resource_constraints/)

---

# End of TASK_09_Containerization.txt