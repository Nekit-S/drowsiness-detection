# TASK_08_Unit_Tests.txt

# Task Title
Create Unit Tests for Critical Components with Session Support

---

# Goal
Implement simple and reliable unit tests for the most important services:
- SessionService
- FaceDetectionService
- EventLoggingService

Unit tests help catch bugs early and validate core logic automatically, especially after changes to the architecture.

---

# Why This Task Is Important
- Increases reliability of the session-based system
- Helps prevent future regressions
- Validates proper interactions between components
- Necessary for any production-quality backend

---

# Prerequisites
Before starting this task:
- Complete `TASK_07_Notification_System.txt`.
- Review `CODING_STANDARDS.txt`.
- Understand basic JUnit 5 and Mockito usage.

---

# Detailed Instructions

## Step 1: Setup Test Structure
- Tests must be placed under:

```bash
src/test/java/com/driver_monitoring/
```

- Use package structures matching the main code (e.g., `service`, `model`).
- Create test resources directory:

```bash
src/test/resources/
```

## Step 2: Add Test Dependencies
Update `build.gradle` to include testing libraries:

```gradle
// Testing dependencies
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.mockito:mockito-core:5.3.1'
```

## Step 3: Create SessionServiceTest
- File: `src/test/java/com/driver_monitoring/service/SessionServiceTest.java`

```java
// What is this file?
// Unit tests for SessionService.
// Why is this needed?
// To verify that session creation, retrieval and ending work correctly.

package com.driver_monitoring.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import com.driver_monitoring.model.DriverSession;
import com.driver_monitoring.repository.DriverSessionRepository;

@SpringBootTest
class SessionServiceTest {

    @Mock
    private DriverSessionRepository sessionRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private final String TEST_DRIVER_ID = "123456";

    @BeforeEach
    void setUp() {
        // Clear any previous interactions
        reset(sessionRepository);
    }

    @Test
    void testStartSession_NoExistingSession() {
        // Arrange
        when(sessionRepository.findByDriverIdAndActiveTrue(TEST_DRIVER_ID))
            .thenReturn(Optional.empty());
        
        DriverSession newSession = new DriverSession(TEST_DRIVER_ID);
        when(sessionRepository.save(any(DriverSession.class))).thenReturn(newSession);

        // Act
        DriverSession result = sessionService.startSession(TEST_DRIVER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_DRIVER_ID, result.getDriverId());
        assertTrue(result.isActive());
        verify(sessionRepository).findByDriverIdAndActiveTrue(TEST_DRIVER_ID);
        verify(sessionRepository).save(any(DriverSession.class));
    }

    @Test
    void testStartSession_WithExistingSession() {
        // Arrange
        DriverSession existingSession = new DriverSession(TEST_DRIVER_ID);
        existingSession.setSessionId(1L);
        existingSession.setStartTime(LocalDateTime.now().minusHours(1));
        existingSession.setActive(true);
        
        when(sessionRepository.findByDriverIdAndActiveTrue(TEST_DRIVER_ID))
            .thenReturn(Optional.of(existingSession));
        
        DriverSession newSession = new DriverSession(TEST_DRIVER_ID);
        when(sessionRepository.save(any(DriverSession.class))).thenReturn(newSession);

        // Act
        DriverSession result = sessionService.startSession(TEST_DRIVER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_DRIVER_ID, result.getDriverId());
        assertTrue(result.isActive());
        verify(sessionRepository).findByDriverIdAndActiveTrue(TEST_DRIVER_ID);
        // Verify old session ended and saved + new session saved
        verify(sessionRepository, times(2)).save(any(DriverSession.class));
    }

    @Test
    void testEndSession_WithActiveSession() {
        // Arrange
        DriverSession activeSession = new DriverSession(TEST_DRIVER_ID);
        activeSession.setSessionId(1L);
        activeSession.setStartTime(LocalDateTime.now().minusHours(1));
        activeSession.setActive(true);
        
        when(sessionRepository.findByDriverIdAndActiveTrue(TEST_DRIVER_ID))
            .thenReturn(Optional.of(activeSession));
        when(sessionRepository.save(any(DriverSession.class))).thenReturn(activeSession);

        // Act
        DriverSession result = sessionService.endSession(TEST_DRIVER_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isActive());
        assertNotNull(result.getEndTime());
        assertNotNull(result.getTotalDrivingTimeSeconds());
        verify(sessionRepository).findByDriverIdAndActiveTrue(TEST_DRIVER_ID);
        verify(sessionRepository).save(activeSession);
    }

    @Test
    void testEndSession_NoActiveSession() {
        // Arrange
        when(sessionRepository.findByDriverIdAndActiveTrue(TEST_DRIVER_ID))
            .thenReturn(Optional.empty());

        // Act
        DriverSession result = sessionService.endSession(TEST_DRIVER_ID);

        // Assert
        assertNull(result);
        verify(sessionRepository).findByDriverIdAndActiveTrue(TEST_DRIVER_ID);
        verify(sessionRepository, never()).save(any(DriverSession.class));
    }

    @Test
    void testGetActiveSession_SessionExists() {
        // Arrange
        DriverSession activeSession = new DriverSession(TEST_DRIVER_ID);
        activeSession.setActive(true);
        
        when(sessionRepository.findByDriverIdAndActiveTrue(TEST_DRIVER_ID))
            .thenReturn(Optional.of(activeSession));

        // Act
        DriverSession result = sessionService.getActiveSession(TEST_DRIVER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_DRIVER_ID, result.getDriverId());
        assertTrue(result.isActive());
    }

    @Test
    void testGetActiveSession_NoSessionExists() {
        // Arrange
        when(sessionRepository.findByDriverIdAndActiveTrue(TEST_DRIVER_ID))
            .thenReturn(Optional.empty());

        // Act
        DriverSession result = sessionService.getActiveSession(TEST_DRIVER_ID);

        // Assert
        assertNull(result);
    }
}
```

