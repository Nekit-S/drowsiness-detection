# TASK_06_Event_Logging_Service_MediaPipe.txt

# Task Title
Create Event Logging Service with MediaPipe Integration

---

# Goal
Develop a service that:
- Принимает события обнаружения состояний водителя от клиентского MediaPipe
- Логирует критические события (Сонливость или Отвлечение) для водителей в рамках их активной сессии
- Сохраняет данные событий с богатым контекстом (метаданными)
- Управляет связью между событиями и сессиями
- Создает JSON-метаданные для продвинутой аналитики

---

# Why This Task Is Important
- Правильное логирование событий критично для панели диспетчера и аналитики
- Отслеживание на основе сессий обеспечивает лучший контекст для событий
- Подход с метаданными позволяет гибко собирать данные без изменения схемы БД

---

# Prerequisites
Before starting this task:
- Complete `TASK_05_Face_and_Eye_Detection_MediaPipe.txt` with MediaPipe integration.
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

## Step 2: Create Client-Side Event DTO
Создайте DTO для приема событий от клиентского JavaScript:

- Package: `com.driver_monitoring.dto`
- File: `ClientEventDTO.java`

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

## Step 4: Create EventLoggingServiceImpl
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
            safeMetadata.put("source", "MediaPipe"); // Добавляем источник события
            
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
            logger.debug("Logged {} event with metadata from MediaPipe for driver {}, duration: {}s, session: {}", 
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
    
    // Helper method to build metadata for drowsy events from MediaPipe
    public Map<String, Object> buildMediaPipeDrowsyEventMetadata(float earValue, float blinkRate, long closeDuration) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("earValue", earValue);
        metadata.put("blinkRate", blinkRate);
        metadata.put("closeDuration", closeDuration);
        metadata.put("source", "MediaPipe");
        metadata.put("timestamp", LocalDateTime.now().toString());
        return metadata;
    }
    
    // Helper method to build metadata for distracted events from MediaPipe
    public Map<String, Object> buildMediaPipeDistractedEventMetadata(boolean faceDetected) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("faceDetected", faceDetected);
        metadata.put("source", "MediaPipe");
        metadata.put("timestamp", LocalDateTime.now().toString());
        return metadata;
    }
}
```

## Step 5: Create REST Controller for MediaPipe Events
- Package: `com.driver_monitoring.controller`
- File: `MediaPipeEventController.java`

```java
// What is this file?
// REST controller for receiving detection events from browser-based MediaPipe.
// Why is this needed?
// To bridge between client-side MediaPipe detection and server-side event logging.

package com.driver_monitoring.controller;

