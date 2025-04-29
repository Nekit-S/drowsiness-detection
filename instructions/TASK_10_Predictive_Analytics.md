# TASK_10_Predictive_Analytics.txt

# Task Title
Implement Predictive Fatigue Analytics (AFTER MVP)

---

# Goal
Разработать сервис предиктивной аналитики, который:
- Анализирует предыдущие события водителя для прогнозирования будущей усталости
- Рассчитывает факторы риска (время суток, продолжительность вождения и т.д.)
- Предоставляет рекомендации по предотвращению усталости
- Визуализирует риск усталости на панели диспетчера

---

# Why This Task Is Important
- Повышает оценку по критерию использования ИИ
- Превращает систему из реактивной в проактивную
- Добавляет ценность для транспортных компаний (бизнес-аспект)
- Повышает безопасность, предупреждая усталость до её наступления

---

# Prerequisites
Before starting this task:
- Complete all core MVP tasks (TASK_01 through TASK_09, except TASK_08).
- Ensure you have a working Event logging system.
- Have a working dispatcher panel.

---

# Detailed Instructions

## Step 1: Create Supporting Classes

### FatiguePrediction.java
- Package: `com.driver_monitoring.model`

```java
// What is this file?
// A model class representing the predicted fatigue state for a driver.
// Why is this needed?
// It structures the output of the prediction model for easy consumption by the UI.

package com.driver_monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FatiguePrediction {
    
    private RiskLevel riskLevel;
    private float probability;
    private int minutesUntilHigh;
    private String recommendation;
    
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
```

### RiskFactor.java
- Package: `com.driver_monitoring.model`

```java
// What is this file?
// A model class for individual fatigue risk factors calculated from driver events.
// Why is this needed?
// It helps identify specific causes of fatigue for more detailed analytics.

package com.driver_monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskFactor {
    
    private String name;
    private float value;
    private float contribution; // % contribution to overall risk
    
    public static final String BLINK_FREQUENCY = "blinkFrequency";
    public static final String DROWSY_EVENTS = "drowsyEvents";
    public static final String DRIVING_DURATION = "drivingDuration";
    public static final String TIME_OF_DAY = "timeOfDay";
    public static final String DISTRACTION_FREQUENCY = "distractionFrequency";
}
```

## Step 2: Create PredictiveFatigueService Interface
- Package: `com.driver_monitoring.service`

```java
// What is this file?
// This service defines the contract for predicting future driver fatigue based on historical data.
// Why is this needed?
// It enables proactive warnings before critical fatigue occurs, enhancing safety.

package com.driver_monitoring.service;

import com.driver_monitoring.model.FatiguePrediction;
import com.driver_monitoring.model.RiskFactor;
import java.util.List;

public interface PredictiveFatigueService {

    /**
     * Предсказывает вероятность усталости для водителя в ближайшие 15-30 минут
     * @param driverId ID водителя
     * @return FatiguePrediction содержащий уровень риска и вероятность
     */
    FatiguePrediction predictFatigueRisk(String driverId);
    
    /**
     * Рассчитывает факторы риска усталости на основе недавних событий
     * @param driverId ID водителя
     * @return Список факторов риска и их значений
     */
    List<RiskFactor> calculateRiskFactors(String driverId);
}
```

## Step 3: Implement PredictiveFatigueServiceImpl
- Package: `com.driver_monitoring.service`

