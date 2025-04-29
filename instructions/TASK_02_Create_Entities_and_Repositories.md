# TASK_02_Create_Entities_and_Repositories.txt

# Task Title
Create Driver, DriverSession, Event Entities and DriverState Enum with JPA Repositories

---

# Goal
Design and implement database entities (Driver, DriverSession, Event), the DriverState enum, and their corresponding repository interfaces.
These will store all necessary information about drivers, their driving sessions and fatigue/distraction events.

---

# Why This Task Is Important
- We need a persistent structure to log driver sessions and events
- The two-level structure (sessions containing events) enables better analytics
- The DriverState enum provides consistent state definitions across the application
- This is the foundation for all logging and statistics in the project

---

# Prerequisites
Before starting this task:
- Complete `TASK_01_Setup_Backend.txt`.
- Review `CODING_STANDARDS.txt`.

---

# Detailed Instructions

## Step 1: Create DriverState Enum

- Package: `com.driver_monitoring.model`
- File: `DriverState.java`

```java
// What is this file?
// This enum defines all possible states of a driver.
// Why is this needed?
// It provides a standardized way to represent driver states across the application.

package com.driver_monitoring.model;

public enum DriverState {
    NORMAL,     // Driver is alert and focused
    DISTRACTED, // Driver is not looking at the road
    DROWSY      // Driver appears to be sleepy or fatigued
}
```

## Step 2: Create Driver Entity

- Package: `com.driver_monitoring.model`
- File: `Driver.java`

```java
// What is this file?
// This class represents a Driver entity stored in the database.
// Why is this needed?
// It stores the driver's ID and name, which are used to associate driving sessions.

package com.driver_monitoring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    private String driverId; // Must be exactly 6 digits
    
    private String driverName;
}
```

## Step 3: Create DriverSession Entity

- Package: `com.driver_monitoring.model`
- File: `DriverSession.java`

```java
// What is this file?
// This class represents a driving session entity that tracks when a driver starts and ends driving.
// Why is this needed?
// It groups events within a specific time period and allows tracking total driving time.

package com.driver_monitoring.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;
    
    private String driverId; // FK to Driver
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime; // Null if session is active
    
    private Long totalDrivingTimeSeconds; // Will be calculated when session ends
    
    private boolean active = true; // Flag to mark active sessions
    
    // Constructor for starting a new session
    public DriverSession(String driverId) {
        this.driverId = driverId;
        this.startTime = LocalDateTime.now();
        this.active = true;
    }
    
    // Method to end a session
    public void endSession() {
        if (this.active) {
            this.endTime = LocalDateTime.now();
            this.totalDrivingTimeSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            this.active = false;
        }
    }
}
```

## Step 4: Create Event Entity

- Package: `com.driver_monitoring.model`
- File: `Event.java`

```java
// What is this file?
// This class represents an Event (either distraction or drowsiness) logged within a driving session.
// Why is this needed?
// It captures when and why a warning event occurred during driver monitoring.

package com.driver_monitoring.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;
    
    private Long sessionId; // FK to DriverSession
    
    private String driverId; // For direct queries by driver
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime; // For continuous events
    
    private float duration; // Duration in seconds
    
    private String eventType; // "DISTRACTED" or "DROWSY"
    
    @Column(columnDefinition = "CLOB")
    private String metadata; // JSON with additional metrics (EAR value, etc.)
    
    // Constructor for simple events
    public Event(Long sessionId, String driverId, String eventType, float duration) {
        this.sessionId = sessionId;
        this.driverId = driverId;
        this.startTime = LocalDateTime.now();
        this.endTime = LocalDateTime.now().plusSeconds((long)duration);
        this.duration = duration;
        this.eventType = eventType;
    }
    
    // Constructor with metadata
    public Event(Long sessionId, String driverId, String eventType, float duration, String metadata) {
        this(sessionId, driverId, eventType, duration);
        this.metadata = metadata;
    }
}
```

## Step 5: Create Repositories

- Package: `com.driver_monitoring.repository`

Create three interfaces:

**DriverRepository.java**
```java
// What is this file?
// Repository interface for accessing Driver data from the database.
// Why is this needed?
// It allows easy CRUD operations on Driver entities without boilerplate code.

package com.driver_monitoring.repository;

import com.driver_monitoring.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, String> {
}
```

**DriverSessionRepository.java**
```java
// What is this file?
// Repository interface for accessing DriverSession data from the database.
// Why is this needed?
// It allows management of driving sessions and finding active sessions.

package com.driver_monitoring.repository;

import com.driver_monitoring.model.DriverSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DriverSessionRepository extends JpaRepository<DriverSession, Long> {
    
    // Find all sessions for a specific driver
    List<DriverSession> findByDriverId(String driverId);
    
    // Find active session for a driver
    Optional<DriverSession> findByDriverIdAndActiveTrue(String driverId);
    
    // Find multiple active sessions for a driver (for error checking)
    List<DriverSession> findAllByDriverIdAndActiveTrue(String driverId);
    
    // Find stale active sessions (for cleanup)
    List<DriverSession> findByActiveTrueAndStartTimeBefore(LocalDateTime threshold);
    
    // Find most recent sessions
    @Query("SELECT s FROM DriverSession s ORDER BY s.startTime DESC")
    List<DriverSession> findRecentSessions();
}
```

