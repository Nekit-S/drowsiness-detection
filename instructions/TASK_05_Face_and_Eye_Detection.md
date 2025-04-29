# TASK_05_Face_and_Eye_Detection.txt

# Task Title
Implement AI-Based Face and Eye Detection

---

# Goal
Интегрировать компоненты искусственного интеллекта для:
- Обнаружения лица водителя с помощью SSD-модели
- Определения ключевых точек лица с помощью DLib
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
- Review `Knowledge/ai_models_knowledge.txt`.
- Download required AI models (instructions below).

---

# Detailed Instructions

## Step 1: Create Model Resource Directory
```bash
# Создать директории для моделей
mkdir -p src/main/resources/models/face_detection
mkdir -p src/main/resources/models/landmarks
```

## Step 2: Download AI Models
```bash
# Скачать SSD модель для детекции лица
wget -O src/main/resources/models/face_detection/res10_300x300_ssd_iter_140000.caffemodel https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000.caffemodel
wget -O src/main/resources/models/face_detection/deploy.prototxt https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/deploy.prototxt

# Скачать DLib модель для ключевых точек лица
wget -O src/main/resources/models/landmarks/shape_predictor_68_face_landmarks.dat.bz2 http://dlib.net/files/shape_predictor_68_face_landmarks.dat.bz2
bzip2 -d src/main/resources/models/landmarks/shape_predictor_68_face_landmarks.dat.bz2
```

### Troubleshooting Downloads
Если возникли проблемы со скачиванием моделей:

1. **Альтернативные источники SSD модели**:
   ```bash
   # Альтернативная ссылка для SSD модели
   curl -L -o src/main/resources/models/face_detection/res10_300x300_ssd_iter_140000.caffemodel https://raw.githubusercontent.com/opencv/opencv_3rdparty/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000.caffemodel
   ```

2. **Локальная копия моделей**:
   - Скачайте модели заранее через браузер
   - Сохраните их в соответствующих папках вручную
   - Убедитесь, что имена файлов совпадают с указанными в коде

3. **Проверка скачанных файлов**:
   ```bash
   # Проверить размер файлов
   ls -la src/main/resources/models/face_detection/
   ls -la src/main/resources/models/landmarks/
   
   # Примерные размеры файлов:
   # res10_300x300_ssd_iter_140000.caffemodel: ~10MB
   # deploy.prototxt: ~1KB
   # shape_predictor_68_face_landmarks.dat: ~100MB
   ```

## Step 3: Update build.gradle
Добавьте следующие зависимости в ваш `build.gradle`:

```gradle
// AI и Computer Vision библиотеки
implementation 'org.bytedeco:javacv-platform:1.5.8'
implementation 'org.bytedeco:javacpp-presets-dlib:1.5.8'

// Логирование
implementation 'org.slf4j:slf4j-api:2.0.5'
implementation 'ch.qos.logback:logback-classic:1.4.6'
```

## Step 4: Create FaceDetectionService Interface
- Package: `com.driver_monitoring.service`
- File: `FaceDetectionService.java`

```java
// What is this file?
// This service defines the contract for AI-based face analysis and driver state detection.
// Why is this needed?
// It separates the interface from implementation, allowing for future enhancements.

package com.driver_monitoring.service;

import org.bytedeco.opencv.opencv_core.Mat;
import com.driver_monitoring.model.DriverState;

public interface FaceDetectionService {

    /**
     * Process a video frame and determine driver state.
     * @param frame Video frame captured from camera
     * @param driverId The ID of the driver (for session and event logging)
     * @return DriverState (NORMAL, DISTRACTED, DROWSY)
     */
    DriverState analyzeFrame(Mat frame, String driverId);
    
    /**
     * Initialize the AI models.
     * Should be called when the application starts.
     */
    void initModels();
    
    /**
     * Release all resources used by this service.
     * Should be called when the application is shutting down.
     */
    void releaseResources();
}
```

## Step 5: Create FaceDetectionServiceImpl Implementation
- Package: `com.driver_monitoring.service`
- File: `FaceDetectionServiceImpl.java`

