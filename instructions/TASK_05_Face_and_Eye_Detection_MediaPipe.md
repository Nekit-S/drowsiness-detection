# TASK_05_Face_and_Eye_Detection_MediaPipe.txt

# Task Title
Implement AI-Based Face and Eye Detection using MediaPipe

---

# Goal
Интегрировать компоненты искусственного интеллекта для:
- Обнаружения лица водителя с помощью MediaPipe Face Mesh
- Определения ключевых точек лица и глаз
- Расчета параметра EAR (Eye Aspect Ratio) для обнаружения сонливости
- Определения состояний водителя: Нормальное, Отвлеченное, Сонливое
- Логирования состояний в контексте текущей сессии вождения

---

# Why This Task Is Important
- Это ключевая задача для критерия "Использование ИИ" (до 10 баллов)
- Без этого компонента система не сможет точно определять состояние водителя
- ИИ-модели обеспечивают более точное распознавание, чем традиционные алгоритмы
- Интеграция с системой сессий обеспечивает контекст для анализа данных

---

# Prerequisites
Before starting this task:
- Complete `TASK_02_Create_Entities_and_Repositories.txt` which includes DriverState enum.
- Complete `TASK_03_Create_Driver_Screen.txt` with session management.
- Review `CODING_STANDARDS.txt`.

---

# Detailed Instructions

## Step 1: Обновите шаблон driver_monitoring.html
Добавьте необходимые скрипты MediaPipe в HTML шаблон и создайте структуру для обработки видео:

```html
<!-- Подключение библиотек MediaPipe -->
<script src="https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh/face_mesh.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@mediapipe/camera_utils/camera_utils.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@mediapipe/drawing_utils/drawing_utils.js"></script>

<!-- Контейнер с видео и canvas для отрисовки -->
<div class="video-container">
    <video id="videoElement" width="640" height="480" autoplay></video>
    <canvas id="outputCanvas" width="640" height="480"></canvas>
    <div id="notificationArea"></div>
    <div id="statusIndicator" class="status-indicator bg-success text-white">
        Status: Normal
    </div>
</div>
```

## Step 2: Создайте REST API для приема событий от фронтенда
Реализуйте контроллер для получения и логирования событий:

- Package: `com.driver_monitoring.controller`
- File: `FaceDetectionController.java`

```java
// What is this file?
// Controller for receiving face detection events from the client-side MediaPipe.
// Why is this needed?
// To log detection events from JavaScript-based MediaPipe to our Java backend.

package com.driver_monitoring.controller;

import com.driver_monitoring.model.DriverState;
import com.driver_monitoring.service.EventLoggingService;
import com.driver_monitoring.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class FaceDetectionController {

    @Autowired
    private EventLoggingService eventLoggingService;

    @Autowired
    private SessionService sessionService;

    @PostMapping("/api/detection-event")
    public ResponseEntity<?> logDetectionEvent(@RequestBody Map<String, Object> eventData) {
        // Получаем данные из запроса
        String driverId = (String) eventData.get("driverId");
        String stateStr = (String) eventData.get("state");
        Double duration = (Double) eventData.get("duration");
        
        // Проверяем активную сессию
        if (sessionService.getActiveSession(driverId) == null) {
            return ResponseEntity.badRequest().body("No active session");
        }
        
        // Конвертируем состояние в DriverState
        DriverState state;
        try {
            state = DriverState.valueOf(stateStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid state");
        }
        
        // Только логируем DROWSY и DISTRACTED события
        if (state != DriverState.NORMAL) {
            // Получаем метаданные
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
            
            // Логируем событие
            eventLoggingService.logEventWithMetadata(
                driverId, 
                state, 
                duration.floatValue(), 
                metadata
            );
        }
        
        return ResponseEntity.ok().build();
    }
}
```

## Step 3: Добавьте JavaScript код для MediaPipe
Добавьте JavaScript-код в шаблон driver_monitoring.html для обработки видеопотока:

