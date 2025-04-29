# TASK_04_Create_Dispatcher_Screen.txt

# Task Title
Create Dispatcher Screen: View Drivers, Sessions, and Events

---

# Goal
Develop a web-based Dispatcher Panel that:
- Lists all registered drivers
- Shows active and past driving sessions
- Displays events and statistics for each session
- Provides insights based on collected event data

---

# Why This Task Is Important
- Dispatchers need a centralized place to monitor driver behavior
- Session-based organization provides context for events
- Historical data analysis helps identify patterns and risky drivers

---

# Prerequisites
Before starting this task:
- Complete `TASK_03_Create_Driver_Screen.txt`.
- Review `CODING_STANDARDS.txt`.

---

# Detailed Instructions

## Step 1: Create Dispatcher Main Page
- File: `src/main/resources/templates/dispatcher_panel.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Dispatcher Panel</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css">
    <style>
        .active-status {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            display: inline-block;
            margin-right: 5px;
        }
        .active-status.active {
            background-color: #28a745;
        }
        .active-status.inactive {
            background-color: #dc3545;
        }
    </style>
</head>
<body>
    <div class="container mt-4">
        <h1>Dispatcher Panel</h1>
        <div class="row mt-4">
            <div class="col-md-4">
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0">Drivers</h5>
                    </div>
                    <div class="card-body">
                        <div class="list-group">
                            <div th:each="driver : ${drivers}" class="list-group-item list-group-item-action">
                                <div class="d-flex w-100 justify-content-between">
                                    <h5 class="mb-1">
                                        <span th:class="${driver.hasActiveSession ? 'active-status active' : 'active-status inactive'}"></span>
                                        <span th:text="${driver.driverName}"></span>
                                    </h5>
                                    <small th:text="${'ID: ' + driver.driverId}"></small>
                                </div>
                                <p class="mb-1" th:if="${driver.hasActiveSession}">
                                    <strong>Active since:</strong> <span th:text="${driver.activeSessionStartTime}"></span>
                                </p>
                                <p class="mb-1" th:unless="${driver.hasActiveSession}">
                                    <strong>Last active:</strong> <span th:text="${driver.lastSessionEndTime ?: 'Never'}"></span>
                                </p>
                                <div class="d-flex justify-content-end">
                                    <a th:href="@{'/dispatcher/driver/' + ${driver.driverId}}" class="btn btn-sm btn-primary">View Details</a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="col-md-8">
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0">Active Sessions</h5>
                    </div>
                    <div class="card-body">
                        <div th:if="${activeSessions.isEmpty()}">
                            <p>No active sessions at the moment.</p>
                        </div>
                        <div th:unless="${activeSessions.isEmpty()}" class="table-responsive">
                            <table class="table table-striped">
                                <thead>
                                    <tr>
                                        <th>Driver</th>
                                        <th>Start Time</th>
                                        <th>Duration</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr th:each="session : ${activeSessions}">
                                        <td th:text="${session.driverName}"></td>
                                        <td th:text="${session.startTime}"></td>
                                        <td th:text="${session.duration}"></td>
                                        <td>
                                            <a th:href="@{'/dispatcher/session/' + ${session.sessionId}}" class="btn btn-sm btn-info">View</a>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

## Step 2: Create Driver Detail Page
- File: `src/main/resources/templates/driver_detail.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Driver Details</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <div class="container mt-4">
        <div class="d-flex justify-content-between align-items-center">
            <h1>Driver Details</h1>
            <a href="/dispatcher" class="btn btn-primary">Back to Dashboard</a>
        </div>
        
        <div class="card mt-4">
            <div class="card-header">
                <h3 th:text="${driver.driverName + ' (ID: ' + driver.driverId + ')'}"></h3>
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-md-4">
                        <div class="card">
                            <div class="card-body text-center">
                                <h5 class="card-title">Status</h5>
                                <span th:if="${activeSession != null}" class="badge bg-success fs-5">Active</span>
                                <span th:unless="${activeSession != null}" class="badge bg-secondary fs-5">Inactive</span>
                                
                                <div th:if="${activeSession != null}" class="mt-3">
                                    <p><strong>Session Started:</strong> <span th:text="${activeSession.startTime}"></span></p>
                                    <p><strong>Duration:</strong> <span th:text="${activeSession.duration}"></span></p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="card mt-4">
            <div class="card-header">
                <h5>Session History</h5>
            </div>
            <div class="card-body">
                <div th:if="${sessions.isEmpty()}">
                    <p>No session history available for this driver.</p>
                </div>
                <div th:unless="${sessions.isEmpty()}" class="table-responsive">
                    <table class="table table-striped">
                        <thead>
                            <tr>
                                <th>Session ID</th>
                                <th>Start Time</th>
                                <th>End Time</th>
                                <th>Duration</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr th:each="session : ${sessions}">
                                <td th:text="${session.sessionId}"></td>
                                <td th:text="${session.startTime}"></td>
                                <td th:text="${session.endTime ?: 'Active'}"></td>
                                <td th:text="${session.duration}"></td>
                                <td>
                                    <a th:href="@{'/dispatcher/session/' + ${session.sessionId}}" class="btn btn-sm btn-info">View</a>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

