# Knowledge: ai_models_knowledge.txt

# Purpose
This knowledge file explains AI models for driver fatigue and distraction detection, providing information about ready-to-use pre-trained models, their sources, and basic implementation details for use with the session-based logging architecture.

---

# AI Models Overview
The project uses three types of AI models in a pipeline:

1. **Face Detection Model**: Locates and extracts the driver's face in video frames
2. **Facial Landmarks Model**: Identifies 68 key points on the face
3. **Behavior Analysis**: Uses the landmarks to calculate fatigue metrics and logs events within driver sessions

---

# 1. Face Detection Model (SSD)

## Model Details
- **Name**: Single Shot MultiBox Detector (SSD)
- **File**: `res10_300x300_ssd_iter_140000.caffemodel`
- **Configuration**: `deploy.prototxt`
- **Source**: Included in OpenCV (or download from OpenCV GitHub repository)

## How to Download
```bash
# Download links if not available in OpenCV
wget -O src/main/resources/models/face_detection/res10_300x300_ssd_iter_140000.caffemodel https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000.caffemodel
wget -O src/main/resources/models/face_detection/deploy.prototxt https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/deploy.prototxt
```

## Implementation
```java
// Load the model
Net faceNet = readNetFromCaffe("models/deploy.prototxt", "models/res10_300x300_ssd_iter_140000.caffemodel");

// Prepare image for network (preprocessing)
Mat blob = blobFromImage(frame, 1.0, new Size(300, 300), new Scalar(104, 177, 123));

// Pass through the network
faceNet.setInput(blob);
Mat detections = faceNet.forward();

// Process results (confidence threshold 0.7)
float confidence = detections.get(0, 0, i, 2)[0];
if (confidence > 0.7f) {
    // Face detected - extract coordinates
    int x1 = (int) (detections.get(0, 0, i, 3)[0] * frameWidth);
    int y1 = (int) (detections.get(0, 0, i, 4)[0] * frameHeight);
    int x2 = (int) (detections.get(0, 0, i, 5)[0] * frameWidth);
    int y2 = (int) (detections.get(0, 0, i, 6)[0] * frameHeight);
    
    // Create region of interest
    Rect faceRect = new Rect(x1, y1, (x2 - x1), (y2 - y1));
}
```

---

# 2. Facial Landmarks Model (DLib)

## Model Details
- **Name**: DLib's 68-point Face Landmark Detector
- **File**: `shape_predictor_68_face_landmarks.dat`
- **Source**: http://dlib.net/files/shape_predictor_68_face_landmarks.dat.bz2

## How to Download
```bash
# Download and decompress the file
wget -O src/main/resources/models/landmarks/shape_predictor_68_face_landmarks.dat.bz2 http://dlib.net/files/shape_predictor_68_face_landmarks.dat.bz2
bzip2 -d src/main/resources/models/landmarks/shape_predictor_68_face_landmarks.dat.bz2
```

## Implementation with JavaCV
```java
// Load the landmark detector (use JavaCV's wrapper for DLib)
ShapePredictor landmarkDetector = new ShapePredictor();
landmarkDetector.load("models/shape_predictor_68_face_landmarks.dat");

// Detect landmarks on the face region
FullObjectDetection landmarks = landmarkDetector.detect(dlibImage, dlibRectangle);

// Get specific landmarks (eye points)
Point2f leftEyePoints[] = new Point2f[6];
for (int i = 0; i < 6; i++) {
    Point p = landmarks.getPart(i + 36); // Left eye points are 36-41
    leftEyePoints[i] = new Point2f(p.x(), p.y());
}

Point2f rightEyePoints[] = new Point2f[6]; 
for (int i = 0; i < 6; i++) {
    Point p = landmarks.getPart(i + 42); // Right eye points are 42-47
    rightEyePoints[i] = new Point2f(p.x(), p.y());
}
```

## Landmark Points Key
The 68 landmarks include:
- Points 0-16: Jawline
- Points 17-26: Eyebrows
- Points 27-35: Nose
- Points 36-41: Left eye
- Points 42-47: Right eye
- Points 48-67: Mouth

---

# 3. Integration with Session-Based Logging

## Session Context for AI Analysis
When working with AI models in the context of driving sessions:

```java
@Autowired
private SessionService sessionService;

@Autowired
private EventLoggingService eventLoggingService;

// In FaceDetectionService
public DriverState analyzeFrame(Mat frame, String driverId) {
    // Get active session for the driver
    DriverSession activeSession = sessionService.getActiveSession(driverId);
    if (activeSession == null) {
        // No active session, cannot analyze
        return DriverState.NORMAL;
    }
    
    // Proceed with AI analysis...
    boolean faceDetected = detectFace(frame) != null;
    
    if (!faceDetected) {
        // Driver is distracted - log the event with metadata in the current session
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("faceDetected", false);
        metadata.put("frameTime", System.currentTimeMillis());
        
        eventLoggingService.logEventWithMetadata(
            driverId,
            DriverState.DISTRACTED,
            1.0f, // Duration in seconds
            metadata
        );
        
        return DriverState.DISTRACTED;
    }
    
    // Continue with eye analysis...
}
```

## Creating Rich Event Metadata
Enhance events with AI-derived metrics for better analysis:

