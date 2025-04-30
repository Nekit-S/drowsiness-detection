package com.drivermonitoring.ai;

import com.drivermonitoring.ai.FatiguePrediction;
import java.util.List;
import java.util.Map;

public interface PredictionModel { // TODO: Здесь легко подключить обученную ML/AI модель вместо rule-based логики
    FatiguePrediction predict(Map<String, Float> features);
    void train(List<Map<String, Object>> trainingData); // Имитация обучения
}