```java
// What is this file?
// Implementation of the predictive analytics service using a rule-based approach.
// Why is this needed?
// It processes historical driver data to predict fatigue before it becomes critical.

package com.driver_monitoring.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.driver_monitoring.model.Event;
import com.driver_monitoring.model.FatiguePrediction;
import com.driver_monitoring.model.RiskFactor;
import com.driver_monitoring.model.FatiguePrediction.RiskLevel;
import com.driver_monitoring.repository.EventRepository;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class PredictiveFatigueServiceImpl implements PredictiveFatigueService {

    @Autowired
    private EventRepository eventRepository;
    
    // Весовые коэффициенты для разных факторов риска
    private Map<String, Float> riskFactorWeights;
    
    @PostConstruct
    public void init() {
        // Инициализация весов
        riskFactorWeights = new HashMap<>();
        riskFactorWeights.put(RiskFactor.BLINK_FREQUENCY, 0.25f);
        riskFactorWeights.put(RiskFactor.DROWSY_EVENTS, 0.30f);
        riskFactorWeights.put(RiskFactor.DRIVING_DURATION, 0.20f);
        riskFactorWeights.put(RiskFactor.TIME_OF_DAY, 0.15f);
        riskFactorWeights.put(RiskFactor.DISTRACTION_FREQUENCY, 0.10f);
    }
    
    @Override
    public FatiguePrediction predictFatigueRisk(String driverId) {
        // Рассчитываем факторы риска
        List<RiskFactor> factors = calculateRiskFactors(driverId);
        
        // Рассчитываем взвешенную вероятность усталости
        float totalRisk = 0.0f;
        for (RiskFactor factor : factors) {
            float weight = riskFactorWeights.getOrDefault(factor.getName(), 0.0f);
            totalRisk += factor.getValue() * weight;
        }
        
        // Масштабируем до вероятности (0-1)
        float probability = Math.min(1.0f, Math.max(0.0f, totalRisk));
        
        // Определяем уровень риска
        RiskLevel level;
        String recommendation;
        int minutesUntilHigh = 0;
        
        if (probability < 0.3f) {
            level = RiskLevel.LOW;
            recommendation = "Нет необходимости в действиях";
            minutesUntilHigh = estimateTimeToHighRisk(probability, factors);
        } else if (probability < 0.6f) {
            level = RiskLevel.MEDIUM;
            recommendation = "Рекомендуется сделать перерыв в течение 30 минут";
            minutesUntilHigh = estimateTimeToHighRisk(probability, factors);
        } else {
            level = RiskLevel.HIGH;
            recommendation = "Рекомендуется немедленный перерыв";
            minutesUntilHigh = 0;
        }
        
        return new FatiguePrediction(level, probability, minutesUntilHigh, recommendation);
    }
    
    @Override
    public List<RiskFactor> calculateRiskFactors(String driverId) {
        List<RiskFactor> factors = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        
        // Получаем недавние события для этого водителя
        List<Event> recentEvents = eventRepository.findByDriverId(driverId).stream()
                .filter(e -> e.getTimestamp().isAfter(oneHourAgo))
                .collect(Collectors.toList());
        
        // Если нет недавних событий, возвращаем факторы с низким риском
        if (recentEvents.isEmpty()) {
            factors.add(new RiskFactor(RiskFactor.BLINK_FREQUENCY, 0.1f, 0.25f));
            factors.add(new RiskFactor(RiskFactor.DROWSY_EVENTS, 0.0f, 0.30f));
            factors.add(new RiskFactor(RiskFactor.DRIVING_DURATION, 0.2f, 0.20f));
            factors.add(new RiskFactor(RiskFactor.TIME_OF_DAY, calculateTimeOfDayRisk(now), 0.15f));
            factors.add(new RiskFactor(RiskFactor.DISTRACTION_FREQUENCY, 0.1f, 0.10f));
            return factors;
        }
        
        // Подсчитываем количество событий каждого типа
        int drowsyCount = 0;
        int distractedCount = 0;
        
        for (Event event : recentEvents) {
            if ("DROWSY".equals(event.getEventType())) {
                drowsyCount++;
            } else if ("DISTRACTED".equals(event.getEventType())) {
                distractedCount++;
            }
        }
        
        // Рассчитываем каждый фактор риска
        
        // 1. Риск сонливости - больше событий = выше риск
        float drowsyRisk = Math.min(1.0f, drowsyCount / 5.0f); // 5+ событий = макс. риск
        factors.add(new RiskFactor(RiskFactor.DROWSY_EVENTS, drowsyRisk, 0.30f));
        
        // 2. Риск отвлечения
        float distractionRisk = Math.min(1.0f, distractedCount / 10.0f); // 10+ событий = макс. риск
        factors.add(new RiskFactor(RiskFactor.DISTRACTION_FREQUENCY, distractionRisk, 0.10f));
        
        // 3. Риск частоты моргания (упрощенно - коррелирует с сонливостью)
        float blinkRisk = drowsyRisk * 0.8f + 0.1f;
        factors.add(new RiskFactor(RiskFactor.BLINK_FREQUENCY, blinkRisk, 0.25f));
        
        // 4. Риск продолжительности вождения
        LocalDateTime firstEvent = recentEvents.stream()
                .map(Event::getTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(oneHourAgo);
                
        long drivingMinutes = ChronoUnit.MINUTES.between(firstEvent, now);
        float durationRisk = Math.min(1.0f, drivingMinutes / 240.0f); // 4+ часа = макс. риск
        factors.add(new RiskFactor(RiskFactor.DRIVING_DURATION, durationRisk, 0.20f));
        
        // 5. Риск времени суток
        float timeRisk = calculateTimeOfDayRisk(now);
        factors.add(new RiskFactor(RiskFactor.TIME_OF_DAY, timeRisk, 0.15f));
        
        return factors;
    }
    
    /**
     * Оценивает количество минут до достижения водителем высокого риска
     * Простая линейная экстраполяция на основе текущего уровня риска
     */
    private int estimateTimeToHighRisk(float currentProbability, List<RiskFactor> factors) {
        // Простая линейная проекция
        if (currentProbability >= 0.6f) return 0;
        
        // Получаем фактор продолжительности вождения
        float durationFactor = factors.stream()
                .filter(f -> RiskFactor.DRIVING_DURATION.equals(f.getName()))
                .findFirst()
                .map(RiskFactor::getValue)
                .orElse(0.0f);
        
        // Рассчитываем скорость увеличения (упрощенно)
        float rateOfIncrease = 0.05f + (durationFactor * 0.05f);
        
        // Оцениваем минуты до достижения высокого риска (0.6)
        float remainingRisk = 0.6f - currentProbability;
        return Math.max(10, Math.round(remainingRisk / rateOfIncrease * 60));
    }
    
    /**
     * Рассчитывает риск на основе времени суток (циркадные ритмы)
     * Наивысший риск: 2-6 утра и 14-16 дня
     */
    private float calculateTimeOfDayRisk(LocalDateTime time) {
        int hour = time.getHour();
        
        // Ночное вождение (2:00 - 6:00) - наивысший риск
        if (hour >= 2 && hour < 6) {
            return 1.0f;
        }
        
        // Дневной спад (14:00 - 16:00) - высокий риск
        if (hour >= 14 && hour < 16) {
            return 0.7f;
        }
        
        // Вечер (20:00 - 2:00) - средний риск
        if (hour >= 20 || hour < 2) {
            return 0.5f;
        }
        
        // Утро (6:00 - 10:00) - низкий риск
        if (hour >= 6 && hour < 10) {
            return 0.2f;
        }
        
        // Другое время - минимальный риск
        return 0.1f;
    }
}
```