## Step 3: Create Session Detail Page
- File: `src/main/resources/templates/session_detail.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Session Details</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-4">
        <div class="d-flex justify-content-between align-items-center">
            <h1>Session Details</h1>
            <div>
                <a th:href="@{'/dispatcher/driver/' + ${session.driverId}}" class="btn btn-secondary me-2">Back to Driver</a>
                <a href="/dispatcher" class="btn btn-primary">Back to Dashboard</a>
            </div>
        </div>
        
        <div class="card mt-4">
            <div class="card-header">
                <h3 th:text="${'Session #' + session.sessionId + ' - ' + session.driverName}"></h3>
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-md-6">
                        <p><strong>Start Time:</strong> <span th:text="${session.startTime}"></span></p>
                        <p><strong>End Time:</strong> <span th:text="${session.endTime ?: 'Active'}"></span></p>
                        <p><strong>Duration:</strong> <span th:text="${session.duration}"></span></p>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="card mt-4">
            <div class="card-header">
                <h5>Note</h5>
            </div>
            <div class="card-body">
                <p>Detailed event listing and analytics will be available after implementing the Event Logging Service.</p>
                <p>This simplified view shows only basic session information for now.</p>
            </div>
        </div>
    </div>
</body>
</html>
```

## Step 4: Create Data Transfer Objects (DTOs)
These DTOs will help transform database entities to view-friendly objects:

- Package: `com.driver_monitoring.dto`
- File: `DriverDTO.java`

```java
// What is this file?
// Data Transfer Object for Driver information in the dispatcher view.
// Why is this needed?
// It aggregates driver and session data for the view layer.

package com.driver_monitoring.dto;

import lombok.Data;

@Data
public class DriverDTO {
    private String driverId;
    private String driverName;
    private boolean hasActiveSession;
    private String activeSessionStartTime;
    private String lastSessionEndTime;
    private Long totalSessionCount;
}
```

- File: `SessionDTO.java`

```java
// What is this file?
// Data Transfer Object for Session information in the dispatcher view.
// Why is this needed?
// It provides formatted session data for the view layer.

package com.driver_monitoring.dto;

import lombok.Data;

@Data
public class SessionDTO {
    private Long sessionId;
    private String driverId;
    private String driverName;
    private String startTime;
    private String endTime;
    private String duration;
}
```

## Step 5: Create Dispatcher Controller
- Package: `com.driver_monitoring.controller`
- File: `DispatcherController.java`

