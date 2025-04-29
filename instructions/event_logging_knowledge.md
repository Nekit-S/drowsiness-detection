# Knowledge: event_logging_knowledge.txt

# Purpose
This knowledge file explains the two-level logging approach for driver events in the Driver Monitoring System.
It provides clear rules, examples, and best practices for working with sessions and events.

---

# Logging Architecture Overview

The system uses a two-level hierarchical structure for logging:

1. **Driver Sessions**: Represent time periods when a driver is actively using the system
2. **Driver Events**: Individual incidents (drowsiness, distraction) that occur within a session

This structure provides better context and enables more sophisticated analytics.

---

# Database Design

## 1. Driver Entity
| Field       | Type   | Description |
|-------------|--------|-------------|
| driverId    | String | 6-digit unique identifier (PK) |
| driverName  | String | Name of the driver |

## 2. DriverSession Entity
| Field                  | Type          | Description |
|------------------------|---------------|-------------|
| sessionId              | Long (auto)   | Unique session identifier (PK) |
| driverId               | String        | Foreign key to Driver |
| startTime              | LocalDateTime | When the session began |
| endTime                | LocalDateTime | When the session ended (null if active) |
| totalDrivingTimeSeconds| Long          | Duration of session in seconds |
| active                 | boolean       | Flag for active sessions |

## 3. Event Entity
| Field          | Type          | Description |
|----------------|---------------|-------------|
| eventId        | Long (auto)   | Unique event identifier (PK) |
| sessionId      | Long          | Foreign key to DriverSession |
| driverId       | String        | Foreign key to Driver (for direct queries) |
| startTime      | LocalDateTime | When the event started |
| endTime        | LocalDateTime | When the event ended |
| duration       | float         | Duration in seconds |
| eventType      | String        | "DROWSY" or "DISTRACTED" |
| metadata       | JSON (CLOB)   | Additional event details as JSON |

---

# Session Management

## 1. Starting a Session
When a driver logs in, create a new session:

```java
DriverSession session = new DriverSession(driverId);
session.setStartTime(LocalDateTime.now());
session.setActive(true);
sessionRepository.save(session);
```

## 2. Ending a Session
When a driver logs out, end the current session:

```java
DriverSession session = sessionRepository.findByDriverIdAndActiveTrue(driverId).orElse(null);
if (session != null) {
    session.setEndTime(LocalDateTime.now());
    session.setTotalDrivingTimeSeconds(
        ChronoUnit.SECONDS.between(session.getStartTime(), session.getEndTime())
    );
    session.setActive(false);
    sessionRepository.save(session);
}
```

## 3. Finding Active Session
Before logging events, always get the current active session:

```java
Optional<DriverSession> activeSession = sessionRepository.findByDriverIdAndActiveTrue(driverId);
if (!activeSession.isPresent()) {
    // Cannot log event without an active session
    return;
}
Long sessionId = activeSession.get().getSessionId();
```

---

# Event Logging Strategy

## 1. What to Log
Log only critical events:
- **Distracted**: When the driver looks away from the road
- **Drowsy**: When the driver closes eyes for too long

**Important:**
- **Do NOT** log "Normal" states
- Only create a new event entry if an actual problem is detected
- Always associate events with the current active session

## 2. Using Metadata for Rich Contextual Information
Store additional data as JSON in the metadata field:

```java
// Create metadata for a drowsy event
Map<String, Object> metadata = new HashMap<>();
metadata.put("earValue", 0.15);        // Eye Aspect Ratio
metadata.put("blinkRate", 25);         // Blinks per minute
metadata.put("closeDuration", 2500);   // Milliseconds
metadata.put("headPosition", "center");
metadata.put("timestamp", System.currentTimeMillis());

// Convert to JSON
String metadataJson = objectMapper.writeValueAsString(metadata);

// Create and save the event
Event event = new Event(
    sessionId,        // From active session
    driverId,
    "DROWSY",
    2.5f,             // Duration in seconds
    metadataJson      // JSON metadata
);
```

## 3. Recommended Metadata Fields

### For Drowsy Events:
- `earValue`: The Eye Aspect Ratio value (typically 0.15-0.3)
- `closeDuration`: How long eyes were closed in milliseconds
- `blinkRate`: Estimated blinks per minute (if available)
- `timestamp`: System time when event was detected

