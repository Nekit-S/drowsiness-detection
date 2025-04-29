// File: src/main/java/com/drivermonitoring/service/EventMetadataService.java
// What is this file?
// Service for advanced analytics on event metadata, including from MediaPipe.
// Why is this needed?
// It extracts insights from event metadata for reporting and visualization.

package com.drivermonitoring.service;

import com.drivermonitoring.model.Event;
import com.drivermonitoring.repository.EventRepository;
import com.drivermonitoring.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
public class EventMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(EventMetadataService.class);

    @Autowired
    private EventRepository eventRepository; // Assuming EventRepository exists

    /**
     * Calculates the average Eye Aspect Ratio (EAR) value from drowsy events within a specific session.
     * It checks common keys like 'earValue' or 'eyeAspectRatio' in the event metadata.
     * @param sessionId The ID of the session to analyze.
     * @return The average EAR value as a double, or OptionalDouble.empty() if no valid EAR values are found or an error occurs.
     */
    public OptionalDouble getAverageEARForSession(Long sessionId) {
        if (sessionId == null) {
            logger.warn("Cannot calculate average EAR: sessionId is null");
            return OptionalDouble.empty();
        }

        try {
            // Retrieve only DROWSY events for the session
            List<Event> drowsyEvents = eventRepository.findBySessionIdAndEventType(sessionId, "DROWSY");

            OptionalDouble average = drowsyEvents.stream()
                .map(Event::getMetadata) // Get metadata JSON string from each event
                .map(metadataJson -> {
                    // Try extracting EAR value using common keys, attempting conversion to Double
                    Optional<Double> earOpt = JsonUtils.getTypedValueFromJson(metadataJson, "earValue", Double.class);
                    if (earOpt.isPresent()) return earOpt;

                    // Fallback to another common key
                    return JsonUtils.getTypedValueFromJson(metadataJson, "eyeAspectRatio", Double.class);
                })
                .filter(Optional::isPresent) // Keep only those where an EAR value was found
                .mapToDouble(Optional::get) // Convert Optional<Double> to DoubleStream
                .average(); // Calculate the average

            if (!average.isPresent()) {
                 logger.debug("No valid EAR values found in metadata for drowsy events in session {}", sessionId);
            }

            return average;
        } catch (Exception e) {
            logger.error("Error calculating average EAR for session {}: {}", sessionId, e.getMessage(), e);
            return OptionalDouble.empty(); // Return empty on error
        }
    }

    /**
     * Collects all unique metadata field names used across all events in a specific session.
     * @param sessionId The ID of the session to analyze.
     * @return A sorted List of unique metadata keys found, or an empty list if an error occurs.
     */
    public List<String> getAllMetadataFieldsForSession(Long sessionId) {
         if (sessionId == null) {
            logger.warn("Cannot get metadata fields: sessionId is null");
            return Collections.emptyList();
        }

        try {
            List<Event> events = eventRepository.findBySessionId(sessionId);

            return events.stream()
                .map(Event::getMetadata) // Get metadata JSON string
                .map(JsonUtils::parseJson) // Parse JSON into a Map
                .flatMap(map -> map.keySet().stream()) // Get all keys from each map
                .distinct() // Keep only unique keys
                .sorted() // Sort alphabetically
                .collect(Collectors.toList()); // Collect into a List
        } catch (Exception e) {
            logger.error("Error collecting metadata fields for session {}: {}", sessionId, e.getMessage(), e);
            return Collections.emptyList(); // Return empty list on error
        }
    }

    /**
     * Calculates the distribution (count) of different event source types (e.g., 'MediaPipe', 'manual')
     * based on the 'source' key in the event metadata for a specific session.
     * @param sessionId The ID of the session to analyze.
     * @return A Map where keys are source types (String) and values are their counts (Long).
     *         Returns an empty map if an error occurs.
     */
    public Map<String, Long> getSourceDistributionForSession(Long sessionId) {
         if (sessionId == null) {
            logger.warn("Cannot get source distribution: sessionId is null");
            return Collections.emptyMap();
        }

        try {
            List<Event> events = eventRepository.findBySessionId(sessionId);

            return events.stream()
                .map(Event::getMetadata) // Get metadata JSON
                .map(metadata -> JsonUtils.getTypedValueFromJson(metadata, "source", String.class)
                     .orElse("unknown")) // Extract 'source' field, default to 'unknown'
                .collect(Collectors.groupingBy(source -> source, Collectors.counting())); // Group by source and count
        } catch (Exception e) {
            logger.error("Error calculating source distribution for session {}: {}",
                        sessionId, e.getMessage(), e);
            return Collections.emptyMap(); // Return empty map on error
        }
    }

    /**
     * Calculates the distribution (count) of different event types (e.g., 'DROWSY', 'DISTRACTED')
     * for a specific session directly from the Event entity's eventType field.
     * @param sessionId The ID of the session to analyze.
     * @return A Map where keys are event types (String) and values are their counts (Long).
     *         Returns an empty map if an error occurs.
     */
    public Map<String, Long> getEventTypeDistributionForSession(Long sessionId) {
         if (sessionId == null) {
            logger.warn("Cannot get event type distribution: sessionId is null");
            return Collections.emptyMap();
        }

        try {
            // Use the repository method for potentially better performance if available
            // return eventRepository.countEventsBySessionIdGroupedByType(sessionId);
            // Or use stream processing if repository method doesn't exist:
            List<Event> events = eventRepository.findBySessionId(sessionId);
            return events.stream()
                .collect(Collectors.groupingBy(Event::getEventType, Collectors.counting()));
        } catch (Exception e) {
            logger.error("Error calculating event type distribution for session {}: {}",
                        sessionId, e.getMessage(), e);
            return Collections.emptyMap(); // Return empty map on error
        }
    }
}