**EventRepository.java**
```java
// What is this file?
// Repository interface for accessing Event data from the database.
// Why is this needed?
// It allows easy CRUD operations on Event entities and searching by session ID.

package com.driver_monitoring.repository;

import com.driver_monitoring.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    
    // Find events by session
    List<Event> findBySessionId(Long sessionId);
    
    // Find events by driver
    List<Event> findByDriverId(String driverId);
    
    // Find events by driver ordered by time (latest first)
    List<Event> findByDriverIdOrderByStartTimeDesc(String driverId);
    
    // Find events by session and type
    List<Event> findBySessionIdAndEventType(Long sessionId, String eventType);
    
    // Find events before a certain date (for archiving)
    List<Event> findByStartTimeBefore(LocalDateTime threshold);
    
    // Delete old events (for cleanup)
    @Modifying
    @Transactional
    void deleteByStartTimeBefore(LocalDateTime threshold);
    
    // Count events by session and type
    long countBySessionIdAndEventType(Long sessionId, String eventType);
}
```

## Step 6: Update application.properties for File-Based H2

- File: `src/main/resources/application.properties`

```properties
# Server settings
server.port=8080

# H2 Database settings - file-based for persistence
spring.datasource.url=jdbc:h2:file:./data/driver-monitoring-db;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE;MVCC=TRUE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

# JPA/Hibernate settings
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# H2 Console settings
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=false

# Connection pool settings
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

## Step 7: Create SessionCleanupService
- Package: `com.driver_monitoring.service`
- File: `SessionCleanupService.java`

```java
// What is this file?
// Service for cleaning up stale sessions and archiving old events.
// Why is this needed?
// To prevent database growth and ensure sessions are properly closed.

package com.driver_monitoring.service;

import com.driver_monitoring.model.DriverSession;
import com.driver_monitoring.model.Event;
import com.driver_monitoring.repository.DriverSessionRepository;
import com.driver_monitoring.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(SessionCleanupService.class);
    
    @Autowired
    private DriverSessionRepository sessionRepository;
    
    @Autowired
    private EventRepository eventRepository;
    
    // Run every hour to check for stale sessions (sessions that were not properly closed)
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void checkStaleActiveSessions() {
        // Sessions active for more than 12 hours are likely stale
        LocalDateTime threshold = LocalDateTime.now().minusHours(12);
        List<DriverSession> staleSessions = sessionRepository.findByActiveTrueAndStartTimeBefore(threshold);
        
        for (DriverSession session : staleSessions) {
            logger.warn("Found stale session: {} for driver: {}, active since: {}", 
                 session.getSessionId(), session.getDriverId(), session.getStartTime());
            
            session.endSession();
            sessionRepository.save(session);
            
            logger.info("Automatically closed stale session: {}", session.getSessionId());
        }
    }
    
    // Run once a day at midnight to archive old events (older than 30 days)
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void archiveOldEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        
        // For a prototype, we just delete old events
        // In a production system, you would archive them first
        long count = eventRepository.countByStartTimeBefore(threshold);
        eventRepository.deleteByStartTimeBefore(threshold);
        
        logger.info("Cleaned up {} old events from before {}", count, threshold);
    }
}
```

---

# Preventing Common Errors

## Database Access Issues
- **Always use transactions** for operations that modify multiple entities
- **Use connection pooling** to prevent "too many connections" errors
- **Set AUTO_SERVER=TRUE** in H2 URL to allow multiple processes to access the database
- **Set DB_CLOSE_ON_EXIT=FALSE** to prevent database corruption on unexpected shutdowns

## Orphaned Sessions
- Implement a cleanup service to detect and close "stale" sessions
- Always check for existing active sessions before creating new ones
- Use proper error handling to ensure sessions are closed even if exceptions occur

## Data Integrity
- Use appropriate column types (`CLOB` for JSON metadata)
- Include both `sessionId` and `driverId` in events for flexible querying
- Periodically archive or clean up old data to prevent database bloat

## Transaction Management
- Use `@Transactional` annotation for methods that update multiple records
- Handle concurrent access with appropriate isolation levels
- Add appropriate indexes for frequently queried fields

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Clear simple code
- Proper comments at the top of each class/file
- Logical structuring of fields and methods
- Use Lombok to reduce boilerplate code

---

# Success Criteria
- DriverState enum defines NORMAL, DISTRACTED, and DROWSY states
- Entities are correctly mapped to tables `drivers`, `driver_sessions` and `driver_events`
- Repositories compile without errors
- Application starts normally after adding entities
- Database is configured to persist data between application restarts
- Classes are properly commented according to standards
- Stale session cleanup mechanism is implemented

---

# References
- [Spring Data JPA Basics](https://spring.io/projects/spring-data-jpa)
- [Lombok Documentation](https://projectlombok.org/features/all)
- [H2 Database File Persistence](https://www.h2database.com/html/features.html#connection_modes)
- [Spring Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)

---

# End of TASK_02_Create_Entities_and_Repositories.txt