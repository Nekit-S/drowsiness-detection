// File: src/main/java/com/drivermonitoring/service/SessionServiceImpl.java
// What is this file?
// Implementation of the SessionService interface.
// Why is this needed?
// Provides the concrete logic for managing driver sessions using the repository.

package com.drivermonitoring.service;

import com.drivermonitoring.model.DriverSession;
import com.drivermonitoring.repository.DriverSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SessionServiceImpl implements SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);

    @Autowired
    private DriverSessionRepository sessionRepository;

    @Override
    @Transactional
    public DriverSession startSession(String driverId) {
        if (driverId == null || driverId.trim().isEmpty()) {
            logger.error("Cannot start session: driverId is null or empty.");
            return null; // Or throw an IllegalArgumentException
        }

        // Check if there's already an active session for this driver
        Optional<DriverSession> existingActiveSession = sessionRepository.findByDriverIdAndActiveTrue(driverId);
        if (existingActiveSession.isPresent()) {
            logger.warn("Driver {} already has an active session ({}). Ending it before starting a new one.",
                        driverId, existingActiveSession.get().getSessionId());
            // Optionally end the existing session first
            endSessionInternal(existingActiveSession.get());
        }

        // Create and save the new session
        DriverSession newSession = new DriverSession(driverId);
        newSession.setStartTime(LocalDateTime.now());
        newSession.setActive(true);
        DriverSession savedSession = sessionRepository.save(newSession);
        logger.info("Started new session {} for driver {}", savedSession.getSessionId(), driverId);
        return savedSession;
    }

    @Override
    @Transactional
    public DriverSession endSession(String driverId) {
        if (driverId == null || driverId.trim().isEmpty()) {
            logger.error("Cannot end session: driverId is null or empty.");
            return null;
        }

        Optional<DriverSession> activeSessionOpt = sessionRepository.findByDriverIdAndActiveTrue(driverId);
        if (activeSessionOpt.isPresent()) {
            DriverSession sessionToEnd = activeSessionOpt.get();
            return endSessionInternal(sessionToEnd);
        } else {
            logger.warn("No active session found for driver {} to end.", driverId);
            return null;
        }
    }

    // Internal helper to end a session object
    private DriverSession endSessionInternal(DriverSession session) {
        session.endSession(); // Use the method within DriverSession entity
        DriverSession endedSession = sessionRepository.save(session);
        logger.info("Ended session {} for driver {}. Duration: {} seconds.",
                    endedSession.getSessionId(), endedSession.getDriverId(), endedSession.getTotalDrivingTimeSeconds());
        return endedSession;
    }

    @Override
    public DriverSession getActiveSession(String driverId) {
        if (driverId == null || driverId.trim().isEmpty()) {
            logger.warn("Cannot get active session: driverId is null or empty.");
            return null;
        }
        // findByDriverIdAndActiveTrue returns Optional, orElse(null) fits the required return type
        return sessionRepository.findByDriverIdAndActiveTrue(driverId).orElse(null);
    }

    @Override
    public Optional<DriverSession> getSessionById(Long sessionId) {
        if (sessionId == null) {
            logger.warn("Cannot get session by ID: sessionId is null.");
            return Optional.empty();
        }
        return sessionRepository.findById(sessionId);
    }

    @Override
    public List<DriverSession> getSessionsForDriver(String driverId) {
        if (driverId == null || driverId.trim().isEmpty()) {
            logger.warn("Cannot get sessions for driver: driverId is null or empty.");
            return List.of(); // Return empty list
        }
        return sessionRepository.findByDriverId(driverId);
    }

    @Override
    public List<DriverSession> getAllActiveSessions() {
        // Assuming repository has a method like findByActiveTrue()
        return sessionRepository.findByActiveTrue();
    }
}
