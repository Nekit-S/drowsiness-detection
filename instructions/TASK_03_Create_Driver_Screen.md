# TASK_03_Create_Driver_Screen.txt

# Task Title
Create Driver Screen: Login, Start Session, Show Video and Notifications

---

# Goal
Develop the front-end screen for the driver:
- Allow entering the driver's name and ID.
- Create a new driving session when driver starts monitoring.
- Start the face and eye detection after login.
- Display video stream with detection overlay.
- Show simple notifications when driver is distracted or drowsy.
- End driving session when driver exits monitoring.

---

# Why This Task Is Important
- This is the main entry point for the system.
- Proper session management is crucial for data integrity.
- Real-time feedback to the driver is essential for safety.

---

# Prerequisites
Before starting this task:
- Complete `TASK_02_Create_Entities_and_Repositories.txt`.
- Review `CODING_STANDARDS.txt`.

---

# Detailed Instructions

## Step 1: Create a Session Service
- Package: `com.driver_monitoring.service`
- File: `SessionService.java`

```java
// What is this file?
// Service interface for managing driver sessions.
// Why is this needed?
// It provides methods for starting and ending driving sessions.

package com.driver_monitoring.service;

import com.driver_monitoring.model.DriverSession;

public interface SessionService {
    
    /**
     * Start a new driving session for a driver
     * @param driverId The ID of the driver
     * @return The created DriverSession
     */
    DriverSession startSession(String driverId);
    
    /**
     * End an active driving session
     * @param driverId The ID of the driver
     * @return The ended DriverSession
     */
    DriverSession endSession(String driverId);
    
    /**
     * Get the current active session for a driver
     * @param driverId The ID of the driver
     * @return The active DriverSession or null if no active session
     */
    DriverSession getActiveSession(String driverId);
}
```

## Step 2: Implement Session Service
- File: `SessionServiceImpl.java`

```java
// What is this file?
// Implementation of the session service.
// Why is this needed?
// It handles the logic for creating, tracking, and ending driver sessions.

package com.driver_monitoring.service;

import com.driver_monitoring.model.DriverSession;
import com.driver_monitoring.repository.DriverSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class SessionServiceImpl implements SessionService {

    @Autowired
    private DriverSessionRepository sessionRepository;
    
    @Override
    public DriverSession startSession(String driverId) {
        // Check if there's already an active session
        Optional<DriverSession> existingSession = sessionRepository.findByDriverIdAndActiveTrue(driverId);
        
        if (existingSession.isPresent()) {
            // End the existing session first
            DriverSession session = existingSession.get();
            session.endSession();
            sessionRepository.save(session);
        }
        
        // Create a new session
        DriverSession newSession = new DriverSession(driverId);
        return sessionRepository.save(newSession);
    }
    
    @Override
    public DriverSession endSession(String driverId) {
        Optional<DriverSession> existingSession = sessionRepository.findByDriverIdAndActiveTrue(driverId);
        
        if (existingSession.isPresent()) {
            DriverSession session = existingSession.get();
            session.endSession();
            return sessionRepository.save(session);
        }
        
        return null; // No active session found
    }
    
    @Override
    public DriverSession getActiveSession(String driverId) {
        Optional<DriverSession> session = sessionRepository.findByDriverIdAndActiveTrue(driverId);
        return session.orElse(null);
    }
}
```

## Step 3: Create Login Page (Driver Login Form)
- File: `src/main/resources/templates/driver_login.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Driver Login</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css">
    <style>
        .login-form {
            max-width: 400px;
            margin: 0 auto;
            padding: 20px;
        }
    </style>
</head>
<body>
    <div class="container mt-5">
        <div class="card login-form">
            <div class="card-header bg-primary text-white">
                <h3 class="mb-0">Driver Login</h3>
            </div>
            <div class="card-body">
                <form th:action="@{/driver/start}" method="post">
                    <div class="mb-3">
                        <label for="driverName" class="form-label">Your Name</label>
                        <input type="text" class="form-control" id="driverName" name="driverName" required>
                    </div>
                    <div class="mb-3">
                        <label for="driverId" class="form-label">Driver ID (6 digits)</label>
                        <input type="text" class="form-control" id="driverId" name="driverId" 
                               pattern="[0-9]{6}" maxlength="6" required>
                        <div class="form-text">Enter your 6-digit driver ID</div>
                    </div>
                    <button type="submit" class="btn btn-primary w-100">Start Monitoring</button>
                </form>
            </div>
            <div class="card-footer text-muted">
                Driver Monitoring System
            </div>
        </div>
    </div>
</body>
</html>
```

