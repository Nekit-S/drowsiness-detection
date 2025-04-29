# TASK_06_Event_Logging_Service.txt

# Task Title
Create Event Logging Service with Session Management

---

# Goal
Develop a service that:
- Logs critical events (Distraction or Drowsiness) for drivers within their active session
- Saves event data with rich contextual information
- Manages the relationship between events and sessions
- Creates JSON metadata for advanced analytics

---

# Why This Task Is Important
- Proper event logging is critical for the Dispatcher Panel and analytics
- Session-based tracking provides better context for events
- The metadata approach allows for flexible data collection without schema changes

---

# Prerequisites
Before starting this task:
- Complete `TASK_05_Face_and_Eye_Detection.txt`.
- Complete `TASK_03_Create_Driver_Screen.txt` with session management.
- Review `CODING_STANDARDS.txt`.

---

# Detailed Instructions

## Step 1: Add Jackson Dependency for JSON Processing
Update the build.gradle file to include Jackson:

```gradle
// Add to dependencies section in build.gradle
implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
implementation 'org.slf4j:slf4j-api:2.0.5'
implementation 'ch.qos.logback:logback-classic:1.4.6'
```

## Step 2: Create EventLoggingService Interface
- Package: `com.driver_monitoring.service`
- File: `EventLoggingService.java`

```java
// What is this file?
// This service provides functionality to log important driver events into the database.
// Why is this needed?
// To capture and persist distraction and drowsiness incidents within driving sessions for further analysis.

package com.driver_monitoring.service;

import com.driver_monitoring.model.DriverState;
import com.driver_monitoring.model.Event;
import java.util.List;
import java.util.Map;

public interface EventLoggingService {

    /**
     * Logs a basic event for a driver in the current active session.
     * @param driverId The ID of the driver.
     * @param driverState The type of event (DROWSY or DISTRACTED).
     * @param duration The duration of the event in seconds.
     * @return The created Event or null if no active session
     */
    Event logEvent(String driverId, DriverState driverState, float duration);
    
    /**
     * Logs an event with additional metadata for advanced analytics.
     * @param driverId The ID of the driver.
     * @param driverState The type of event (DROWSY or DISTRACTED).
     * @param duration The duration of the event in seconds.
     * @param metadata Additional data to store with the event (e.g., EAR value, head position).
     * @return The created Event or null if no active session
     */
    Event logEventWithMetadata(String driverId, DriverState driverState, float duration, Map<String, Object> metadata);
    
    /**
     * Retrieves events for a specific driver session.
     * @param sessionId The ID of the session.
     * @return List of events for the session.
     */
    List<Event> getEventsForSession(Long sessionId);
    
    /**
     * Retrieves recent events for a driver.
     * @param driverId The ID of the driver.
     * @param limit Maximum number of events to retrieve.
     * @return List of recent events for the driver.
     */
    List<Event> getRecentEventsForDriver(String driverId, int limit);
}
```

## Step 3: Create EventLoggingServiceImpl
- Package: `com.driver_monitoring.service`
- File: `EventLoggingServiceImpl.java`

