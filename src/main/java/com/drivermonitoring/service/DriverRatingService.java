package com.drivermonitoring.service;

import com.drivermonitoring.model.Event;
import com.drivermonitoring.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DriverRatingService {
    @Autowired
    private EventRepository eventRepository;

    public String getDriverRating(String driverId) {
        List<Event> events = eventRepository.findByDriverId(driverId);
        long drowsy = events.stream().filter(e -> "DROWSY".equalsIgnoreCase(e.getEventType())).count();
        long distracted = events.stream().filter(e -> "DISTRACTED".equalsIgnoreCase(e.getEventType())).count();
        long total = events.size();
        float risk = (drowsy + distracted) / (float) Math.max(1, total);
        if (risk < 0.05) return "Надёжный";
        if (risk < 0.15) return "Требует внимания";
        return "Рискованный";
    }
}
