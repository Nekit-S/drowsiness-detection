package com.drivermonitoring.service;

import com.drivermonitoring.ai.FatiguePrediction;

public interface DriverAnalyticsService {
    FatiguePrediction getFatiguePrediction(String driverId);
    FatiguePrediction getFatiguePrediction(String driverId, int periodMinutes);
}
