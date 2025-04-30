package com.drivermonitoring.controller;

import com.drivermonitoring.ai.FatiguePrediction;
import com.drivermonitoring.service.DriverAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DriverApiController {
    @Autowired
    private DriverAnalyticsService driverAnalyticsService;

    @GetMapping("/api/driver/{driverId}/prediction")
    public FatiguePrediction getDriverPrediction(@PathVariable String driverId, @RequestParam(defaultValue = "1") int period) {
        return driverAnalyticsService.getFatiguePrediction(driverId, period);
    }
}
