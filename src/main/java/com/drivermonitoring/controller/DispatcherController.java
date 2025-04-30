// What is this file?
// This controller manages the Dispatcher panel: list drivers, select a driver, view stats and logs.
// Why is this needed?
// It connects the database (Driver, Event entities) with the Dispatcher front-end.

package com.drivermonitoring.controller;

import com.drivermonitoring.model.Driver;
import com.drivermonitoring.model.Event;
import com.drivermonitoring.repository.DriverRepository;
import com.drivermonitoring.repository.EventRepository;
import com.drivermonitoring.service.DriverAnalyticsService;
import com.drivermonitoring.service.DriverRatingService;
import com.drivermonitoring.ai.FatiguePrediction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class DispatcherController {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DriverAnalyticsService driverAnalyticsService;

    @Autowired
    private DriverRatingService driverRatingService;

    @GetMapping("/dispatcher")
    public String dispatcherPanel(Model model) {
        List<Driver> drivers = driverRepository.findAll();
        // Формируем список с оценкой
        List<HashMap<String, String>> driverInfos = new ArrayList<>();
        for (Driver driver : drivers) {
            HashMap<String, String> info = new HashMap<>();
            info.put("driverName", driver.getDriverName());
            info.put("driverId", driver.getDriverId());
            info.put("rating", driverRatingService.getDriverRating(driver.getDriverId()));
            driverInfos.add(info);
        }
        model.addAttribute("driverInfos", driverInfos);
        return "dispatcher_panel";
    }

    @GetMapping("/dispatcher/driver/{driverId}")
    public String driverStats(@PathVariable String driverId, Model model) {
        Driver driver = driverRepository.findById(driverId).orElse(null);
        List<Event> events = eventRepository.findByDriverIdOrderByStartTimeDesc(driverId);
        // Сводная статистика
        int totalEvents = events.size();
        float totalDuration = 0f;
        float drowsyTime = 0f;
        float distractedTime = 0f;
        float normalTime = 0f;
        float sumEar = 0f;
        int earCount = 0;
        float sumBlinkRate = 0f;
        int blinkCount = 0;
        for (Event e : events) {
            float dur = e.getDuration();
            totalDuration += dur;
            if ("DROWSY".equalsIgnoreCase(e.getEventType())) drowsyTime += dur;
            else if ("DISTRACTED".equalsIgnoreCase(e.getEventType())) distractedTime += dur;
            else normalTime += dur;
            if (e.getEarValue() != null) {
                sumEar += e.getEarValue();
                earCount++;
            }
            if (e.getMetadata() != null && e.getMetadata().contains("blinkRate")) {
                try {
                    String meta = e.getMetadata();
                    int idx = meta.indexOf("blinkRate");
                    if (idx >= 0) {
                        String sub = meta.substring(idx);
                        String[] parts = sub.split(":");
                        if (parts.length > 1) {
                            String val = parts[1].replaceAll("[^0-9.]", "");
                            sumBlinkRate += Float.parseFloat(val);
                            blinkCount++;
                        }
                    }
                } catch (Exception ignore) {}
            }
        }
        float totalRiskTime = drowsyTime + distractedTime;
        float drowsyPercent = totalDuration > 0 ? drowsyTime / totalDuration * 100f : 0f;
        float distractedPercent = totalDuration > 0 ? distractedTime / totalDuration * 100f : 0f;
        float normalPercent = totalDuration > 0 ? normalTime / totalDuration * 100f : 0f;
        float avgEar = earCount > 0 ? sumEar / earCount : 0f;
        float avgBlinkRate = blinkCount > 0 ? sumBlinkRate / blinkCount : 0f;
        // Количество уникальных сессий
        long sessionCount = events.stream().map(Event::getSessionId).distinct().count();
        float avgSessionDuration = sessionCount > 0 ? totalDuration / sessionCount : 0f;
        // Передаём в шаблон
        model.addAttribute("driver", driver);
        model.addAttribute("events", events);
        model.addAttribute("drowsyPercent", drowsyPercent);
        model.addAttribute("distractedPercent", distractedPercent);
        model.addAttribute("normalPercent", normalPercent);
        model.addAttribute("avgEar", avgEar);
        model.addAttribute("avgBlinkRate", avgBlinkRate);
        model.addAttribute("sessionCount", sessionCount);
        model.addAttribute("avgSessionDuration", avgSessionDuration);
        return "driver_statistics";
    }

    @GetMapping("/dispatcher/driver/{driverId}/prediction")
    public String driverPrediction(@PathVariable String driverId, Model model) {
        FatiguePrediction prediction = driverAnalyticsService.getFatiguePrediction(driverId);
        model.addAttribute("prediction", prediction);
        model.addAttribute("driverId", driverId);
        return "driver_prediction";
    }
}