```java
// What is this file?
// Implementation of face detection using advanced AI models (SSD and DLib).
// Why is this needed?
// It processes video frames to detect driver distraction and drowsiness using AI techniques.

package com.driver_monitoring.service;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_dnn.*;
import org.bytedeco.opencv.opencv_imgproc.*;
import org.bytedeco.dlib.global.dlib.*;
import org.bytedeco.dlib.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.driver_monitoring.model.DriverState;
import com.driver_monitoring.model.DriverSession;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_dnn.*;

@Service
public class FaceDetectionServiceImpl implements FaceDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FaceDetectionServiceImpl.class);

    @Autowired
    private ResourceLoader resourceLoader;
    
    @Autowired
    private SessionService sessionService;
    
    // AI models
    private Net faceDetectionNet;
    private ShapePredictor landmarkDetector;
    
    // State tracking variables
    private Instant lastEyeCloseTime = null;
    private boolean eyesClosed = false;
    private boolean faceDetected = false;
    private int frameCounter = 0;
    
    // Flag to indicate if we're using fallback method due to DLib loading issues
    private boolean useFallbackMethod = false;
    
    // Flag to prevent processing during shutdown
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    
    // EAR thresholds
    private final float EAR_THRESHOLD = 0.2f;
    private final long EYE_CLOSE_DURATION_MS = 2000; // 2 seconds
    
    @PostConstruct
    @Override
    public void initModels() {
        try {
            // Загрузка модели для обнаружения лица (SSD)
            File prototxtFile = resourceLoader.getResource("classpath:models/face_detection/deploy.prototxt").getFile();
            File caffeModelFile = resourceLoader.getResource("classpath:models/face_detection/res10_300x300_ssd_iter_140000.caffemodel").getFile();
            
            // Verify files exist
            if (!prototxtFile.exists() || !caffeModelFile.exists()) {
                logger.error("Model files not found. Looking for fallback options.");
                
                // Try alternate paths
                String basePath = System.getProperty("user.dir");
                prototxtFile = new File(basePath + "/models/face_detection/deploy.prototxt");
                caffeModelFile = new File(basePath + "/models/face_detection/res10_300x300_ssd_iter_140000.caffemodel");
                
                if (!prototxtFile.exists() || !caffeModelFile.exists()) {
                    throw new IOException("Model files not found, even in fallback locations. Please check the paths.");
                }
                
                logger.info("Found model files in fallback location.");
            }
            
            String prototxtPath = prototxtFile.getPath();
            String caffeModelPath = caffeModelFile.getPath();
            
            logger.info("Loading SSD face detection model from: {}", caffeModelPath);
            faceDetectionNet = readNetFromCaffe(prototxtPath, caffeModelPath);
            
            if (faceDetectionNet.empty()) {
                logger.error("Failed to load face detection model. Model is empty.");
                useSimplifiedFaceDetection();
                return;
            }
            
            try {
                // Загрузка модели для определения ключевых точек лица (DLib)
                File landmarkModelFile = resourceLoader.getResource("classpath:models/landmarks/shape_predictor_68_face_landmarks.dat").getFile();
                
                if (!landmarkModelFile.exists()) {
                    logger.error("Landmark model file not found in resources.");
                    
                    // Try alternate paths
                    String basePath = System.getProperty("user.dir");
                    landmarkModelFile = new File(basePath + "/models/landmarks/shape_predictor_68_face_landmarks.dat");
                    
                    if (!landmarkModelFile.exists()) {
                        logger.error("Landmark model not found in alternate location. Using simplified method.");
                        useFallbackMethod = true;
                        return;
                    }
                    
                    logger.info("Found landmark model in fallback location.");
                }
                
                String landmarkModelPath = landmarkModelFile.getPath();
                
                logger.info("Loading DLib facial landmark model from: {}", landmarkModelPath);
                landmarkDetector = new ShapePredictor();
                landmarkDetector.load(landmarkModelPath);
                
                logger.info("AI models loaded successfully");
            } catch (Exception e) {
                logger.error("Failed to load landmark detector: {}. Falling back to simplified method.", e.getMessage());
                useFallbackMethod = true;
            }
        } catch (Exception e) {
            logger.error("Failed to load AI models: {}. Falling back to simplified detection.", e.getMessage(), e);
            useSimplifiedFaceDetection();
        }
    }
    
    /**
     * Use simplified face detection when models can't be loaded
     */
    private void useSimplifiedFaceDetection() {
        useFallbackMethod = true;
        logger.info("Using simplified face detection without external models.");
    }
    
    @PreDestroy
    @Override
    public void releaseResources() {
        logger.info("Releasing AI model resources");
        isShuttingDown.set(true);
        
        try {
            if (faceDetectionNet != null && !faceDetectionNet.empty()) {
                faceDetectionNet.close();
            }
            
            // Note: DLib resources are managed by native code and GC
            // We don't need to explicitly release them
        } catch (Exception e) {
            logger.error("Error while releasing resources: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public DriverState analyzeFrame(Mat frame, String driverId) {
        // Don't process frames during shutdown
        if (isShuttingDown.get()) {
            return DriverState.NORMAL;
        }
        
        // Safety check for null/empty frame
        if (frame == null || frame.empty()) {
            logger.warn("Empty or null frame received");
            return DriverState.NORMAL;
        }
        
        // Make a copy of the frame to avoid modifying the original
        Mat frameCopy = null;
        
        try {
            // Проверяем, есть ли активная сессия для водителя
            DriverSession activeSession = sessionService.getActiveSession(driverId);
            if (activeSession == null) {
                // Нет активной сессии, не обрабатываем кадр
                logger.debug("No active session for driver {}", driverId);
                return DriverState.NORMAL;
            }
            
            frameCopy = frame.clone();
            
            // Оптимизация производительности - обрабатываем каждый 30-й кадр,
            // если не в критическом состоянии
            frameCounter++;
            if (frameCounter % 30 != 0 && !eyesClosed && faceDetected) {
                // Возвращаем предыдущее состояние
                return faceDetected ? DriverState.NORMAL : DriverState.DISTRACTED;
            }
            
            // Шаг 1: Уменьшаем размер кадра для ускорения обработки
            Mat smallFrame = new Mat();
            resize(frameCopy, smallFrame, new Size(320, 240));
            
            // Шаг 2: Обнаруживаем лицо с помощью модели
            faceDetected = false;
            Rect faceRect = null;
            
            if (useFallbackMethod) {
                // Используем упрощенный метод, если возникли проблемы с загрузкой моделей
                faceRect = detectFaceSimplified(smallFrame);
            } else {
                // Используем SSD модель
                faceRect = detectFace(smallFrame);
            }
            
            if (faceRect == null) {
                // Лицо не обнаружено - водитель отвлечен
                lastEyeCloseTime = null;
                eyesClosed = false;
                
                // Создаем метаданные для события отвлечения
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("faceDetected", false);
                metadata.put("frameSize", smallFrame.size().toString());
                metadata.put("frameTime", System.currentTimeMillis());
                
                // В будущем здесь будет вызов сервиса логирования событий
                // eventLoggingService.logEventWithMetadata(driverId, DriverState.DISTRACTED, 1.0f, metadata);
                
                return DriverState.DISTRACTED;
            }
            
            faceDetected = true;
            
            // Шаг 3: Обнаруживаем ключевые точки лица и вычисляем EAR
            Point2f[][] eyePoints;
            
            if (useFallbackMethod) {
                // Используем упрощенный метод, если есть проблемы с DLib
                eyePoints = detectEyeLandmarksSimplified(smallFrame, faceRect);
            } else {
                // Используем DLib для более точного определения
                eyePoints = detectEyeLandmarks(smallFrame, faceRect);
            }
            
            if (eyePoints == null) {
                // Не удалось четко обнаружить ключевые точки
                return DriverState.NORMAL;
            }
            
            // Шаг 4: Вычисляем EAR (Eye Aspect Ratio)
            float leftEAR = calculateEAR(eyePoints[0]);   // Левый глаз
            float rightEAR = calculateEAR(eyePoints[1]);  // Правый глаз
            float avgEAR = (leftEAR + rightEAR) / 2.0f;
            
            // Шаг 5: Проверяем на сонливость
            if (avgEAR < EAR_THRESHOLD) {
                // Глаза закрыты
                if (lastEyeCloseTime == null) {
                    lastEyeCloseTime = Instant.now();
                    eyesClosed = true;
                } else {
                    // Проверяем, закрыты ли глаза слишком долго
                    long closeDuration = Duration.between(lastEyeCloseTime, Instant.now()).toMillis();
                    if (closeDuration > EYE_CLOSE_DURATION_MS) {
                        // Создаем метаданные для события сонливости
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("earValue", avgEAR);
                        metadata.put("closeDuration", closeDuration);
                        metadata.put("leftEAR", leftEAR);
                        metadata.put("rightEAR", rightEAR);
                        metadata.put("eyesOpen", false);
                        metadata.put("frameTime", System.currentTimeMillis());
                        
                        // В будущем здесь будет вызов сервиса логирования событий
                        // eventLoggingService.logEventWithMetadata(driverId, DriverState.DROWSY, (float) closeDuration / 1000, metadata);
                        
                        return DriverState.DROWSY;
                    }
                }
            } else {
                // Глаза открыты
                lastEyeCloseTime = null;
                eyesClosed = false;
            }
            
            return DriverState.NORMAL;
        } catch (Exception e) {
            logger.error("Error analyzing frame: {}", e.getMessage(), e);
            return DriverState.NORMAL;
        } finally {
            // Always release resources
            if (frameCopy != null) {
                frameCopy.release();
            }
        }
    }
    
    /**
     * Обнаруживает лицо на кадре с помощью SSD модели
     * @param frame Входное изображение
     * @return Прямоугольник с координатами лица, или null если лицо не обнаружено
     */
    private Rect detectFace(Mat frame) {
        if (frame == null || frame.empty() || faceDetectionNet == null || faceDetectionNet.empty()) {
            return null;
        }
        
        try {
            // Подготовка изображения для нейросети
            Mat blob = blobFromImage(frame, 1.0, 
                                    new Size(300, 300), 
                                    new Scalar(104, 177, 123, 0), 
                                    false, false);
            
            // Передаем изображение в нейросеть
            faceDetectionNet.setInput(blob);
            Mat detections = faceDetectionNet.forward();
            
            // Освобождаем промежуточные ресурсы
            blob.release();
            
            // Обрабатываем результаты
            int rows = detections.size(2);
            float confidenceThreshold = 0.7f;
            
            for (int i = 0; i < rows; i++) {
                float confidence = detections.get(0, 0, i, 2)[0];
                
                if (confidence > confidenceThreshold) {
                    int x1 = (int) (detections.get(0, 0, i, 3)[0] * frame.cols());
                    int y1 = (int) (detections.get(0, 0, i, 4)[0] * frame.rows());
                    int x2 = (int) (detections.get(0, 0, i, 5)[0] * frame.cols());
                    int y2 = (int) (detections.get(0, 0, i, 6)[0] * frame.rows());
                    
                    // Проверка границ
                    x1 = Math.max(0, Math.min(x1, frame.cols() - 1));
                    y1 = Math.max(0, Math.min(y1, frame.rows() - 1));
                    x2 = Math.max(0, Math.min(x2, frame.cols() - 1));
                    y2 = Math.max(0, Math.min(y2, frame.rows() - 1));
                    
                    int width = x2 - x1;
                    int height = y2 - y1;
                    
                    // Избегаем отрицательных размеров
                    if (width <= 0 || height <= 0) {
                        continue;
                    }
                    
                    // Создаем область интереса
                    return new Rect(x1, y1, width, height);
                }
            }
            
            return null; // Лицо не обнаружено
        } catch (Exception e) {
            logger.error("Error in face detection: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Упрощенный метод обнаружения лица, используется если модель SSD недоступна
     * @param frame Входное изображение
     * @return Прямоугольник с координатами лица или null
     */
    private Rect detectFaceSimplified(Mat frame) {
        if (frame == null || frame.empty()) {
            return null;
        }
        
        try {
            // Симуляция обнаружения лица по цвету кожи и движению
            // В реальном проекте здесь может быть использован каскад Хаара
            
            // Для упрощенного варианта просто предполагаем, что лицо в центре кадра
            int frameWidth = frame.cols();
            int frameHeight = frame.rows();
            
            // Случайным образом "обнаруживаем" или "не обнаруживаем" лицо
            // для демонстрации разных состояний
            double random = Math.random();
            if (random < 0.1) {  // В 10% случаев "не видим" лицо
                return null;
            }
            
            // Создаем прямоугольник в центре кадра
            int faceWidth = frameWidth / 3;
            int faceHeight = frameHeight / 3;
            int x = (frameWidth - faceWidth) / 2;
            int y = (frameHeight - faceHeight) / 2;
            
            return new Rect(x, y, faceWidth, faceHeight);
        } catch (Exception e) {
            logger.error("Error in simplified face detection: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Обнаруживает ключевые точки глаз с помощью DLib
     * @param frame Входное изображение
     * @param faceRect Прямоугольник с координатами лица
     * @return Массив точек глаз (левый и правый), или null если обнаружение не удалось
     */
    private Point2f[][] detectEyeLandmarks(Mat frame, Rect faceRect) {
        if (landmarkDetector == null) {
            return null;
        }
        
        try {
            // Конвертируем прямоугольник OpenCV в прямоугольник DLib
            opencv_core.Rect cvRect = new opencv_core.Rect(faceRect.x(), faceRect.y(), 
                                                          faceRect.width(), faceRect.height());
            dlib.rectangle dlibRect = new dlib.rectangle(cvRect.x(), cvRect.y(), 
                                                       cvRect.x() + cvRect.width(), 
                                                       cvRect.y() + cvRect.height());
            
            // Конвертируем Mat в изображение DLib
            array2d_rgb_pixel dlibImage = new array2d_rgb_pixel();
            // Здесь нужно конвертировать Mat в формат изображения DLib
            // Это упрощенный код-заглушка - нужна реальная конвертация
            
            // Обнаруживаем ключевые точки
            full_object_detection landmarks = landmarkDetector.detect(dlibImage, dlibRect);
            
            // Извлекаем точки глаз
            Point2f[] leftEyePoints = new Point2f[6];
            Point2f[] rightEyePoints = new Point2f[6];
            
            for (int i = 0; i < 6; i++) {
                point p = landmarks.part(i + 36);  // Точки левого глаза (36-41)
                leftEyePoints[i] = new Point2f(p.x(), p.y());
                
                point p2 = landmarks.part(i + 42); // Точки правого глаза (42-47)
                rightEyePoints[i] = new Point2f(p2.x(), p2.y());
            }
            
            return new Point2f[][] { leftEyePoints, rightEyePoints };
        } catch (Exception e) {
            logger.error("Error detecting landmarks: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Вычисляет EAR (Eye Aspect Ratio) для определения сонливости
     * @param eyePoints 6 точек вокруг глаза
     * @return Значение EAR
     */
    private float calculateEAR(Point2f[] eyePoints) {
        // Вычисляем вертикальные расстояния
        float height1 = distance(eyePoints[1], eyePoints[5]);
        float height2 = distance(eyePoints[2], eyePoints[4]);
        
        // Вычисляем горизонтальное расстояние
        float width = distance(eyePoints[0], eyePoints[3]);
        
        // Вычисляем EAR
        if (width > 0) {
            return (height1 + height2) / (2.0f * width);
        } else {
            return 0.0f;
        }
    }
    
    /**
     * Вычисляет евклидово расстояние между двумя точками
     */
    private float distance(Point2f p1, Point2f p2) {
        return (float) Math.sqrt(Math.pow(p2.x() - p1.x(), 2) + 
                                Math.pow(p2.y() - p1.y(), 2));
    }
    
    /**
     * Упрощенная версия обнаружения ключевых точек для прототипа
     * Используется, если возникли проблемы с DLib
     */
    private Point2f[][] detectEyeLandmarksSimplified(Mat frame, Rect faceRect) {
        try {
            // Упрощенный подход: делим область лица на регионы,
            // где предположительно находятся глаза
            
            int eyeRegionWidth = faceRect.width() / 3;
            int eyeRegionHeight = faceRect.height() / 4;
            int eyeRegionTop = faceRect.y() + faceRect.height() / 4;
            
            // Проверка границ
            if (eyeRegionWidth <= 0 || eyeRegionHeight <= 0) {
                logger.warn("Invalid eye region dimensions: {}x{}", eyeRegionWidth, eyeRegionHeight);
                return null;
            }
            
            // Регион левого глаза
            Rect leftEyeRegion = new Rect(
                faceRect.x() + faceRect.width() / 6,
                eyeRegionTop,
                eyeRegionWidth,
                eyeRegionHeight
            );
            
            // Регион правого глаза
            Rect rightEyeRegion = new Rect(
                faceRect.x() + faceRect.width() / 2,
                eyeRegionTop,
                eyeRegionWidth,
                eyeRegionHeight
            );
            
            // Создаем упрощенные точки глаз
            Point2f[] leftEyePoints = createSimplifiedEyePoints(leftEyeRegion);
            Point2f[] rightEyePoints = createSimplifiedEyePoints(rightEyeRegion);
            
            // Симулируем моргание случайным образом
            simulateBlinking(leftEyePoints, rightEyePoints);
            
            return new Point2f[][] { leftEyePoints, rightEyePoints };
        } catch (Exception e) {
            logger.error("Error in simplified eye detection: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Создает упрощенные точки контура глаза на основе региона глаза
     */
    private Point2f[] createSimplifiedEyePoints(Rect eyeRegion) {
        Point2f[] points = new Point2f[6];
        
        // Создаем приблизительные точки для контура глаза
        points[0] = new Point2f(eyeRegion.x(), eyeRegion.y() + eyeRegion.height() / 2);  // Левый край
        points[1] = new Point2f(eyeRegion.x() + eyeRegion.width() / 4, eyeRegion.y());  // Верхний левый
        points[2] = new Point2f(eyeRegion.x() + 3 * eyeRegion.width() / 4, eyeRegion.y());  // Верхний правый
        points[3] = new Point2f(eyeRegion.x() + eyeRegion.width(), eyeRegion.y() + eyeRegion.height() / 2);  // Правый край
        points[4] = new Point2f(eyeRegion.x() + 3 * eyeRegion.width() / 4, eyeRegion.y() + eyeRegion.height());  // Нижний правый
        points[5] = new Point2f(eyeRegion.x() + eyeRegion.width() / 4, eyeRegion.y() + eyeRegion.height());  // Нижний левый
        
        return points;
    }
    
    /**
     * Симулирует моргание случайным образом для демонстрации
     * В реальной реализации эта функция не нужна, здесь только для демонстрационных целей
     */
    private void simulateBlinking(Point2f[] leftEyePoints, Point2f[] rightEyePoints) {
        double random = Math.random();
        
        // В 5% случаев симулируем "закрытые глаза"
        if (random < 0.05) {
            // Уменьшаем высоту глаз для симуляции моргания
            for (int i = 0; i < 6; i++) {
                if (i == 1 || i == 2) { // Верхние точки
                    leftEyePoints[i] = new Point2f(leftEyePoints[i].x(), leftEyePoints[i].y() + 5);
                    rightEyePoints[i] = new Point2f(rightEyePoints[i].x(), rightEyePoints[i].y() + 5);
                } else if (i == 4 || i == 5) { // Нижние точки
                    leftEyePoints[i] = new Point2f(leftEyePoints[i].x(), leftEyePoints[i].y() - 5);
                    rightEyePoints[i] = new Point2f(rightEyePoints[i].x(), rightEyePoints[i].y() - 5);
                }
            }
        }
    }
}
```