## Step 4: Update Dispatcher Controller
- Update `DispatcherController.java` to include the predictive service:

```java
@Autowired
private PredictiveFatigueService predictiveFatigueService;

@GetMapping("/dispatcher/driver/{driverId}/prediction")
public String driverPrediction(@PathVariable String driverId, Model model) {
    Driver driver = driverRepository.findById(driverId).orElse(null);
    FatiguePrediction prediction = predictiveFatigueService.predictFatigueRisk(driverId);
    List<RiskFactor> riskFactors = predictiveFatigueService.calculateRiskFactors(driverId);
    
    model.addAttribute("driver", driver);
    model.addAttribute("prediction", prediction);
    model.addAttribute("riskFactors", riskFactors);
    return "driver_prediction";
}
```

## Step 5: Create Prediction View Template
- File: `src/main/resources/templates/driver_prediction.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Predictive Analytics</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <div class="container mt-4">
        <h1>Прогноз усталости водителя</h1>
        <h3 th:text="${driver.driverName + ' (ID: ' + driver.driverId + ')'}"></h3>
        
        <!-- Карточка с прогнозом -->
        <div class="card mb-4" th:classappend="${
            prediction.riskLevel.name() == 'LOW' ? 'border-success' : 
            prediction.riskLevel.name() == 'MEDIUM' ? 'border-warning' : 'border-danger'
        }">
            <div class="card-header" th:classappend="${
                prediction.riskLevel.name() == 'LOW' ? 'bg-success text-white' : 
                prediction.riskLevel.name() == 'MEDIUM' ? 'bg-warning' : 'bg-danger text-white'
            }">
                <h5 class="mb-0">
                    Уровень риска: 
                    <span th:text="${
                        prediction.riskLevel.name() == 'LOW' ? 'Низкий' : 
                        prediction.riskLevel.name() == 'MEDIUM' ? 'Средний' : 'Высокий'
                    }"></span>
                </h5>
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-md-6">
                        <h5>Вероятность усталости: <span th:text="${#numbers.formatPercent(prediction.probability, 1, 0)}"></span></h5>
                        <p th:if="${prediction.minutesUntilHigh > 0}">
                            Прогнозируемое время до высокого риска: <strong th:text="${prediction.minutesUntilHigh}"></strong> минут
                        </p>
                        <p class="mt-3">
                            <strong>Рекомендация:</strong> <span th:text="${prediction.recommendation}"></span>
                        </p>
                    </div>
                    <div class="col-md-6">
                        <!-- Визуализация вероятности -->
                        <canvas id="riskGauge"></canvas>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Факторы риска -->
        <div class="card mb-4">
            <div class="card-header">
                <h5 class="mb-0">Факторы риска</h5>
            </div>
            <div class="card-body">
                <canvas id="riskFactorsChart"></canvas>
            </div>
        </div>
        
        <a href="/dispatcher" class="btn btn-primary">Назад к списку водителей</a>
    </div>
    
    <script th:inline="javascript">
        // Данные из модели
        const prediction = [[${prediction}]];
        const riskFactors = [[${riskFactors}]];
        
        // Инициализация графиков при загрузке страницы
        document.addEventListener('DOMContentLoaded', function() {
            // График-шкала вероятности
            const gaugeCtx = document.getElementById('riskGauge').getContext('2d');
            new Chart(gaugeCtx, {
                type: 'doughnut',
                data: {
                    datasets: [{
                        data: [prediction.probability, 1 - prediction.probability],
                        backgroundColor: [
                            prediction.riskLevel === 'LOW' ? '#28a745' : 
                            prediction.riskLevel === 'MEDIUM' ? '#ffc107' : '#dc3545',
                            '#e9ecef'
                        ]
                    }]
                },
                options: {
                    circumference: 180,
                    rotation: 270,
                    cutout: '80%',
                    plugins: {
                        tooltip: { enabled: false },
                        legend: { display: false }
                    }
                }
            });
            
            // График факторов риска
            const factorsCtx = document.getElementById('riskFactorsChart').getContext('2d');
            const factorLabels = riskFactors.map(f => {
                switch(f.name) {
                    case 'blinkFrequency': return 'Частота моргания';
                    case 'drowsyEvents': return 'События сонливости';
                    case 'drivingDuration': return 'Продолжительность вождения';
                    case 'timeOfDay': return 'Время суток';
                    case 'distractionFrequency': return 'Частота отвлечений';
                    default: return f.name;
                }
            });
            
            new Chart(factorsCtx, {
                type: 'bar',
                data: {
                    labels: factorLabels,
                    datasets: [{
                        label: 'Уровень риска',
                        data: riskFactors.map(f => f.value),
                        backgroundColor: riskFactors.map(f => {
                            if (f.value < 0.3) return 'rgba(40, 167, 69, 0.7)';
                            if (f.value < 0.6) return 'rgba(255, 193, 7, 0.7)';
                            return 'rgba(220, 53, 69, 0.7)';
                        }),
                        borderColor: riskFactors.map(f => {
                            if (f.value < 0.3) return '#28a745';
                            if (f.value < 0.6) return '#ffc107';
                            return '#dc3545';
                        }),
                        borderWidth: 1
                    }]
                },
                options: {
                    scales: {
                        y: {
                            beginAtZero: true,
                            max: 1,
                            ticks: {
                                callback: function(value) {
                                    return (value * 100) + '%';
                                }
                            }
                        }
                    }
                }
            });
        });
    </script>
</body>
</html>
```