### For Distracted Events:
- `headPosition`: Direction of head (left, right, down)
- `headAngle`: Angle of head turn in degrees (if available)
- `faceDetected`: Boolean indicating if face was detected
- `timestamp`: System time when event was detected

---

# Example Code: Complete Event Logging

```java
// This example shows full event logging with session context and metadata
public class EventLoggingServiceImpl implements EventLoggingService {

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private SessionService sessionService;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    public Event logDrowsyEvent(String driverId, float earValue, long closeDuration) {
        // Skip if normal state
        if (earValue > 0.25f) {
            return null; // Not drowsy
        }
        
        // Find active session
        DriverSession session = sessionService.getActiveSession(driverId);
        if (session == null) {
            return null; // Cannot log without session
        }
        
        try {
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("earValue", earValue);
            metadata.put("closeDuration", closeDuration);
            metadata.put("timestamp", System.currentTimeMillis());
            
            // Convert to JSON
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Calculate duration in seconds
            float durationSec = closeDuration / 1000.0f;
            
            // Create event with session context and metadata
            Event event = new Event(
                session.getSessionId(),
                driverId,
                "DROWSY",
                durationSec,
                metadataJson
            );
            
            // Save and return the event
            return eventRepository.save(event);
            
        } catch (Exception e) {
            // Log error and return null on failure
            System.err.println("Failed to log drowsy event: " + e.getMessage());
            return null;
        }
    }
    
    // Similar method for distracted events...
}
```

---

# Working with Metadata

## 1. Storing Metadata

```java
// Convert Java object to JSON string
ObjectMapper mapper = new ObjectMapper();
String metadataJson = mapper.writeValueAsString(metadataMap);

// Store in event
event.setMetadata(metadataJson);
```

## 2. Retrieving Metadata

```java
// Get metadata JSON from event
String metadataJson = event.getMetadata();

// Parse JSON to Java Map
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> metadata = mapper.readValue(
    metadataJson,
    new TypeReference<Map<String, Object>>() {}
);

// Access specific fields
double earValue = (double) metadata.get("earValue");
```

## 3. Displaying Metadata in UI

```html
<!-- In Thymeleaf template -->
<button type="button" class="btn btn-sm btn-info" 
        th:attr="data-metadata=${event.metadata}"
        onclick="showMetadata(this.getAttribute('data-metadata'))">
    Details
</button>

<script>
function showMetadata(metadataJson) {
    const metadata = JSON.parse(metadataJson);
    // Display in modal or tooltip
    console.log(metadata);
}
</script>
```

---

# Best Practices

1. **Always Check for Active Session**:
   - Only log events within an active session
   - This maintains data integrity and provides context

2. **Use Specific Metadata for Each Event Type**:
   - Drowsy events should include EAR values
   - Distracted events should include head position information

3. **Optimize Performance**:
   - Convert maps to JSON efficiently
   - Consider using async logging for non-critical operations

4. **Ensure Proper Error Handling**:
   - Handle JSON parsing exceptions gracefully
   - Never let logging exceptions affect core functionality

5. **Query Data Efficiently**:
   - Use the sessionId as the primary filter for event queries
   - Only query by driverId when looking across multiple sessions

---

# Example Queries

## 1. Get All Events for a Session
```java
List<Event> sessionEvents = eventRepository.findBySessionId(sessionId);
```

## 2. Count Events by Type in a Session
```java
long drowsyCount = eventRepository.countBySessionIdAndEventType(sessionId, "DROWSY");
long distractedCount = eventRepository.countBySessionIdAndEventType(sessionId, "DISTRACTED");
```

## 3. Find All Active Sessions
```java
List<DriverSession> activeSessions = sessionRepository.findByActiveTrue();
```

## 4. Get Recent Events for a Driver
```java
List<Event> recentEvents = eventRepository.findByDriverIdOrderByStartTimeDesc(driverId)
    .stream()
    .limit(10)
    .collect(Collectors.toList());
```

---

# Summary
- Use a two-level structure: Sessions contain Events
- Start a session when driver logs in, end when they log out
- Only log Drowsy and Distracted events (not Normal)
- Use rich metadata in JSON format for additional context
- Always verify an active session exists before logging events
- Query efficiently using sessionId as primary filter

This approach provides a robust foundation for detailed analytics, historical tracking, and pattern recognition with minimal database structure changes.

---

# End of event_logging_knowledge.txt