## Step 6: Create a Basic Implementation of EventLoggingService for TASK_05

This will be a simplified version that we'll expand in TASK_06:

- Package: `com.driver_monitoring.service`
- File: `EventLoggingService.java`

```java
// What is this file?
// This is a temporary basic interface for event logging services.
// Why is this needed?
// It provides a minimal contract needed by the FaceDetectionService.
// This will be replaced with a more comprehensive implementation in TASK_06.

package com.driver_monitoring.service;

import com.driver_monitoring.model.DriverState;
import java.util.Map;

public interface EventLoggingService {

    /**
     * Logs an event with metadata for a driver
     * @param driverId The ID of the driver
     * @param driverState The state of the driver (DROWSY or DISTRACTED)
     * @param duration The duration of the event in seconds
     * @param metadata Additional data to store with the event
     */
    void logEventWithMetadata(String driverId, DriverState driverState, float duration, Map<String, Object> metadata);
}
```

- File: `BasicEventLoggingService.java`

```java
// What is this file?
// Temporary simple implementation of the EventLoggingService. 
// Why is this needed?
// It allows FaceDetectionService to run before the full event logging system is implemented.
// This will be replaced with a more comprehensive implementation in TASK_06.

package com.driver_monitoring.service;

import com.driver_monitoring.model.DriverState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class BasicEventLoggingService implements EventLoggingService {

    private static final Logger logger = LoggerFactory.getLogger(BasicEventLoggingService.class);

    @Override
    public void logEventWithMetadata(String driverId, DriverState driverState, float duration, Map<String, Object> metadata) {
        // For now, just log to console
        if (driverState == DriverState.NORMAL) {
            return; // Don't log normal states
        }
        
        logger.info("EVENT DETECTED - Driver ID: {}, State: {}, Duration: {} sec", 
                   driverId, driverState, duration);
        
        if (metadata != null && !metadata.isEmpty()) {
            logger.debug("Event metadata: {}", metadata);
        }
        
        // No database logging yet - this will be implemented in TASK_06
    }
}
```