```javascript
// Настройки и пороговые значения
const EAR_THRESHOLD = 0.2;     // Порог для определения закрытого глаза
const DROWSY_TIME = 2000;      // 2 секунды для определения сонливости
const FACE_MESH_CONFIG = {
    maxNumFaces: 1,
    refineLandmarks: true,
    minDetectionConfidence: 0.5,
    minTrackingConfidence: 0.5
};

// Состояние
let lastEyeCloseTime = null;
let currentState = 'NORMAL';
let faceMesh;
let camera;

// Получаем элементы DOM
const video = document.getElementById('videoElement');
const canvas = document.getElementById('outputCanvas');
const ctx = canvas.getContext('2d');
const driverId = document.getElementById('driverId').value;
const sessionId = document.getElementById('sessionId').value;

// Инициализация FaceMesh
function setupFaceMesh() {
    faceMesh = new FaceMesh({locateFile: (file) => {
        return `https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh/${file}`;
    }});
    
    faceMesh.setOptions(FACE_MESH_CONFIG);
    
    faceMesh.onResults(onResults);
    
    // Запускаем камеру
    camera = new Camera(video, {
        onFrame: async () => {
            await faceMesh.send({image: video});
        },
        width: 640,
        height: 480
    });
    
    camera.start();
}

// Обработка результатов FaceMesh
function onResults(results) {
    // Очищаем канвас
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Рисуем изображение с камеры
    ctx.drawImage(results.image, 0, 0, canvas.width, canvas.height);
    
    if (results.multiFaceLandmarks && results.multiFaceLandmarks.length > 0) {
        // Лицо обнаружено
        const landmarks = results.multiFaceLandmarks[0];
        
        // Отрисовываем ключевые точки лица (опционально)
        drawConnectors(ctx, landmarks, FACEMESH_TESSELATION, 
                       {color: '#C0C0C070', lineWidth: 1});
        
        // Рассчитываем EAR для определения сонливости
        const leftEAR = calculateEAR(landmarks, LEFT_EYE_INDICES);
        const rightEAR = calculateEAR(landmarks, RIGHT_EYE_INDICES);
        const avgEAR = (leftEAR + rightEAR) / 2;
        
        // Проверяем, закрыты ли глаза
        if (avgEAR < EAR_THRESHOLD) {
            // Глаза закрыты
            if (lastEyeCloseTime === null) {
                lastEyeCloseTime = Date.now();
            } else {
                const closeDuration = Date.now() - lastEyeCloseTime;
                
                // Проверяем продолжительность закрытия глаз
                if (closeDuration > DROWSY_TIME) {
                    // Водитель сонный, если глаза закрыты больше порогового времени
                    updateDriverState('DROWSY', closeDuration / 1000, {
                        earValue: avgEAR,
                        closeDuration: closeDuration,
                        leftEAR: leftEAR,
                        rightEAR: rightEAR
                    });
                }
            }
        } else {
            // Глаза открыты
            lastEyeCloseTime = null;
            
            // Если предыдущее состояние было не NORMAL, обновляем на NORMAL
            if (currentState !== 'NORMAL') {
                updateDriverState('NORMAL', 0, {});
            }
        }
    } else {
        // Лицо не обнаружено - водитель отвлечен
        updateDriverState('DISTRACTED', 1.0, {
            faceDetected: false,
            timestamp: Date.now()
        });
    }
}

// Рассчитывает Eye Aspect Ratio для определения закрытости глаз
function calculateEAR(landmarks, eyeIndices) {
    // Получаем координаты ключевых точек глаза
    const eyePoints = eyeIndices.map(index => landmarks[index]);
    
    // Вертикальные расстояния
    const height1 = distance(eyePoints[1], eyePoints[5]);
    const height2 = distance(eyePoints[2], eyePoints[4]);
    
    // Горизонтальное расстояние
    const width = distance(eyePoints[0], eyePoints[3]);
    
    // Рассчитываем EAR
    return (height1 + height2) / (2 * width);
}

// Евклидово расстояние между двумя точками
function distance(p1, p2) {
    return Math.sqrt(
        Math.pow(p2.x - p1.x, 2) + 
        Math.pow(p2.y - p1.y, 2)
    );
}

// Обновляет состояние водителя и отправляет данные на сервер
function updateDriverState(state, duration, metadata) {
    // Обновляем только при изменении состояния
    if (currentState !== state) {
        currentState = state;
        
        // Обновляем UI
        updateUI(state);
        
        // Если состояние не NORMAL, отправляем событие на сервер
        if (state !== 'NORMAL') {
            sendEventToServer(state, duration, metadata);
        }
    }
}

// Отправляет событие на сервер для логирования
function sendEventToServer(state, duration, metadata) {
    fetch('/api/detection-event', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            driverId: driverId,
            sessionId: sessionId,
            state: state,
            duration: duration,
            metadata: {
                ...metadata,
                timestamp: Date.now()
            }
        })
    }).catch(error => console.error('Error sending event:', error));
}

// Индексы ключевых точек для глаз в MediaPipe Face Mesh
// Левый глаз: точки 362-386
// Правый глаз: точки 133-157
const LEFT_EYE_INDICES = [362, 385, 387, 263, 373, 380];  // Внешние точки левого глаза
const RIGHT_EYE_INDICES = [33, 160, 158, 133, 153, 144];  // Внешние точки правого глаза

// Запуск системы при загрузке страницы
document.addEventListener('DOMContentLoaded', setupFaceMesh);
```

