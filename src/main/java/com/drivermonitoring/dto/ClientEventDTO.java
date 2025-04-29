// File: src/main/java/com/drivermonitoring/dto/ClientEventDTO.java
// What is this file?
// Data Transfer Object for client-side detection events.
// Why is this needed?
// It structures incoming JSON data from MediaPipe processing.

package com.drivermonitoring.dto;

import lombok.Data;
import java.util.Map;

@Data // Lombok annotation to generate getters, setters, toString, etc.
public class ClientEventDTO {
    private String driverId;
    private Long sessionId; // Changed from Object to Long for type safety
    private String state;
    private Float duration; // Changed from Double to Float for consistency with JS/potential precision needs
    private Map<String, Object> metadata;
}