## Step 4: Create Driver Monitoring Page
- File: `src/main/resources/templates/driver_monitoring.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Driver Monitoring</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css">
    <style>
        .video-container {
            position: relative;
            width: 640px;
            height: 480px;
            margin: 0 auto;
            border: 1px solid #ddd;
            border-radius: 5px;
            overflow: hidden;
        }
        #notificationArea {
            position: absolute;
            top: 10px;
            left: 10px;
            right: 10px;
            z-index: 1000;
        }
        .status-indicator {
            position: absolute;
            bottom: 10px;
            left: 10px;
            right: 10px;
            padding: 5px;
            border-radius: 5px;
            text-align: center;
            font-weight: bold;
        }
        .session-info {
            margin: 20px auto;
            max-width: 640px;
        }
    </style>
</head>
<body>
    <div class="container mt-3">
        <div class="row mb-3">
            <div class="col">
                <h2>Driver Monitoring</h2>
                <h4 th:text="${'Driver: ' + driverName + ' (ID: ' + driverId + ')'}"></h4>
                <input type="hidden" id="driverId" th:value="${driverId}">
                <input type="hidden" id="sessionId" th:value="${sessionId}">
            </div>
        </div>
        
        <div class="row">
            <div class="col">
                <div class="video-container">
                    <video id="videoElement" width="640" height="480" autoplay></video>
                    <div id="notificationArea"></div>
                    <div id="statusIndicator" class="status-indicator bg-success text-white">
                        Status: Normal
                    </div>
                </div>
                
                <div class="session-info">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Session Information</h5>
                            <p>Session Started: <span id="sessionStart" th:text="${sessionStart}"></span></p>
                            <p>Session Duration: <span id="sessionDuration">00:00:00</span></p>
                            <div class="d-flex justify-content-between">
                                <button id="pauseButton" class="btn btn-warning">Pause</button>
                                <form th:action="@{/driver/exit}" method="post">
                                    <input type="hidden" name="driverId" th:value="${driverId}">
                                    <button type="submit" class="btn btn-danger">End Session</button>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <script>
        let video = document.getElementById('videoElement');
        let pauseButton = document.getElementById('pauseButton');
        let isPaused = false;
        let startTime = new Date();
        let durationDisplay = document.getElementById('sessionDuration');
        
        // Get camera stream
        async function startCamera() {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({ video: true });
                video.srcObject = stream;
            } catch (err) {
                console.error("Error accessing camera: ", err);
                showNormalNotification("Camera access error. Please check permissions.");
            }
        }
        
        // Update session duration
        function updateDuration() {
            if (!isPaused) {
                const now = new Date();
                const diff = now - startTime;
                const hours = Math.floor(diff / 3600000).toString().padStart(2, '0');
                const minutes = Math.floor((diff % 3600000) / 60000).toString().padStart(2, '0');
                const seconds = Math.floor((diff % 60000) / 1000).toString().padStart(2, '0');
                durationDisplay.textContent = `${hours}:${minutes}:${seconds}`;
            }
            setTimeout(updateDuration, 1000);
        }
        
        // Notifications
        function showNormalNotification() {
            document.getElementById('notificationArea').innerHTML = `
                <div class="alert alert-success" role="alert">
                    Status: Normal
                </div>`;
            document.getElementById('statusIndicator').className = 'status-indicator bg-success text-white';
            document.getElementById('statusIndicator').textContent = 'Status: Normal';
        }
        
        function showDistractedNotification() {
            document.getElementById('notificationArea').innerHTML = `
                <div class="alert alert-warning" role="alert">
                    Warning: Distracted!
                </div>`;
            document.getElementById('statusIndicator').className = 'status-indicator bg-warning text-dark';
            document.getElementById('statusIndicator').textContent = 'Status: Distracted';
        }
        
        function showDrowsyNotification() {
            document.getElementById('notificationArea').innerHTML = `
                <div class="alert alert-danger" role="alert">
                    Warning: Drowsy!
                </div>`;
            document.getElementById('statusIndicator').className = 'status-indicator bg-danger text-white';
            document.getElementById('statusIndicator').textContent = 'Status: Drowsy';
        }
        
        // Pause/Resume button
        pauseButton.addEventListener('click', function() {
            isPaused = !isPaused;
            if (isPaused) {
                pauseButton.textContent = 'Resume';
                pauseButton.className = 'btn btn-success';
                if (video.srcObject) {
                    const tracks = video.srcObject.getTracks();
                    tracks.forEach(track => track.stop());
                }
            } else {
                pauseButton.textContent = 'Pause';
                pauseButton.className = 'btn btn-warning';
                startCamera();
            }
        });
        
        // Initialize
        document.addEventListener('DOMContentLoaded', function() {
            startCamera();
            updateDuration();
        });
    </script>
</body>
</html>
```