```java
// What is this file?
// Implements the logic for saving driver events to the database with session context.
// Why is this needed?
// It provides a clean, reusable way to store monitoring data with rich contextual information.

package com.driver_monitoring.service;

import com.driver_monitoring.model.DriverSession;
import com.driver_monitoring.model.DriverState;
import com.driver_monitoring.model.Event;
import com.driver_monitoring.repository.EventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventLoggingServiceImpl implements EventLoggingService {

    private static final Logger logger = LoggerFactory.getLogger(EventLoggingServiceImpl.class);

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private SessionService sessionService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Event logEvent(String driverId, DriverState driverState, float duration) {
        // Validate input parameters
        if (driverId == null || driverState == null) {
            logger.error("Invalid parameters: driverId or driverState is null");
            return null;
        }
        
        // Skip logging for NORMAL state
        if (driverState == DriverState.NORMAL) {
            return null;
        }
        
        try {
            // Get active session for the driver
            DriverSession session = sessionService.getActiveSession(driverId);
            if (session == null) {
                // Cannot log event without an active session
                logger.warn("Cannot log event: No active session for driver {}", driverId);
                return null;
            }
            
            Event event = new Event(
                session.getSessionId(),
                driverId,
                driverState.name(),
                duration
            );
            
            Event savedEvent = eventRepository.save(event);
            logger.debug("Logged {} event for driver {}, duration: {}s, session: {}", 
                        driverState, driverId, duration, session.getSessionId());
            
            return savedEvent;
        } catch (Exception e) {
            logger.error("Error logging event for driver {}: {}", driverId, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    @Transactional
    public Event logEventWithMetadata(String driverId, DriverState driverState, float duration, Map<String, Object> metadata) {
        // Validate input parameters
        if (driverId == null || driverState == null) {
            logger.error("Invalid parameters: driverId or driverState is null");
            return null;
        }
        
        // Skip logging for NORMAL state
        if (driverState == DriverState.NORMAL) {
            return null;
        }
        
        try {
            // Get active session for the driver
            DriverSession session = sessionService.getActiveSession(driverId);
            if (session == null) {
                logger.warn("Cannot log event with metadata: No active session for driver {}", driverId);
                return null;
            }
            
            // Ensure metadata is not null
            Map<String, Object> safeMetadata = metadata != null ? metadata : new HashMap<>();
            
            // Add timestamp if not present
            if (!safeMetadata.containsKey("timestamp")) {
                safeMetadata.put("timestamp", System.currentTimeMillis());
            }
            
            // Add event context
            safeMetadata.put("sessionId", session.getSessionId());
            safeMetadata.put("eventType", driverState.name());
            
            // Convert metadata map to JSON string
            String metadataJson;
            try {
                metadataJson = objectMapper.writeValueAsString(safeMetadata);
            } catch (JsonProcessingException e) {
                // If JSON conversion fails, log the error and use an empty JSON object
                logger.error("Failed to convert metadata to JSON: {}", e.getMessage());
                metadataJson = "{}";
            }
            
            Event event = new Event(
                session.getSessionId(),
                driverId,
                driverState.name(),
                duration,
                metadataJson
            );
            
            Event savedEvent = eventRepository.save(event);
            logger.debug("Logged {} event with metadata for driver {}, duration: {}s, session: {}", 
                    driverState, driverId, duration, session.getSessionId());
            
            return savedEvent;
        } catch (Exception e) {
            logger.error("Error logging event with metadata for driver {}: {}", driverId, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public List<Event> getEventsForSession(Long sessionId) {
        if (sessionId == null) {
            logger.warn("Cannot get events: sessionId is null");
            return Collections.emptyList();
        }
        
        try {
            return eventRepository.findBySessionId(sessionId);
        } catch (Exception e) {
            logger.error("Error retrieving events for session {}: {}", sessionId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<Event> getRecentEventsForDriver(String driverId, int limit) {
        if (driverId == null) {
            logger.warn("Cannot get recent events: driverId is null");
            return Collections.emptyList();
        }
        
        try {
            return eventRepository.findByDriverIdOrderByStartTimeDesc(driverId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving recent events for driver {}: {}", driverId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    // Helper method to build metadata for drowsy events
    public Map<String, Object> buildDrowsyEventMetadata(float earValue, float blinkRate, String headPosition) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("earValue", earValue);
        metadata.put("blinkRate", blinkRate);
        metadata.put("headPosition", headPosition);
        metadata.put("timestamp", LocalDateTime.now().toString());
        return metadata;
    }
    
    // Helper method to build metadata for distracted events
    public Map<String, Object> buildDistractedEventMetadata(String headPosition, float headAngle) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("headPosition", headPosition);
        metadata.put("headAngle", headAngle);
        metadata.put("timestamp", LocalDateTime.now().toString());
        return metadata;
    }
}
```

## Step 4: Create JSON Utility Class for Working with Event Metadata
- Package: `com.driver_monitoring.util`
- File: `JsonUtils.java`

```java
// What is this file?
// Utility class for working with JSON metadata in events.
// Why is this needed?
// It provides helper methods for extracting and analyzing metadata stored as JSON strings.

package com.driver_monitoring.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Safely parses a JSON string into a Map
     * @param json JSON string to parse
     * @return Map of key-value pairs, or empty map if parsing fails
     */
    public static Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return Collections.emptyMap();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * Extracts a specific value from JSON metadata
     * @param json JSON string
     * @param key Key to extract
     * @return Optional containing the value or empty if not found or error
     */
    public static Optional<Object> getValueFromJson(String json, String key) {
        Map<String, Object> map = parseJson(json);
        return Optional.ofNullable(map.get(key));
    }
    
    /**
     * Safely extracts a typed value from JSON
     * @param json JSON string
     * @param key Key to extract
     * @param type Class of the expected value type
     * @return Optional containing the typed value or empty if not found or error
     */
    public static <T> Optional<T> getTypedValueFromJson(String json, String key, Class<T> type) {
        Optional<Object> value = getValueFromJson(json, key);
        
        if (value.isPresent()) {
            Object obj = value.get();
            if (type.isInstance(obj)) {
                return Optional.of(type.cast(obj));
            } else {
                logger.warn("Value for key '{}' is not of expected type {}", key, type.getSimpleName());
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Safely converts an object to JSON string
     * @param object Object to convert
     * @return JSON string or "{}" if conversion fails
     */
    public static String toJson(Object object) {
        if (object == null) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert to JSON: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Merges multiple JSON strings into a single JSON object
     * @param jsonStrings Array of JSON strings to merge
     * @return A merged JSON string
     */
    public static String mergeJsonStrings(String... jsonStrings) {
        Map<String, Object> resultMap = new HashMap<>();
        
        for (String json : jsonStrings) {
            if (json != null && !json.isEmpty()) {
                Map<String, Object> map = parseJson(json);
                resultMap.putAll(map);
            }
        }
        
        return toJson(resultMap);
    }
}
```

## Step 5: Create EventMetadataService for Advanced Analytics
- Package: `com.driver_monitoring.service`
- File: `EventMetadataService.java`