## Step 6: Add Link to Dispatcher Panel
- Update `src/main/resources/templates/dispatcher_panel.html`:

```html
<!-- Добавить кнопку прогноза для каждого водителя -->
<div class="btn-group" role="group">
    <a th:href="@{'/dispatcher/driver/' + ${driver.driverId}}" class="btn btn-primary">Статистика</a>
    <a th:href="@{'/dispatcher/driver/' + ${driver.driverId} + '/prediction'}" class="btn btn-info">Прогноз</a>
</div>
```

---

# Important Details
- Эта задача предназначена для реализации **после** завершения основного MVP
- В реальной системе можно использовать более сложные алгоритмы машинного обучения
- Время суток важно для прогнозирования усталости (циркадные ритмы)
- Анализ учитывает как недавние события, так и продолжительность вождения

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Clear comments explaining подход к прогнозированию и факторы риска
- Логичная организация методов
- Простая и понятная структура кода

---

# Success Criteria
- Сервис прогнозирования рассчитывает риск усталости на основе нескольких факторов
- Факторы риска показывают осмысленный вклад в общий риск
- Интерфейс четко отображает результаты прогнозирования и рекомендации
- Система предоставляет заблаговременные предупреждения на основе прогнозов
- Код простой, чистый и правильно прокомментирован
- Улучшенный компонент ИИ для повышения оценки на хакатоне

---

# References
- [Исследования усталости водителей](https://www.fmcsa.dot.gov/research-and-analysis/research/commercial-motor-vehicledriver-fatigue-alertness-and-countermeasures)
- [Циркадные ритмы и усталость](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2605299/)

---

# End of TASK_10_Predictive_Analytics.txt
