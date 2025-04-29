// File: src/main/java/com/drivermonitoring/service/EventLoggingService.java
// What is this file?
// This service provides functionality to log important driver events into the database.
// Why is this needed?
// To capture and persist distraction and drowsiness incidents within driving sessions for further analysis.

package com.drivermonitoring.service;

import com.drivermonitoring.model.DriverState;
import com.drivermonitoring.model.Event;
import java.util.List;
import java.util.Map;

public interface EventLoggingService {

    /**
     * Logs a basic event for a driver in the current active session.
     * @param driverId The ID of the driver.
     * @param driverState The type of event (DROWSY or DISTRACTED).
     * @param duration The duration of the event in seconds.
     * @return The created Event or null if no active session or if state is NORMAL.
     */
    Event logEvent(String driverId, DriverState driverState, float duration);

    /**
     * Logs an event with additional metadata for advanced analytics.
     * @param driverId The ID of the driver.
     * @param driverState The type of event (DROWSY or DISTRACTED).
     * @param duration The duration of the event in seconds.
     * @param metadata Additional data to store with the event (e.g., EAR value, head position).
     * @return The created Event or null if no active session or if state is NORMAL.
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
