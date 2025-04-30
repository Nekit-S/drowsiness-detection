package com.drivermonitoring.ai;

import java.util.*;

public class MockAIFatiguePredictionModel implements PredictionModel {
    private final Map<String, Float> weights;
    private final Random random = new Random();

    public MockAIFatiguePredictionModel() {
        // Веса скорректированы для новых признаков (доля времени)
        weights = new HashMap<>();
        weights.put("earValue", 0.33f);
        weights.put("drowsyTimeFraction", 0.22f);
        weights.put("drivingDuration", 0.17f);
        weights.put("timeOfDay", 0.12f);
        weights.put("blinkRate", 0.10f);
        weights.put("distractedTimeFraction", 0.06f);
    }

    @Override
    public FatiguePrediction predict(Map<String, Float> features) {
        float distractedTimeFraction = features.getOrDefault("distractedTimeFraction", 0f);
        float drowsyTimeFraction = features.getOrDefault("drowsyTimeFraction", 0f);
        float blinkRate = features.getOrDefault("blinkRate", 0f) * 60f; // blinkRate хранится в мин-1

        // Только три рекомендации
        if (drowsyTimeFraction > 0.1f || blinkRate > 24) {
            return new FatiguePrediction(FatiguePrediction.RiskLevel.HIGH, 1f, 0, "Водитель засыпает");
        } else if (distractedTimeFraction > 0.1f) {
            return new FatiguePrediction(FatiguePrediction.RiskLevel.MEDIUM, 0.5f, 10, "Водитель часто отвлекается");
        } else {
            return new FatiguePrediction(FatiguePrediction.RiskLevel.LOW, 0f, 120, "Всё нормально");
        }
    }

    @Override
    public void train(List<Map<String, Object>> trainingData) {
        // Имитация обучения: просто логируем вызов
        System.out.println("[MockAIFatiguePredictionModel] Имитация обучения на " + trainingData.size() + " примерах.");
        // Можно добавить небольшое случайное изменение весов для имитации
        for (String key : weights.keySet()) {
            float delta = (random.nextFloat() - 0.5f) * 0.01f;
            weights.put(key, Math.max(0f, Math.min(1f, weights.get(key) + delta)));
        }
    }

    // Оценка времени до высокого риска (очень упрощённо)
    private int estimateTimeToHighRisk(float probability, Map<String, Float> features) {
        if (probability >= 0.6f) return 0;
        float rate = 0.01f + features.getOrDefault("drowsyEvents", 0.01f) + features.getOrDefault("drivingDuration", 0.01f);
        float delta = 0.6f - probability;
        int minutes = (int) Math.ceil(delta / rate * 10); // чем выше rate, тем быстрее нарастает риск
        return Math.max(1, Math.min(minutes, 120));
    }
}
