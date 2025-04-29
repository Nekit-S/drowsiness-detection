// File: src/main/java/com/drivermonitoring/controller/MediaPipeEventController.java
// What is this file?
// REST controller for receiving detection events from browser-based MediaPipe.
// Why is this needed?
// To bridge between client-side MediaPipe detection and server-side event logging.

package com.drivermonitoring.controller;

import com.drivermonitoring.dto.ClientEventDTO;
import com.drivermonitoring.model.DriverState;
import com.drivermonitoring.service.EventLoggingService;
import com.drivermonitoring.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api") // Base path for API endpoints
public class MediaPipeEventController {

    private static final Logger logger = LoggerFactory.getLogger(MediaPipeEventController.class);

    @Autowired
    private EventLoggingService eventLoggingService;

    @Autowired
    private SessionService sessionService; // Assuming SessionService exists

    @PostMapping("/detection-event")
    public ResponseEntity<?> logDetectionEvent(@RequestBody ClientEventDTO eventData) {
        try {
            logger.debug("Received detection event: {}", eventData);

            // Validate essential input data
            if (eventData == null || eventData.getDriverId() == null || eventData.getState() == null) {
                logger.warn("Received invalid event data: {}", eventData);
                return ResponseEntity.badRequest().body("Missing required fields: driverId, state");
            }

            String driverId = eventData.getDriverId();
            String stateStr = eventData.getState();

            // Check for active session *before* processing further
            if (sessionService.getActiveSession(driverId) == null) {
                logger.warn("No active session found for driver {} when receiving event.", driverId);
                // Consider if this should be an error or just ignored depending on requirements
                return ResponseEntity.badRequest().body("No active session found for driver: " + driverId);
            }

            // Convert state string to enum, handling potential errors
            DriverState driverState;
            try {
                driverState = DriverState.valueOf(stateStr.toUpperCase()); // Use uppercase for robustness
            } catch (IllegalArgumentException e) {
                logger.warn("Received invalid driver state value: {}", stateStr);
                return ResponseEntity.badRequest().body("Invalid driver state: " + stateStr);
            }

            // Log only non-NORMAL states
            if (driverState != DriverState.NORMAL) {
                // Use default duration if not provided (e.g., 1.0 second for instantaneous events)
                float duration = (eventData.getDuration() != null) ? eventData.getDuration() : 1.0f;

                // Log the event using the service
                eventLoggingService.logEventWithMetadata(
                    driverId,
                    driverState,
                    duration,
                    eventData.getMetadata() // Pass metadata map directly
                );

                // No need for logger.info here as the service implementation already logs
            } else {
                 logger.debug("Received NORMAL state event for driver {}, not logging.", driverId);
            }

            return ResponseEntity.ok().build(); // Acknowledge successful processing

        } catch (Exception e) {
            // Catch unexpected errors during processing
            logger.error("Error processing detection event for driver {}: {}",
                         (eventData != null ? eventData.getDriverId() : "unknown"), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("An internal error occurred while processing the event.");
        }
    }

    // The task description included an alternative endpoint /driver-state.
    // If needed, it can be added here, potentially calling the same logic.
    /*
    @PostMapping("/driver-state")
    public ResponseEntity<?> updateDriverState(@RequestBody ClientEventDTO eventData) {
        logger.debug("Received event via /driver-state endpoint, forwarding to logDetectionEvent.");
        return logDetectionEvent(eventData);
    }
    */
}