## Step 5: Create Driver Controller
- Package: `com.driver_monitoring.controller`
- File: `DriverController.java`

```java
// What is this file?
// This controller handles the Driver screen: login, session management, and monitoring.
// Why is this needed?
// It manages the frontend views and connects driver actions to the backend services.

package com.driver_monitoring.controller;

import com.driver_monitoring.model.Driver;
import com.driver_monitoring.model.DriverSession;
import com.driver_monitoring.repository.DriverRepository;
import com.driver_monitoring.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import java.time.format.DateTimeFormatter;

@Controller
public class DriverController {

    @Autowired
    private DriverRepository driverRepository;
    
    @Autowired
    private SessionService sessionService;
    
    @GetMapping("/driver/login")
    public String driverLogin() {
        return "driver_login";
    }
    
    @PostMapping("/driver/start")
    public String startMonitoring(@RequestParam String driverName,
                                   @RequestParam String driverId,
                                   HttpSession httpSession,
                                   Model model) {
        // Save driver information if it doesn't exist
        if (!driverRepository.existsById(driverId)) {
            Driver driver = new Driver(driverId, driverName);
            driverRepository.save(driver);
        }
        
        // Start a new driving session
        DriverSession driverSession = sessionService.startSession(driverId);
        
        // Store information in HTTP session
        httpSession.setAttribute("driverName", driverName);
        httpSession.setAttribute("driverId", driverId);
        httpSession.setAttribute("sessionId", driverSession.getSessionId());
        
        // Add attributes for the view
        model.addAttribute("driverName", driverName);
        model.addAttribute("driverId", driverId);
        model.addAttribute("sessionId", driverSession.getSessionId());
        model.addAttribute("sessionStart", 
                           driverSession.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return "driver_monitoring";
    }
    
    @PostMapping("/driver/exit")
    public String exitSession(@RequestParam String driverId, HttpSession httpSession) {
        // End the driving session
        sessionService.endSession(driverId);
        
        // Invalidate HTTP session
        httpSession.invalidate();
        
        return "redirect:/driver/login";
    }
    
    @GetMapping("/")
    public String home() {
        return "redirect:/driver/login";
    }
}
```

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Simple and clear controllers
- Proper comments at the top of each file
- Proper HTML structure (Bootstrap classes)

---

# Success Criteria
- Login page loads and accepts Name + 6-digit ID
- New driving session is created in the database when driver logs in
- After login, video stream starts automatically
- Notifications are shown on driver state change
- End Session button correctly ends the driving session and redirects to login
- Session duration is displayed and updated in real-time
- Code is simple, clean, and properly commented

---

# References
- [Spring Boot MVC Basics](https://spring.io/guides/gs/serving-web-content/)
- [MDN: MediaDevices.getUserMedia()](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia)
- [Bootstrap Components](https://getbootstrap.com/docs/5.0/components/alerts/)

---

# End of TASK_03_Create_Driver_Screen.txt
