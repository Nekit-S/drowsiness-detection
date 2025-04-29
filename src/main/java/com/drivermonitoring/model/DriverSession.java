// What is this file?
// This class represents a driving session entity that tracks when a driver starts and ends driving.
// Why is this needed?
// It groups events within a specific time period and allows tracking total driving time.

package com.drivermonitoring.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;
    
    private String driverId; // FK to Driver
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime; // Null if session is active
    
    private Long totalDrivingTimeSeconds; // Will be calculated when session ends
    
    private boolean active = true; // Flag to mark active sessions
    
    // Constructor for starting a new session
    public DriverSession(String driverId) {
        this.driverId = driverId;
        this.startTime = LocalDateTime.now();
        this.active = true;
    }
    
    // Method to end a session
    public void endSession() {
        if (this.active) {
            this.endTime = LocalDateTime.now();
            this.totalDrivingTimeSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            this.active = false;
        }
    }
}