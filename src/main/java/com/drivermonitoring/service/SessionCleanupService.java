// What is this file?
// Service for cleaning up stale sessions and archiving old events.
// Why is this needed?
// To prevent database growth and ensure sessions are properly closed.

package com.drivermonitoring.service;

import com.drivermonitoring.model.DriverSession;
import com.drivermonitoring.repository.DriverSessionRepository;
import com.drivermonitoring.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@EnableScheduling
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