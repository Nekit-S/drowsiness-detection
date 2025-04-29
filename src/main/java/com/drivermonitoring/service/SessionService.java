// File: src/main/java/com/drivermonitoring/service/SessionService.java
// What is this file?
// Interface for managing driver sessions.
// Why is this needed?
// Defines the contract for starting, ending, and retrieving driver sessions.

package com.drivermonitoring.service;

import com.drivermonitoring.model.DriverSession;
import java.util.List;
import java.util.Optional;

public interface SessionService {

    /**
     * Starts a new session for a given driver.
     * If a session is already active for the driver, it might end the previous one or handle as an error.
     * @param driverId The ID of the driver starting the session.
     * @return The newly created DriverSession.
     */
    DriverSession startSession(String driverId);

    /**
     * Ends the currently active session for a given driver.
     * Calculates duration and marks the session as inactive.
     * @param driverId The ID of the driver ending the session.
     * @return The ended DriverSession, or null if no active session was found.
     */
    DriverSession endSession(String driverId);

    /**
     * Retrieves the currently active session for a given driver.
     * @param driverId The ID of the driver.
     * @return The active DriverSession, or null if no session is active for this driver.
     */
    DriverSession getActiveSession(String driverId); // Method needed by EventLoggingService

    /**
     * Retrieves a session by its unique ID.
     * @param sessionId The ID of the session.
     * @return An Optional containing the DriverSession if found, otherwise empty.
     */
    Optional<DriverSession> getSessionById(Long sessionId);

    /**
     * Retrieves all sessions for a specific driver.
     * @param driverId The ID of the driver.
     * @return A list of all sessions (active and inactive) for the driver.
     */
    List<DriverSession> getSessionsForDriver(String driverId);

    /**
     * Retrieves all currently active sessions.
     * @return A list of all active DriverSessions.
     */
    List<DriverSession> getAllActiveSessions();
}