## Step 4: Create EventLoggingServiceTest
- File: `src/test/java/com/driver_monitoring/service/EventLoggingServiceTest.java`

```java
// What is this file?
// Unit tests for EventLoggingService.
// Why is this needed?
// To verify that event logging works correctly within sessions and with metadata.

package com.driver_monitoring.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import com.driver_monitoring.model.DriverSession;
import com.driver_monitoring.model.DriverState;
import com.driver_monitoring.model.Event;
import com.driver_monitoring.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class EventLoggingServiceTest {

    @Mock
    private EventRepository eventRepository;
    
    @Mock
    private SessionService sessionService;
    
    @InjectMocks
    private EventLoggingServiceImpl eventLoggingService;
    
    private final String TEST_DRIVER_ID = "123456";
    private final Long TEST_SESSION_ID = 1L;
    private DriverSession activeSession;
    
    @BeforeEach
    void setUp() {
        // Clear any previous interactions
        reset(eventRepository, sessionService);
        
        // Setup active session
        activeSession = new DriverSession(TEST_DRIVER_ID);
        activeSession.setSessionId(TEST_SESSION_ID);
        activeSession.setStartTime(LocalDateTime.now().minusHours(1));
        activeSession.setActive(true);
    }
    
    @Test
    void testLogEvent_WithActiveSession() {
        // Arrange
        when(sessionService.getActiveSession(TEST_DRIVER_ID)).thenReturn(activeSession);
        
        Event savedEvent = new Event(TEST_SESSION_ID, TEST_DRIVER_ID, "DROWSY", 2.5f);
        savedEvent.setEventId(1L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        
        // Act
        Event result = eventLoggingService.logEvent(TEST_DRIVER_ID, DriverState.DROWSY, 2.5f);
        
        // Assert
        assertNotNull(result);
        assertEquals(TEST_SESSION_ID, result.getSessionId());
        assertEquals(TEST_DRIVER_ID, result.getDriverId());
        assertEquals("DROWSY", result.getEventType());
        assertEquals(2.5f, result.getDuration());
        
        verify(sessionService).getActiveSession(TEST_DRIVER_ID);
        verify(eventRepository).save(any(Event.class));
    }
    
    @Test
    void testLogEvent_NormalState_DoesNotLog() {
        // Act
        Event result = eventLoggingService.logEvent(TEST_DRIVER_ID, DriverState.NORMAL, 1.0f);
        
        // Assert
        assertNull(result);
        verify(sessionService, never()).getActiveSession(anyString());
        verify(eventRepository, never()).save(any(Event.class));
    }
    
    @Test
    void testLogEvent_NoActiveSession_DoesNotLog() {
        // Arrange
        when(sessionService.getActiveSession(TEST_DRIVER_ID)).thenReturn(null);
        
        // Act
        Event result = eventLoggingService.logEvent(TEST_DRIVER_ID, DriverState.DROWSY, 2.0f);
        
        // Assert
        assertNull(result);
        verify(sessionService).getActiveSession(TEST_DRIVER_ID);
        verify(eventRepository, never()).save(any(Event.class));
    }
    
    @Test
    void testLogEventWithMetadata_WithActiveSession() {
        // Arrange
        when(sessionService.getActiveSession(TEST_DRIVER_ID)).thenReturn(activeSession);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("earValue", 0.15);
        metadata.put("closeDuration", 2500);
        
        Event savedEvent = new Event(TEST_SESSION_ID, TEST_DRIVER_ID, "DROWSY", 2.5f, "{}");
        savedEvent.setEventId(1L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        
        // Act
        Event result = eventLoggingService.logEventWithMetadata(
            TEST_DRIVER_ID,
            DriverState.DROWSY,
            2.5f,
            metadata
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(TEST_SESSION_ID, result.getSessionId());
        assertEquals(TEST_DRIVER_ID, result.getDriverId());
        assertEquals("DROWSY", result.getEventType());
        assertEquals(2.5f, result.getDuration());
        
        verify(sessionService).getActiveSession(TEST_DRIVER_ID);
        verify(eventRepository).save(any(Event.class));
    }
    
    @Test
    void testGetEventsForSession() {
        // Arrange
        Event event1 = new Event(TEST_SESSION_ID, TEST_DRIVER_ID, "DROWSY", 2.0f);
        Event event2 = new Event(TEST_SESSION_ID, TEST_DRIVER_ID, "DISTRACTED", 1.5f);
        List<Event> events = List.of(event1, event2);
        
        when(eventRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(events);
        
        // Act
        List<Event> result = eventLoggingService.getEventsForSession(TEST_SESSION_ID);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(TEST_SESSION_ID, result.get(0).getSessionId());
        assertEquals(TEST_SESSION_ID, result.get(1).getSessionId());
        verify(eventRepository).findBySessionId(TEST_SESSION_ID);
    }
}
```