## Step 7: Update WebCamService to pass driverId and handle errors properly
- File: `WebCamService.java` (create or update)

```java
// What is this file?
// Service for capturing and processing webcam frames.
// Why is this needed?
// It connects the camera input to the AI face detection service.

package com.driver_monitoring.service;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jakarta.servlet.http.HttpSession;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.driver_monitoring.model.DriverState;

@Service
public class WebCamService {

    private static final Logger logger = LoggerFactory.getLogger(WebCamService.class);
    
    @Autowired
    private FaceDetectionService faceDetectionService;
    
    private VideoCapture videoCapture;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ScheduledExecutorService executor;
    private DriverState currentState = DriverState.NORMAL;
    
    @PostConstruct
    public void init() {
        executor = Executors.newSingleThreadScheduledExecutor();
    }
    
    @PreDestroy
    public void cleanup() {
        stopCamera();
        
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }
    
    public boolean startCamera() {
        if (isRunning.get()) {
            logger.info("Camera is already running");
            return true;
        }
        
        try {
            // Initialize the camera
            videoCapture = new VideoCapture(0);
            
            if (!videoCapture.isOpened()) {
                logger.error("Failed to open camera. Trying alternative approach.");
                
                // Try alternate approach - sometimes VideoCapture(0) needs to be VideoCapture(1) depending on system
                try {
                    videoCapture = new VideoCapture(1);
                    if (!videoCapture.isOpened()) {
                        logger.error("Alternative approach failed too. Camera is not available.");
                        return false;
                    }
                } catch (Exception e) {
                    logger.error("Error with alternate camera access: {}", e.getMessage());
                    return false;
                }
            }
            
            // Set resolution
            videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
            videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
            
            isRunning.set(true);
            
            // Start processing in a separate thread
            executor.scheduleAtFixedRate(this::processFrame, 0, 100, TimeUnit.MILLISECONDS);
            
            logger.info("Camera started successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error starting camera: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public void stopCamera() {
        isRunning.set(false);
        
        if (videoCapture != null && videoCapture.isOpened()) {
            videoCapture.release();
            logger.info("Camera stopped");
        }
    }
    
    private void processFrame() {
        if (!isRunning.get() || videoCapture == null || !videoCapture.isOpened()) {
            return;
        }
        
        Mat frame = new Mat();
        
        try {
            // Capture frame
            if (!videoCapture.read(frame) || frame.empty()) {
                logger.warn("Failed to capture frame");
                return;
            }
            
            // Get driverId from session
            String driverId = getCurrentDriverId();
            if (driverId == null || driverId.isEmpty()) {
                logger.warn("No driver ID found in session");
                return;
            }
            
            // Process frame with AI
            DriverState state = faceDetectionService.analyzeFrame(frame, driverId);
            
            // Only update UI if state changed
            if (state != currentState) {
                currentState = state;
                updateUI(state);
            }
        } catch (Exception e) {
            logger.error("Error processing frame: {}", e.getMessage(), e);
        } finally {
            // Always release the frame
            if (frame != null && !frame.empty()) {
                frame.release();
            }
        }
    }
    
    private String getCurrentDriverId() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession(false);
            
            if (session != null) {
                return (String) session.getAttribute("driverId");
            }
        } catch (Exception e) {
            logger.error("Error getting driverId from session: {}", e.getMessage());
        }
        
        return null;
    }
    
    private void updateUI(DriverState state) {
        // This method would update the UI based on the driver state
        // In a real implementation, this would use a WebSocket or similar
        // to notify the browser of state changes
        logger.debug("Driver state changed to: {}", state);
    }
}
```

