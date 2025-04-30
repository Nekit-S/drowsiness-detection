package com.drivermonitoring.service;

import com.drivermonitoring.model.Event;
import com.drivermonitoring.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DriverFeatureExtractor {
    @Autowired
    private EventRepository eventRepository;

    // Извлекает признаки для анализа за последние 30 минут
    public Map<String, Float> extractFeatures(String driverId, LocalDateTime sessionStart, LocalDateTime now, int periodMinutes) {
        List<Event> recentEvents = eventRepository.findByDriverIdOrderByStartTimeDesc(driverId)
                .stream()
                .filter(e -> e.getStartTime() != null && e.getStartTime().isAfter(now.minusMinutes(periodMinutes)))
                .collect(Collectors.toList());

        // EAR
        List<Float> earValues = recentEvents.stream()
                .map(Event::getEarValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        float avgEar = earValues.isEmpty() ? 0.3f : (float) earValues.stream().mapToDouble(f -> f).average().orElse(0.3);
        float minEar = earValues.isEmpty() ? 0.3f : Collections.min(earValues);

        // Количество событий сонливости
        long drowsyEvents = recentEvents.stream()
                .filter(e -> "DROWSY".equalsIgnoreCase(e.getEventType()))
                .count();
        // Количество событий отвлечения
        long distractionEvents = recentEvents.stream()
                .filter(e -> "DISTRACTED".equalsIgnoreCase(e.getEventType()))
                .count();

        // Продолжительность текущей сессии (в минутах)
        float drivingDuration = sessionStart != null ? (float) Duration.between(sessionStart, now).toMinutes() : 0f;

        // Фактор времени суток
        float timeOfDayFactor = calculateTimeOfDayFactor(now);

        // Имитация частоты моргания (количество событий blink за 30 мин)
        long blinkEvents = recentEvents.stream()
                .filter(e -> e.getMetadata() != null && e.getMetadata().contains("blink"))
                .count();
        float blinkRate = blinkEvents / 30.0f; // в мин-1

        // Доля времени в тревожном состоянии
        float periodSeconds = periodMinutes * 60f;
        float drowsyTime = recentEvents.stream()
            .filter(e -> "DROWSY".equalsIgnoreCase(e.getEventType()))
            .map(Event::getDuration)
            .filter(Objects::nonNull)
            .reduce(0f, Float::sum);
        float distractedTime = recentEvents.stream()
            .filter(e -> "DISTRACTED".equalsIgnoreCase(e.getEventType()))
            .map(Event::getDuration)
            .filter(Objects::nonNull)
            .reduce(0f, Float::sum);
        float drowsyTimeFraction = periodSeconds > 0 ? drowsyTime / periodSeconds : 0f;
        float distractedTimeFraction = periodSeconds > 0 ? distractedTime / periodSeconds : 0f;

        Map<String, Float> features = new HashMap<>();
        features.put("earValue", avgEar);
        features.put("minEar", minEar);
        features.put("drowsyEvents", drowsyEvents / 30.0f); // нормализация
        features.put("distractionCount", distractionEvents / 30.0f);
        features.put("drivingDuration", drivingDuration / 120.0f); // нормализация на 2 часа
        features.put("timeOfDay", timeOfDayFactor);
        features.put("blinkRate", blinkRate);
        features.put("drowsyEventsCount", (float)drowsyEvents);
        features.put("distractionEventsCount", (float)distractionEvents);
        features.put("drowsyTimeFraction", drowsyTimeFraction);
        features.put("distractedTimeFraction", distractedTimeFraction);
        // (Опционально: можно удалить старые признаки drowsyEvents/distractionCount, если они больше не нужны)
        return features;
    }

    // Старый метод для обратной совместимости (по умолчанию 30 минут)
    public Map<String, Float> extractFeatures(String driverId, LocalDateTime sessionStart, LocalDateTime now) {
        return extractFeatures(driverId, sessionStart, now, 30);
    }

    // Фактор времени суток по руководству
    public float calculateTimeOfDayFactor(LocalDateTime time) {
        int hour = time.getHour();
        if (hour >= 2 && hour < 6) return 1.0f;
        if (hour >= 14 && hour < 16) return 0.7f;
        if (hour >= 20 || hour < 2) return 0.5f;
        if (hour >= 6 && hour < 10) return 0.2f;
        return 0.1f;
    }
}