```java
// What is this file?
// Service for advanced analytics on event metadata.
// Why is this needed?
// It extracts insights from event metadata for reporting and visualization.

package com.driver_monitoring.service;

import com.driver_monitoring.model.Event;
import com.driver_monitoring.repository.EventRepository;
import com.driver_monitoring.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
public class EventMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(EventMetadataService.class);

    @Autowired
    private EventRepository eventRepository;

    /**
     * Calculates average EAR value from drowsy events in a session
     * @param sessionId Session ID
     * @return Average EAR value or -1 if not available
     */
    public double getAverageEARForSession(Long sessionId) {
        try {
            List<Event> drowsyEvents = eventRepository.findBySessionIdAndEventType(sessionId, "DROWSY");
            
            OptionalDouble average = drowsyEvents.stream()
                .map(Event::getMetadata)
                .map(metadata -> JsonUtils.getTypedValueFromJson(metadata, "earValue", Double.class))
                .filter(Optional::isPresent)
                .mapToDouble(Optional::get)
                .average();
            
            return average.orElse(-1.0);
        } catch (Exception e) {
            logger.error("Error calculating average EAR for session {}: {}", sessionId, e.getMessage(), e);
            return -1.0;
        }
    }
    
    /**
     * Extracts all distinct head positions from distracted events
     * @param sessionId Session ID
     * @return List of head positions
     */
    public List<String> getHeadPositionsForSession(Long sessionId) {
        try {
            List<Event> distractedEvents = eventRepository.findBySessionIdAndEventType(sessionId, "DISTRACTED");
            
            return distractedEvents.stream()
                .map(Event::getMetadata)
                .map(metadata -> JsonUtils.getTypedValueFromJson(metadata, "headPosition", String.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error extracting head positions for session {}: {}", sessionId, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Gets all metadata fields used across events in a session
     * @param sessionId Session ID
     * @return Set of metadata field names
     */
    public List<String> getAllMetadataFieldsForSession(Long sessionId) {
        try {
            List<Event> events = eventRepository.findBySessionId(sessionId);
            
            return events.stream()
                .map(Event::getMetadata)
                .map(JsonUtils::parseJson)
                .flatMap(map -> map.keySet().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error collecting metadata fields for session {}: {}", sessionId, e.getMessage(), e);
            return List.of();
        }
    }
}
```

---

# Preventing Common Errors

## JSON Processing

### Security and Data Integrity
- **Always validate input metadata**: Check for null values and invalid content
- **Use try-catch for JSON operations**: All JSON conversions can throw exceptions
- **Provide fallbacks**: Always have a default when JSON parsing fails

```java
// Example of safe JSON conversion
String metadataJson;
try {
    metadataJson = objectMapper.writeValueAsString(metadata != null ? metadata : new HashMap<>());
} catch (JsonProcessingException e) {
    logger.error("Failed to serialize metadata: {}", e.getMessage());
    metadataJson = "{}"; // Default empty JSON
}
```

### Type Safety in Metadata
- **Use utility methods for type-safe extraction**:
```java
// Safe extraction with proper type checking
Optional<Double> earValue = JsonUtils.getTypedValueFromJson(metadata, "earValue", Double.class);
if (earValue.isPresent()) {
    double ear = earValue.get();
    // Process valid EAR value
}
```

## Session Management

### Session Validation
- **Always check for active session before logging**: Events without session context are meaningless
- **Log warning when session not found**: Helps diagnose integration issues
- **Consider session boundaries**: Be careful with events at session start/end

## Database Operations

### Error Handling
- **Use @Transactional for database operations**: Ensures atomicity
- **Return empty collections instead of null**: Prevents NPEs in calling code
- **Implement retry mechanism for transient errors**: Improves reliability during high load

## Concurrent Access

### Thread Safety
- **Ensure ObjectMapper is thread-safe**: Either create per-request or use a thread-safe configuration
- **Watch for race conditions**: Be careful when sessions are starting/ending

---

# Important Details
- Event duration should be passed in seconds (e.g., 2.5 seconds)
- Events are always linked to an active session
- Metadata allows for flexible data collection without schema changes
- Only DROWSY and DISTRACTED states are logged, not NORMAL states
- Use proper error handling to ensure logging doesn't disrupt critical functions

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Clear method responsibilities
- Proper comments at class and method level
- Minimal and logical code flow
- Comprehensive error handling

---

# Success Criteria
- Events are created only for Distracted or Drowsy states
- Events are properly saved into the database with session context
- JSON metadata is correctly serialized and stored
- Application runs without any exceptions during logging
- Database contains rich contextual information for analysis
- Utility methods provide safe access to metadata fields
- The service is robust against malformed input and network issues
- Code is simple, clean, and properly commented

---

# References
- [Spring Data JPA Repositories](https://spring.io/guides/gs/accessing-data-jpa/)
- [Jackson ObjectMapper](https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html)
- [JSON Processing with Jackson](https://www.baeldung.com/jackson-object-mapper-tutorial)
- [Spring Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)

---

# End of TASK_06_Event_Logging_Service.txt