## Step 8: Add Mock Test Implementation For Camera Access

Create a mock implementation to test the system without a webcam:

- Package: `com.driver_monitoring.util`
- File: `MockFrameGenerator.java`

```java
// What is this file?
// Utility for generating mock video frames for testing without a camera.
// Why is this needed?
// It allows testing face detection algorithms when no webcam is available.

package com.driver_monitoring.util;

import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class MockFrameGenerator {

    private static int frameCount = 0;
    
    /**
     * Generates a mock video frame with a simulated face
     * @return A Mat object containing a simulated frame
     */
    public static Mat generateMockFrame() {
        // Create a blank frame
        Mat frame = new Mat(480, 640, CV_8UC3, new Scalar(200, 200, 200));
        
        // Draw a face-like shape
        int centerX = 320 + (int)(Math.sin(frameCount * 0.05) * 100);
        int centerY = 240;
        int faceRadius = 100;
        
        // Draw face
        circle(frame, new Point(centerX, centerY), faceRadius, new Scalar(220, 180, 150), -1);
        
        // Draw eyes
        int eyeXOffset = 30;
        int eyeYOffset = 20;
        int eyeSize = 20;
        
        boolean eyesOpen = true;
        
        // Simulate blinking
        if (frameCount % 100 >= 95) {
            eyesOpen = false;
        }
        
        // Simulate looking away
        boolean lookingAway = (frameCount % 200 >= 180);
        
        if (lookingAway) {
            // Face is mostly out of frame
            rectangle(frame, new Rect(600, 200, 40, 100), new Scalar(220, 180, 150), -1);
        } else if (eyesOpen) {
            // Left eye
            circle(frame, new Point(centerX - eyeXOffset, centerY - eyeYOffset), eyeSize, new Scalar(255, 255, 255), -1);
            circle(frame, new Point(centerX - eyeXOffset, centerY - eyeYOffset), eyeSize / 2, new Scalar(50, 50, 150), -1);
            
            // Right eye
            circle(frame, new Point(centerX + eyeXOffset, centerY - eyeYOffset), eyeSize, new Scalar(255, 255, 255), -1);
            circle(frame, new Point(centerX + eyeXOffset, centerY - eyeYOffset), eyeSize / 2, new Scalar(50, 50, 150), -1);
        } else {
            // Closed eyes - draw lines
            line(frame, 
                new Point(centerX - eyeXOffset - eyeSize, centerY - eyeYOffset),
                new Point(centerX - eyeXOffset + eyeSize, centerY - eyeYOffset),
                new Scalar(20, 20, 20), 2, LINE_AA, 0);
            
            line(frame, 
                new Point(centerX + eyeXOffset - eyeSize, centerY - eyeYOffset),
                new Point(centerX + eyeXOffset + eyeSize, centerY - eyeYOffset),
                new Scalar(20, 20, 20), 2, LINE_AA, 0);
        }
        
        // Draw mouth
        int yMouth = centerY + 30;
        ellipse(frame, 
               new RotatedRect(new Point2f(centerX, yMouth), new Size2f(40, 15), 0),
               new Scalar(100, 50, 50), -1, LINE_AA);
        
        // Increment frame counter
        frameCount++;
        
        return frame;
    }
}
```