## Step 5: Create FaceDetectionServiceTest
- File: `src/test/java/com/driver_monitoring/service/FaceDetectionServiceTest.java`

```java
// What is this file?
// Unit tests for FaceDetectionService.
// Why is this needed?
// To ensure face and eye detection logic works correctly with session integration.

package com.driver_monitoring.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import com.driver_monitoring.model.DriverSession;
import com.driver_monitoring.model.DriverState;

@SpringBootTest
class FaceDetectionServiceTest {

    @Mock
    private ResourceLoader resourceLoader;
    
    @Mock
    private EventLoggingService eventLoggingService;
    
    @Mock
    private SessionService sessionService;
    
    @Spy
    @InjectMocks
    private FaceDetectionServiceImpl faceDetectionService;
    
    private final String TEST_DRIVER_ID = "123456";
    private DriverSession activeSession;
    
    @BeforeEach
    void setUp() {
        // Clear any previous interactions
        reset(eventLoggingService, sessionService);
        
        // Setup active session
        activeSession = new DriverSession(TEST_DRIVER_ID);
        activeSession.setSessionId(1L);
        activeSession.setActive(true);
    }
    
    @Test
    void testAnalyzeFrame_NoActiveSession_ReturnsNormal() {
        // Arrange
        Mat frame = new Mat();
        when(sessionService.getActiveSession(TEST_DRIVER_ID)).thenReturn(null);
        
        // Act
        DriverState result = faceDetectionService.analyzeFrame(frame, TEST_DRIVER_ID);
        
        // Assert
        assertEquals(DriverState.NORMAL, result);
        verify(sessionService).getActiveSession(TEST_DRIVER_ID);
        verify(eventLoggingService, never()).logEventWithMetadata(anyString(), any(), anyFloat(), anyMap());
    }
    
    @Test
    void testAnalyzeFrame_FaceNotDetected_ReturnsDistracted() {
        // Arrange
        Mat frame = new Mat();
        when(sessionService.getActiveSession(TEST_DRIVER_ID)).thenReturn(activeSession);
        
        // Mock internal methods to simulate no face detected
        doReturn(null).when(faceDetectionService).detectFace(any(Mat.class));
        
        // Act
        DriverState result = faceDetectionService.analyzeFrame(frame, TEST_DRIVER_ID);
        
        // Assert
        assertEquals(DriverState.DISTRACTED, result);
        verify(sessionService).getActiveSession(TEST_DRIVER_ID);
        verify(eventLoggingService).logEventWithMetadata(
            eq(TEST_DRIVER_ID),
            eq(DriverState.DISTRACTED),
            anyFloat(),
            any(Map.class)
        );
    }
    
    @Test
    void testAnalyzeFrame_FaceDetectedNoEyePoints_ReturnsNormal() {
        // Arrange
        Mat frame = new Mat();
        when(sessionService.getActiveSession(TEST_DRIVER_ID)).thenReturn(activeSession);
        
        // Mock internal methods to simulate face detected but no eye landmarks
        doReturn(new Rect()).when(faceDetectionService).detectFace(any(Mat.class));
        doReturn(null).when(faceDetectionService).detectEyeLandmarks(any(Mat.class), any(Rect.class));
        
        // Act
        DriverState result = faceDetectionService.analyzeFrame(frame, TEST_DRIVER_ID);
        
        // Assert
        assertEquals(DriverState.NORMAL, result);
        verify(sessionService).getActiveSession(TEST_DRIVER_ID);
        verify(eventLoggingService, never()).logEventWithMetadata(anyString(), any(), anyFloat(), anyMap());
    }
    
    // Note: Complete eye detection tests would require more complex mocking
    // or integration tests with actual image data
}
```

## Step 6: Create Simple Test Resources
- Create a simple test image in `src/test/resources/test_face.jpg`
- Create a mock model file for testing in `src/test/resources/models/mock_model.dat`

---

# Important Details
- Mock dependencies to avoid real AI model loading during tests
- Use `@Spy` for partial mocking where needed
- Tests should be focused on the business logic, not the image processing
- Keep all tests stable, fast, and reliable for CI/CD integration

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Clear naming of tests
- One logical assertion per test if possible
- Proper comments describing each test case

---

# Success Criteria
- Tests compile and pass successfully
- Tests cover the most important scenarios including session logic
- Tests validate proper interactions between components
- Code follows the established standards

---

# References
- [JUnit 5 Basics](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

---

# End of TASK_08_Unit_Tests.txt