## Step 4: Добавьте функции обновления UI
Добавьте в JavaScript код функции для обновления UI при изменении состояния:

```javascript
// Обновляет UI на основе состояния водителя
function updateUI(state) {
    switch(state) {
        case 'DROWSY':
            showDrowsyNotification();
            break;
        case 'DISTRACTED':
            showDistractedNotification();
            break;
        case 'NORMAL':
        default:
            showNormalNotification();
            break;
    }
}

// Показывает уведомление о нормальном состоянии
function showNormalNotification() {
    document.getElementById('notificationArea').innerHTML = `
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <strong>Status:</strong> Normal
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>`;
    
    document.getElementById('statusIndicator').className = 'status-indicator bg-success text-white';
    document.getElementById('statusIndicator').textContent = 'Status: Normal';
}

// Показывает уведомление о отвлечении
function showDistractedNotification() {
    document.getElementById('notificationArea').innerHTML = `
        <div class="alert alert-warning alert-dismissible fade show" role="alert">
            <strong>Warning:</strong> Eyes on the road! You appear distracted.
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>`;
    
    document.getElementById('statusIndicator').className = 'status-indicator bg-warning text-dark';
    document.getElementById('statusIndicator').textContent = 'Status: Distracted';
    
    // Воспроизводим звук уведомления (если звуки настроены)
    playAlertSound('distracted');
}

// Показывает уведомление о сонливости
function showDrowsyNotification() {
    document.getElementById('notificationArea').innerHTML = `
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <strong>Warning:</strong> You appear drowsy! Consider taking a break.
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>`;
    
    document.getElementById('statusIndicator').className = 'status-indicator bg-danger text-white';
    document.getElementById('statusIndicator').textContent = 'Status: Drowsy';
    
    // Воспроизводим звук уведомления (если звуки настроены)
    playAlertSound('drowsy');
}

// Воспроизводит звук уведомления
function playAlertSound(type) {
    try {
        const sound = type === 'drowsy' ? 'drowsy_alert.mp3' : 'distracted_alert.mp3';
        const audio = new Audio('/sounds/' + sound);
        audio.volume = 0.7;
        audio.play().catch(e => console.log('Could not play alert sound:', e));
    } catch(e) {
        console.log('Sound playback not supported');
    }
}
```

## Step 5: Добавьте стили для MediaPipe визуализации
Добавьте CSS стили для корректного отображения Canvas и видео:

```css
.video-container {
    position: relative;
    width: 640px;
    height: 480px;
    margin: 0 auto;
    border: 1px solid #ddd;
    border-radius: 5px;
    overflow: hidden;
}

#videoElement {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
}

#outputCanvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 2;
}

#notificationArea {
    position: absolute;
    top: 10px;
    left: 10px;
    right: 10px;
    z-index: 10;
}

.status-indicator {
    position: absolute;
    bottom: 10px;
    left: 10px;
    right: 10px;
    padding: 5px;
    border-radius: 5px;
    text-align: center;
    font-weight: bold;
    z-index: 10;
}
```

## Step 6: Создание Basic Event DTO
- Package: `com.driver_monitoring.dto`
- File: `ClientEventDTO.java`

```java
// What is this file?
// Data Transfer Object for client-side detection events.
// Why is this needed?
// It structures incoming JSON data from MediaPipe processing.

package com.driver_monitoring.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ClientEventDTO {
    private String driverId;
    private Long sessionId;
    private String state;
    private Float duration;
    private Map<String, Object> metadata;
}
```