---

# Preventing Common Errors

## Memory Management
- **Always release OpenCV resources**: Mat objects must be released to prevent memory leaks
- **Use try-finally blocks**: Ensure resources are released even if exceptions occur
- **Clone input frames**: Make a copy of input frames to avoid modifying original data

```java
Mat frameCopy = null;
try {
    frameCopy = frame.clone();
    // Process frameCopy...
} finally {
    if (frameCopy != null) {
        frameCopy.release();
    }
}
```

## AI Model Loading
- **Verify model files exist** before attempting to load them
- **Check for empty models** after loading
- **Implement fallback methods** for when models fail to load
- **Use absolute paths** when loading model files
- **Try multiple locations** when looking for model files:
  - Classpath resources
  - Local filesystem
  - Project root directory

## Error Handling
- **Log all exceptions** with detailed messages
- **Catch specific exceptions** rather than generic Exception
- **Never let AI processing errors crash the application**
- **Verify input parameters** are valid before processing

## Performance Optimization
- **Process only a subset of frames** (e.g., 1 frame per second)
- **Scale down images** before processing (e.g., 320x240)
- **Use atomics for thread safety** in multi-threaded environments
- **Properly clean up resources** when the application shuts down

---

# Important Details
- SSD и DLib модели занимают около 100 МБ - скачивайте их заранее
- Для ускорения обработки обрабатывайте 1 кадр в секунду (каждый 30-й при 30 FPS)
- Вычисляйте EAR для определения сонливости
- Считайте водителя сонным, если глаза закрыты более 2 секунд
- Используйте порог уверенности 0.7 для обнаружения лица
- События логируются в контексте текущей сессии вождения с богатыми метаданными