import com.driver_monitoring.dto.ClientEventDTO;
import com.driver_monitoring.model.DriverState;
import com.driver_monitoring.service.EventLoggingService;
import com.driver_monitoring.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MediaPipeEventController {

    private static final Logger logger = LoggerFactory.getLogger(MediaPipeEventController.class);

    @Autowired
    private EventLoggingService eventLoggingService;
    
    @Autowired
    private SessionService sessionService;
    
    @PostMapping("/detection-event")
    public ResponseEntity<?> logDetectionEvent(@RequestBody ClientEventDTO eventData) {
        try {
            logger.debug("Received detection event from MediaPipe: {}", eventData);
            
            // Проверяем входные данные
            if (eventData.getDriverId() == null || eventData.getState() == null) {
                return ResponseEntity.badRequest().body("Missing driverId or state");
            }
            
            // Проверяем наличие активной сессии
            if (sessionService.getActiveSession(eventData.getDriverId()) == null) {
                return ResponseEntity.badRequest().body("No active session found for driver: " + eventData.getDriverId());
            }
            
            // Конвертируем строковое состояние в enum
            DriverState driverState;
            try {
                driverState = DriverState.valueOf(eventData.getState());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid driver state: " + eventData.getState());
            }
            
            // Устанавливаем продолжительность по умолчанию, если не указана
            float duration = (eventData.getDuration() != null) ? eventData.getDuration() : 1.0f;
            
            // Логируем событие с метаданными
            if (driverState != DriverState.NORMAL) {
                eventLoggingService.logEventWithMetadata(
                    eventData.getDriverId(),
                    driverState,
                    duration,
                    eventData.getMetadata()
                );
                
                logger.info("Logged {} event from MediaPipe for driver {} with duration {}s", 
                          driverState, eventData.getDriverId(), duration);
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error processing MediaPipe event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing event: " + e.getMessage());
        }
    }
    
    @PostMapping("/driver-state")
    public ResponseEntity<?> updateDriverState(@RequestBody ClientEventDTO eventData) {
        // Альтернативный эндпоинт с тем же функционалом, но другим именем
        // Для разработчиков, которые предпочитают другое именование API
        return logDetectionEvent(eventData);
    }
}
```

## Step 6: Create JSON Utility Class for Working with Event Metadata
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
    
    /**
     * Checks if a JSON string contains a specific key
     * @param json JSON string
     * @param key Key to check
     * @return true if key exists, false otherwise
     */
    public static boolean containsKey(String json, String key) {
        return getValueFromJson(json, key).isPresent();
    }
    
    /**
     * Extracts metadata from MediaPipe JSON format
     * @param json JSON string from MediaPipe
     * @return Map with standardized metadata keys
     */
    public static Map<String, Object> extractMediaPipeMetadata(String json) {
        Map<String, Object> map = parseJson(json);
        Map<String, Object> standardizedMap = new HashMap<>();
        
        // Стандартизация ключей между MediaPipe и серверной частью
        if (map.containsKey("earValue")) standardizedMap.put("eyeAspectRatio", map.get("earValue"));
        if (map.containsKey("closeDuration")) standardizedMap.put("eyeClosureDuration", map.get("closeDuration"));
        if (map.containsKey("faceDetected")) standardizedMap.put("faceDetected", map.get("faceDetected"));
        
        // Добавляем источник
        standardizedMap.put("source", "MediaPipe");
        
        // Копируем все остальные ключи
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!standardizedMap.containsKey(entry.getKey())) {
                standardizedMap.put(entry.getKey(), entry.getValue());
            }
        }
        
        return standardizedMap;
    }
}
```

## Step 7: Create EventMetadataService for Advanced Analytics
- Package: `com.driver_monitoring.service`
- File: `EventMetadataService.java`

```java
// What is this file?
// Service for advanced analytics on event metadata, including from MediaPipe.
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
                .map(metadata -> {
                    // Check both possible keys for EAR (from MediaPipe or server-side)
                    Optional<Double> earValue = JsonUtils.getTypedValueFromJson(metadata, "earValue", Double.class);
                    if (earValue.isPresent()) return earValue;
                    
                    return JsonUtils.getTypedValueFromJson(metadata, "eyeAspectRatio", Double.class);
                })
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
    
    /**
     * Gets frequency of detection source types
     * @param sessionId Session ID
     * @return Map of source type to count
     */
    public Map<String, Long> getSourceDistribution(Long sessionId) {
        try {
            List<Event> events = eventRepository.findBySessionId(sessionId);
            
            return events.stream()
                .map(Event::getMetadata)
                .map(metadata -> JsonUtils.getTypedValueFromJson(metadata, "source", String.class)
                     .orElse("unknown"))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        } catch (Exception e) {
            logger.error("Error calculating source distribution for session {}: {}", 
                        sessionId, e.getMessage(), e);
            return Map.of("error", 0L);
        }
    }
    
    /**
     * Gets count of event types
     * @param sessionId Session ID
     * @return Map of event type to count
     */
    public Map<String, Long> getEventTypeDistribution(Long sessionId) {
        try {
            List<Event> events = eventRepository.findBySessionId(sessionId);
            
            return events.stream()
                .collect(Collectors.groupingBy(Event::getEventType, Collectors.counting()));
        } catch (Exception e) {
            logger.error("Error calculating event type distribution for session {}: {}", 
                        sessionId, e.getMessage(), e);
            return Map.of("error", 0L);
        }
    }
}
```

---

# Preventing Common Errors

## Интеграция с клиентским MediaPipe
- **Обрабатывайте разные форматы метаданных**: MediaPipe может отправлять метаданные в формате, отличном от серверного
- **Стандартизируйте имена ключей**: Используйте JsonUtils для преобразования имен ключей
- **Логируйте входящие данные**: Это поможет при отладке проблем

```java
// Пример стандартизации метаданных от MediaPipe
@PostMapping("/detection-event")
public ResponseEntity<?> logDetectionEvent(@RequestBody ClientEventDTO eventData) {
    // Логируем входящие данные
    logger.debug("Received event from MediaPipe: {}", eventData);
    
    // Стандартизируем метаданные
    Map<String, Object> standardizedMetadata = JsonUtils.extractMediaPipeMetadata(
        JsonUtils.toJson(eventData.getMetadata())
    );
    
    // Используем стандартизированные метаданные вместо оригинальных
    eventData.setMetadata(standardizedMetadata);
}
```

## Обработка JSON
- **Всегда проверяйте входные метаданные**: Они могут быть null или содержать неожиданные типы данных
- **Используйте try-catch для JSON-операций**: Все JSON-преобразования могут выбрасывать исключения
- **Предоставьте резервные варианты**: Всегда имейте значение по умолчанию на случай ошибки JSON-парсинга

```java
// Правильная обработка JSON
try {
    metadataJson = objectMapper.writeValueAsString(
        metadata != null ? metadata : new HashMap<>()
    );
} catch (JsonProcessingException e) {
    logger.error("Failed to serialize metadata: {}", e.getMessage());
    metadataJson = "{}"; // Пустой JSON объект по умолчанию
}
```

## Управление сессиями
- **Всегда проверяйте наличие активной сессии**: События без контекста сессии бессмысленны
- **Логируйте предупреждение при отсутствии сессии**: Это поможет диагностировать проблемы интеграции
- **Обеспечьте корректную работу с границами сессии**: Будьте осторожны с событиями в начале/конце сессии

```java
// Правильная проверка сессии
DriverSession session = sessionService.getActiveSession(driverId);
if (session == null) {
    logger.warn("Cannot log event: No active session for driver {}", driverId);
    return ResponseEntity.badRequest().body("No active session found");
}
```

---

# Important Details
- События приходят от JavaScript-кода MediaPipe из браузера
- Длительность события передается в секундах (например, 2.5 секунды)
- События всегда привязаны к активной сессии
- Метаданные позволяют гибко собирать данные без изменения схемы
- Только состояния DROWSY и DISTRACTED логируются, не NORMAL
- Правильная обработка ошибок обеспечивает работоспособность критических функций

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Clear method responsibilities
- Proper comments at class and method level
- Minimal and logical code flow
- Comprehensive error handling

---

# Success Criteria
- События создаются только для состояний Distracted или Drowsy
- События правильно сохраняются в базе данных с контекстом сессии
- JSON-метаданные корректно сериализуются и сохраняются
- Приложение работает без исключений при логировании
- База данных содержит богатую контекстную информацию для анализа
- Служебные методы обеспечивают безопасный доступ к полям метаданных
- Сервис устойчив к некорректным входным данным и сетевым проблемам
- Код простой, чистый и правильно прокомментирован

---

# References
- [Spring MVC @RestController](https://spring.io/guides/gs/rest-service/)
- [Jackson ObjectMapper](https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html)
- [JSON Processing with Jackson](https://www.baeldung.com/jackson-object-mapper-tutorial)
- [Spring Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)

---

# End of TASK_06_Event_Logging_Service_MediaPipe.txt?
// Data Transfer Object for client-side detection events from MediaPipe.
// Why is this needed?
// It structures incoming JSON data from browser MediaPipe processing.

package com.driver_monitoring.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ClientEventDTO {
    private String driverId;
    private Long sessionId;
    private String state;
    private Float duration;
    private Map<String, Object> metadata;
}
```

## Step 3: Create EventLoggingService Interface
- Package: `com.driver_monitoring.service`
- File: `EventLoggingService.java`

```java
// What is this file