---

# Preventing Common Errors

## Доступ к камере
- **Проверьте разрешения**: Браузеры требуют разрешение на доступ к камере
- **Используйте HTTPS**: Для доступа к камере в продакшене нужен HTTPS
- **Обработайте ошибки**: Обязательно обрабатывайте случаи, когда камера недоступна

```javascript
// Правильная обработка ошибок камеры
camera.start().catch(err => {
    console.error("Ошибка запуска камеры:", err);
    // Показать сообщение пользователю
    document.getElementById('notificationArea').innerHTML = `
        <div class="alert alert-danger">
            Не удалось получить доступ к камере. Проверьте разрешения браузера.
        </div>`;
});
```

## Производительность
- **Уменьшите разрешение**: Используйте небольшое разрешение видео (640x480)
- **Ограничьте частоту кадров**: При необходимости ограничьте FPS
- **Отключайте анализ** при неактивном окне браузера для экономии ресурсов

```javascript
// Оптимизация производительности
document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        // Страница скрыта - приостанавливаем обработку
        if (camera) camera.stop();
    } else {
        // Страница активна - возобновляем обработку
        if (camera) camera.start();
    }
});
```

## Кросс-браузерная совместимость
- **Проверяйте поддержку**: Не все браузеры поддерживают MediaPipe и getUserMedia
- **Добавьте резервные варианты**: Например, загрузку локального видео для демо

```javascript
// Проверка поддержки браузером
function checkBrowserSupport() {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        document.getElementById('notificationArea').innerHTML = `
            <div class="alert alert-danger">
                Ваш браузер не поддерживает доступ к камере. 
                Пожалуйста, используйте Chrome, Firefox или Edge.
            </div>`;
        return false;
    }
    return true;
}
```

---

# Important Details
- MediaPipe Face Mesh обнаруживает 468 ключевых точек лица
- Для глаз используются конкретные точки (указаны в коде)
- EAR рассчитывается как отношение высоты глаза к его ширине
- События о состоянии отправляются на сервер только при изменении состояния
- Аналитика проводится в браузере, логирование - на сервере

---

# Troubleshooting Tips
1. **Проблемы с загрузкой библиотек**:
   - Проверьте сетевое подключение
   - Убедитесь, что ссылки на CDN актуальны
   - При необходимости скачайте библиотеки локально

2. **Камера не запускается**:
   - Проверьте разрешения браузера
   - Проверьте подключение камеры
   - Используйте резервный метод с загрузкой видео

3. **Низкая производительность**:
   - Уменьшите разрешение видео
   - Снизьте частоту обновления (FPS)
   - Используйте более легкую конфигурацию MediaPipe

4. **Ошибки при отправке событий**:
   - Проверьте формат отправляемых данных
   - Убедитесь, что серверные эндпоинты доступны
   - Добавьте повторные попытки для отправки данных

5. **Неточное определение состояния**:
   - Настройте порог EAR для вашего использования
   - Увеличьте время для определения сонливости
   - Используйте сглаживание значений EAR для уменьшения ложных срабатываний

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Clear comments explaining алгоритмы обнаружения
- Правильное обращение с ресурсами браузера
- Обработка всех возможных ошибок

---

# Success Criteria
- Face Mesh успешно отслеживает лицо и глаза в реальном времени
- Система точно определяет состояния: Нормальное, Отвлеченное, Сонливое
- События корректно отправляются на сервер и логируются
- Интерфейс обновляется в соответствии с состоянием водителя
- Код оптимизирован для работы в браузере
- Система работает стабильно в течение длительного времени
- Обработка всех возможных ошибок (нет камеры, камера не работает и т.д.)

---

# References
- [MediaPipe Face Mesh](https://developers.google.com/mediapipe/solutions/vision/face_landmarker)
- [MediaPipe JavaScript API](https://developers.google.com/mediapipe/solutions/vision/face_landmarker/web_js)
- [Eye Aspect Ratio](https://www.pyimagesearch.com/2017/04/24/eye-blink-detection-opencv-python-dlib/)
- [Camera API](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia)

---

# End of TASK_05_Face_and_Eye_Detection_MediaPipe.txt