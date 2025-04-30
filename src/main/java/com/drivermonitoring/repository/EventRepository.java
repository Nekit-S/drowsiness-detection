// What is this file?
// Repository interface for accessing Event data from the database.
// Why is this needed?
// It allows easy CRUD operations on Event entities and searching by session ID.

package com.drivermonitoring.repository;

import com.drivermonitoring.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    
    // Find events by session
    List<Event> findBySessionId(Long sessionId);
    
    // Find events by driver
    List<Event> findByDriverId(String driverId);
    
    // Find events by driver ordered by time (latest first)
    List<Event> findByDriverIdOrderByStartTimeDesc(String driverId);
    
    // Find events by session and type
    List<Event> findBySessionIdAndEventType(Long sessionId, String eventType);
    
    // Find events before a certain date (for archiving)
    List<Event> findByStartTimeBefore(LocalDateTime threshold);
    
    // Delete old events (for cleanup)
    @Modifying
    @Transactional
    void deleteByStartTimeBefore(LocalDateTime threshold);
    
    // Count events before a certain date (for cleanup reporting)
    long countByStartTimeBefore(LocalDateTime threshold);
    
    // Count events by session and type
    long countBySessionIdAndEventType(Long sessionId, String eventType);
}