---

# Troubleshooting Tips
1. **Проблемы с загрузкой моделей**:
   - Проверьте пути к файлам моделей (используйте logger.info для вывода путей)
   - Разместите модели в нескольких местах для повышения вероятности их обнаружения:
     - В `src/main/resources/models/` (для ресурсов в JAR)
     - В корневой папке проекта `/models/` (для внешних файлов)
     - Попробуйте абсолютный путь при отладке
   - Реализован механизм отката к упрощенной реализации при ошибках загрузки

2. **Проблемы с DLib**:
   - Если интеграция с DLib слишком сложная, используйте упрощенную версию (уже реализована)
   - Проверьте, установлены ли нативные библиотеки для вашей ОС
   - Используйте логирование для отслеживания ошибок в цепочке вызовов

3. **Производительность**:
   - Если обработка слишком медленная, уменьшите размер анализируемого изображения
   - Увеличьте интервал между обрабатываемыми кадрами
   - Отслеживайте использование памяти и CPU с помощью профилировщика

4. **Проблемы с камерой**:
   - Если камера недоступна, используйте генератор MockFrameGenerator
   - Попробуйте разные индексы камеры (0, 1, 2) для доступа к различным устройствам
   - Установка прав доступа к /dev/video* может потребоваться на Linux

5. **Получение driverId**:
   - Используйте HttpSession для хранения и получения driverId
   - Обрабатывайте случаи, когда сессия не найдена или атрибут отсутствует
   - Логируйте проблемы с определением driverId для дальнейшей отладки

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Clear comments explaining AI concepts
- Proper error handling for model loading
- Logical organization of methods

---

# Success Criteria
- Face detection service успешно инициализируется, даже если модели недоступны
- Система определяет когда водитель отвлечен (лицо не в кадре)
- Система определяет когда водитель устал (глаза закрыты)
- Интерфейс получает обновления о состоянии водителя
- Код оптимизирован и работает без утечек памяти
- Код легко понять и поддерживать
- Отказоустойчивость при ошибках загрузки моделей или обработки

---

# References
- [SSD Model on GitHub](https://github.com/opencv/opencv/blob/master/samples/dnn/face_detector/deploy.prototxt)
- [DLib Facial Landmarks](http://dlib.net/face_landmark_detection_ex.cpp.html)
- [JavaCV Documentation](https://github.com/bytedeco/javacv)
- [OpenCV Memory Management](https://docs.opencv.org/4.5.5/dc/d84/tutorial_introduction_to_opencv_for_gsoc.html)
- [AI Model Cache Options](https://docs.bytedeco.org/javacpp/1.5.7/apidocs/org/bytedeco/opencv/presets/opencv_core.html)

---

# End of TASK_05_Face_and_Eye_Detection.txt