```java
// For drowsy events, include EAR values
Map<String, Object> metadata = new HashMap<>();
metadata.put("earValue", avgEAR);
metadata.put("leftEAR", leftEAR);
metadata.put("rightEAR", rightEAR);
metadata.put("closeDuration", closeDurationMs);
metadata.put("frameTime", System.currentTimeMillis());

// For distracted events, include head position data if available
Map<String, Object> metadata = new HashMap<>();
metadata.put("faceDetected", false);
metadata.put("headPosition", headPosition); // e.g., "left", "right", "down"
metadata.put("frameTime", System.currentTimeMillis());
```

---

# 4. Eye Aspect Ratio (EAR) Calculation

## Formula
Eye Aspect Ratio (EAR) is calculated from the landmarks:
```
EAR = (||p2-p6|| + ||p3-p5||) / (2 * ||p1-p4||)
```
where p1, p2, p3, p4, p5, and p6 are the eye landmark points.

## Implementation
```java
// Calculate EAR for an eye
public float calculateEAR(Point2f[] eyePoints) {
    // Calculate vertical distances
    float height1 = distance(eyePoints[1], eyePoints[5]);
    float height2 = distance(eyePoints[2], eyePoints[4]);
    
    // Calculate horizontal distance
    float width = distance(eyePoints[0], eyePoints[3]);
    
    // Calculate EAR
    return (height1 + height2) / (2.0f * width);
}

// Helper distance function
private float distance(Point2f p1, Point2f p2) {
    return (float) Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
}
```

## Drowsiness Detection
- Normal EAR: 0.25 - 0.3 (varies by person)
- Closed eyes: EAR < 0.2
- Drowsy: EAR < 0.25 for more than 2 seconds

---

# 5. Performance Optimization

## Frame Rate Reduction
```java
// Process only 1 frame per second to reduce CPU load
if (frameCounter % 30 != 0 && !isInAlertState) {
    return previousState; // Skip processing, return previous state
}
```

## Image Scaling
```java
// Scale down image to speed up processing
Mat smallFrame = new Mat();
double scaleFactor = 0.5; // Process at half size
Size targetSize = new Size(frame.cols() * scaleFactor, frame.rows() * scaleFactor);
resize(frame, smallFrame, targetSize);

// Process the small frame
DriverState state = analyzeSmallFrame(smallFrame);
```

## Processing Pipeline Optimization
```java
// Quick check first - is there a face?
boolean faceDetected = quickFaceCheck(smallFrame);
if (!faceDetected) {
    return DriverState.DISTRACTED;
}

// Only if face is detected, do detailed analysis
DriverState state = analyzeFacialFeatures(smallFrame, faceRect);
```

---

# 6. Alternative Approach: Simplified Landmarks Detection

If you encounter issues with DLib integration, you can use this simplified approach for prototype purposes:

```java
private Point2f[][] detectEyeLandmarksSimplified(Mat frame, Rect faceRect) {
    // Divide face region into eye regions using proportions
    int eyeRegionWidth = faceRect.width() / 3;
    int eyeRegionHeight = faceRect.height() / 4;
    int eyeRegionTop = faceRect.y() + faceRect.height() / 4;
    
    // Left eye region
    Rect leftEyeRegion = new Rect(
        faceRect.x() + faceRect.width() / 6,
        eyeRegionTop,
        eyeRegionWidth,
        eyeRegionHeight
    );
    
    // Right eye region
    Rect rightEyeRegion = new Rect(
        faceRect.x() + faceRect.width() / 2,
        eyeRegionTop,
        eyeRegionWidth,
        eyeRegionHeight
    );
    
    // Create simplified points
    Point2f[] leftEyePoints = createEyeOutlinePoints(leftEyeRegion);
    Point2f[] rightEyePoints = createEyeOutlinePoints(rightEyeRegion);
    
    return new Point2f[][] { leftEyePoints, rightEyePoints };
}

private Point2f[] createEyeOutlinePoints(Rect eyeRegion) {
    Point2f[] points = new Point2f[6];
    
    // Create approximate outline points
    points[0] = new Point2f(eyeRegion.x(), eyeRegion.y() + eyeRegion.height() / 2); // Left
    points[1] = new Point2f(eyeRegion.x() + eyeRegion.width() / 4, eyeRegion.y()); // Top-left
    points[2] = new Point2f(eyeRegion.x() + 3 * eyeRegion.width() / 4, eyeRegion.y()); // Top-right
    points[3] = new Point2f(eyeRegion.x() + eyeRegion.width(), eyeRegion.y() + eyeRegion.height() / 2); // Right
    points[4] = new Point2f(eyeRegion.x() + 3 * eyeRegion.width() / 4, eyeRegion.y() + eyeRegion.height()); // Bottom-right
    points[5] = new Point2f(eyeRegion.x() + eyeRegion.width() / 4, eyeRegion.y() + eyeRegion.height()); // Bottom-left
    
    return points;
}
```

---

# 7. Model Files Structure

Place all model files in the `src/main/resources/models/` directory:
```
src/main/resources/models/
|-- face_detection/
|   |-- deploy.prototxt
|   |-- res10_300x300_ssd_iter_140000.caffemodel
|-- landmarks/
|   |-- shape_predictor_68_face_landmarks.dat
```

---

# Summary
- Use SSD model for face detection (much more accurate than Haar Cascades)
- Use DLib's 68-point landmark model for precise facial feature detection
- Calculate EAR to determine eye openness
- Process 1 frame per second unless in alert state
- Scale images down for faster processing
- Use multi-stage approach: quick check first, detailed analysis only if needed
- Always check for active session before logging events
- Include rich metadata with each event for better analysis

These AI models require no training and are ready to use out of the box, making them perfect for a hackathon project.

---

# End of ai_models_knowledge.txt