```java
// What is this file?
// This controller handles the Dispatcher panel: dashboard, driver details, and session details.
// Why is this needed?
// It connects the database entities to the dispatcher UI and provides analytics.

package com.driver_monitoring.controller;

import com.driver_monitoring.dto.DriverDTO;
import com.driver_monitoring.dto.SessionDTO;
import com.driver_monitoring.model.Driver;
import com.driver_monitoring.model.DriverSession;
import com.driver_monitoring.repository.DriverRepository;
import com.driver_monitoring.repository.DriverSessionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DispatcherController {

    @Autowired
    private DriverRepository driverRepository;
    
    @Autowired
    private DriverSessionRepository sessionRepository;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @GetMapping("/dispatcher")
    public String dispatcherPanel(Model model) {
        // Get all drivers with session information
        List<DriverDTO> driverDTOs = getAllDriversWithInfo();
        
        // Get active sessions
        List<SessionDTO> activeSessions = getActiveSessions();
        
        model.addAttribute("drivers", driverDTOs);
        model.addAttribute("activeSessions", activeSessions);
        
        return "dispatcher_panel";
    }
    
    @GetMapping("/dispatcher/driver/{driverId}")
    public String driverDetails(@PathVariable String driverId, Model model) {
        // Get driver
        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver == null) {
            return "redirect:/dispatcher";
        }
        
        // Get active session if any
        Optional<DriverSession> activeSessionOpt = sessionRepository.findByDriverIdAndActiveTrue(driverId);
        SessionDTO activeSession = null;
        if (activeSessionOpt.isPresent()) {
            activeSession = convertToSessionDTO(activeSessionOpt.get());
        }
        
        // Get all sessions for this driver
        List<SessionDTO> sessions = sessionRepository.findByDriverId(driverId).stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());
        
        model.addAttribute("driver", driver);
        model.addAttribute("activeSession", activeSession);
        model.addAttribute("sessions", sessions);
        
        return "driver_detail";
    }
    
    @GetMapping("/dispatcher/session/{sessionId}")
    public String sessionDetails(@PathVariable Long sessionId, Model model) {
        // Get session
        DriverSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return "redirect:/dispatcher";
        }
        
        // Get driver name
        Driver driver = driverRepository.findById(session.getDriverId()).orElse(null);
        String driverName = (driver != null) ? driver.getDriverName() : "Unknown";
        
        // Get session details
        SessionDTO sessionDTO = convertToSessionDTO(session);
        sessionDTO.setDriverName(driverName);
        
        model.addAttribute("session", sessionDTO);
        
        return "session_detail";
    }
    
    // Helper methods
    
    private List<DriverDTO> getAllDriversWithInfo() {
        List<Driver> drivers = driverRepository.findAll();
        List<DriverDTO> driverDTOs = new ArrayList<>();
        
        for (Driver driver : drivers) {
            DriverDTO dto = new DriverDTO();
            dto.setDriverId(driver.getDriverId());
            dto.setDriverName(driver.getDriverName());
            
            // Check for active session
            Optional<DriverSession> activeSession = sessionRepository.findByDriverIdAndActiveTrue(driver.getDriverId());
            dto.setHasActiveSession(activeSession.isPresent());
            
            if (activeSession.isPresent()) {
                dto.setActiveSessionStartTime(activeSession.get().getStartTime().format(dateFormatter));
            } else {
                // Get last session end time
                List<DriverSession> sessions = sessionRepository.findByDriverId(driver.getDriverId());
                Optional<DriverSession> lastSession = sessions.stream()
                        .filter(s -> s.getEndTime() != null)
                        .max(Comparator.comparing(DriverSession::getEndTime));
                
                if (lastSession.isPresent()) {
                    dto.setLastSessionEndTime(lastSession.get().getEndTime().format(dateFormatter));
                }
            }
            
            // Count sessions
            List<DriverSession> sessions = sessionRepository.findByDriverId(driver.getDriverId());
            dto.setTotalSessionCount((long) sessions.size());
            
            driverDTOs.add(dto);
        }
        
        return driverDTOs;
    }
    
    private List<SessionDTO> getActiveSessions() {
        List<DriverSession> activeSessions = sessionRepository.findAll().stream()
                .filter(DriverSession::isActive)
                .collect(Collectors.toList());
        
        List<SessionDTO> sessionDTOs = new ArrayList<>();
        
        for (DriverSession session : activeSessions) {
            SessionDTO dto = convertToSessionDTO(session);
            
            // Add driver name
            Driver driver = driverRepository.findById(session.getDriverId()).orElse(null);
            if (driver != null) {
                dto.setDriverName(driver.getDriverName());
            } else {
                dto.setDriverName("Unknown");
            }
            
            sessionDTOs.add(dto);
        }
        
        return sessionDTOs;
    }
    
    private SessionDTO convertToSessionDTO(DriverSession session) {
        SessionDTO dto = new SessionDTO();
        dto.setSessionId(session.getSessionId());
        dto.setDriverId(session.getDriverId());
        dto.setStartTime(session.getStartTime().format(dateFormatter));
        
        if (session.getEndTime() != null) {
            dto.setEndTime(session.getEndTime().format(dateFormatter));
            dto.setDuration(formatDuration(session.getTotalDrivingTimeSeconds()));
        } else {
            // Calculate duration until now
            Duration duration = Duration.between(session.getStartTime(), LocalDateTime.now());
            dto.setDuration(formatDuration(duration.getSeconds()));
        }
        
        return dto;
    }
    
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
```

---

# Note About Further Enhancement
After implementing the Event Logging Service (TASK_06), you will need to enhance this dispatcher panel to:
1. Display event counts in the active sessions table
2. Show a list of recent events on the dashboard
3. Add event timeline and detailed event listing to the session detail page
4. Add event summary charts to the driver detail page

These enhancements will be described in more detail after TASK_06 is completed.

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Simple, understandable controllers
- Proper comments at the top of each file
- Clear HTML layouts with Bootstrap classes

---

# Success Criteria
- Dispatcher Panel lists all drivers with their status
- Active sessions are displayed with live duration
- Session details show basic session information
- Code is simple, clean, and properly commented

---

# References
- [Spring Boot MVC Basics](https://spring.io/guides/gs/serving-web-content/)
- [Thymeleaf Tutorial](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)
- [Bootstrap Components](https://getbootstrap.com/docs/5.0/components/modal/)

---

# End of TASK_04_Create_Dispatcher_Screen.txt