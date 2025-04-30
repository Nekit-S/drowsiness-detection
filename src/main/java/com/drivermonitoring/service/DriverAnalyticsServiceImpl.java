package com.drivermonitoring.service;

import com.drivermonitoring.ai.FatiguePrediction;
import com.drivermonitoring.ai.MockAIFatiguePredictionModel;
import com.drivermonitoring.model.DriverSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DriverAnalyticsServiceImpl implements DriverAnalyticsService {
    @Autowired
    private SessionService sessionService;
    @Autowired
    private DriverFeatureExtractor featureExtractor;

    private final MockAIFatiguePredictionModel aiModel = new MockAIFatiguePredictionModel();

    @Override
    public FatiguePrediction getFatiguePrediction(String driverId) {
        DriverSession session = sessionService.getActiveSession(driverId);
        if (session == null) return new FatiguePrediction(FatiguePrediction.RiskLevel.LOW, 0f, 120, "Нет активной сессии");
        LocalDateTime now = LocalDateTime.now();
        var features = featureExtractor.extractFeatures(driverId, session.getStartTime(), now);
        return aiModel.predict(features);
    }

    @Override
    public FatiguePrediction getFatiguePrediction(String driverId, int periodMinutes) {
        DriverSession session = sessionService.getActiveSession(driverId);
        if (session == null) return new FatiguePrediction(FatiguePrediction.RiskLevel.LOW, 0f, 120, "Нет активной сессии");
        LocalDateTime now = LocalDateTime.now();
        var features = featureExtractor.extractFeatures(driverId, session.getStartTime(), now, periodMinutes);
        return aiModel.predict(features);
    }
}
