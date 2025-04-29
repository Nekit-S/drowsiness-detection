// What is this file?
// Repository interface for accessing DriverSession data from the database.
// Why is this needed?
// It allows management of driving sessions and finding active sessions.

package com.drivermonitoring.repository;

import com.drivermonitoring.model.DriverSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DriverSessionRepository extends JpaRepository<DriverSession, Long> {
    
    // Find all sessions for a specific driver
    List<DriverSession> findByDriverId(String driverId);
    
    // Find active session for a driver
    Optional<DriverSession> findByDriverIdAndActiveTrue(String driverId);
    
    // Find multiple active sessions for a driver (for error checking)
    List<DriverSession> findAllByDriverIdAndActiveTrue(String driverId);
    
    // Find stale active sessions (for cleanup)
    List<DriverSession> findByActiveTrueAndStartTimeBefore(LocalDateTime threshold);
    
    // Find most recent sessions
    @Query("SELECT s FROM DriverSession s ORDER BY s.startTime DESC")
    List<DriverSession> findRecentSessions();
}