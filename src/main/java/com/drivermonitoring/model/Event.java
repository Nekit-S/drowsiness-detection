// What is this file?
// This class represents an Event (either distraction or drowsiness) logged within a driving session.
// Why is this needed?
// It captures when and why a warning event occurred during driver monitoring.

package com.drivermonitoring.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;
    
    private Long sessionId; // FK to DriverSession
    
    private String driverId; // For direct queries by driver
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime; // For continuous events
    
    private float duration; // Duration in seconds
    
    private String eventType; // "DISTRACTED" or "DROWSY"
    
    @Column(columnDefinition = "CLOB")
    private String metadata; // JSON with additional metrics (EAR value, etc.)
    
    @Column(nullable = true)
    private Float earValue; // Значение EAR (среднее или последнее)

    @Column(nullable = true)
    private Float leftEar; // Значение EAR для левого глаза

    @Column(nullable = true)
    private Float rightEar; // Значение EAR для правого глаза

    @Column(length = 32, nullable = true)
    private String headDirection; // Направление головы (например, "FORWARD", "LEFT", "RIGHT", "DOWN")

    @Column(nullable = true)
    private Boolean faceDetected; // Было ли обнаружено лицо

    @Column(length = 32, nullable = true)
    private String featureSource; // Источник данных (например, "MediaPipe)
    
    // Constructor for simple events
    public Event(Long sessionId, String driverId, String eventType, float duration) {
        this.sessionId = sessionId;
        this.driverId = driverId;
        this.startTime = LocalDateTime.now();
        this.endTime = LocalDateTime.now().plusSeconds((long)duration);
        this.duration = duration;
        this.eventType = eventType;
    }
    
    // Constructor with metadata
    public Event(Long sessionId, String driverId, String eventType, float duration, String metadata) {
        this(sessionId, driverId, eventType, duration);
        this.metadata = metadata;
    }
}