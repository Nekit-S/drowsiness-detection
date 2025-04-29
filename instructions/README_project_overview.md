# README_project_overview.txt

# Project Title: Driver Monitoring System (Prototype)

---

# Purpose
The goal of this project is to build a simple, working prototype of a Driver Monitoring System to detect driver fatigue and distraction using AI technologies, optimized for low-resource hardware like Raspberry Pi.

Initially, the system will be developed and tested on a standard PC and must be designed to consume minimal CPU/GPU resources.

---

# Target Users
- Transportation companies in Kazakhstan
- Dispatchers monitoring multiple drivers
- Drivers themselves (simple interface to start monitoring)

---

# Main Features (Core MVP)

## Driver Screen
- Input Driver Name and 6-digit Driver ID
- Start a new driving session after login
- Start video-based face and eye detection
- Show driver's real-time video feed
- Detect and classify states:
  - **Normal**
  - **Distracted** (face turned away)
  - **Drowsy** (long eye closure or frequent blinking)
- Simple alert notifications on screen
- Button to exit session (ends driving session)
- Save driver event data with rich metadata

## Dispatcher Screen
- List of all available drivers (Name + ID)
- View active and past driving sessions
- For each session:
  - Graph showing events over time
  - Table of detailed event logs with metadata
  - Analytics by event type

## AI-Based Detection
- Use SSD (Single Shot MultiBox Detector) to accurately detect face
- Apply DLib facial landmarks to identify eye positions
- Calculate EAR (Eye Aspect Ratio) to determine drowsiness
- Process frames efficiently (1 frame per second)

---

# System Architecture

| Component           | Technology     |
|---------------------|-----------------|
| Backend             | Java 17 + Spring Boot |
| Frontend            | Thymeleaf + Bootstrap 5 + jQuery |
| Computer Vision     | OpenCV via JavaCV + DLib |
| Database            | H2 file-based (for persistence) |
| Charting Library    | Chart.js |
| Containerization    | Docker + docker-compose |

---

# Database Structure

## Drivers Table
| Field       | Type     |
|-------------|----------|
| Driver_ID   | String (6 digits, PK) |
| Driver_Name | String |

## Driver Sessions Table
| Field                  | Type     |
|------------------------|----------|
| Session_ID             | Long (PK, auto-generated) |
| Driver_ID              | String (FK -> Drivers.Driver_ID) |
| Start_Time             | DateTime |
| End_Time               | DateTime (null if active) |
| Total_Driving_Time_Seconds | Long |
| Active                 | Boolean |

## Events Table
| Field         | Type     |
|---------------|----------|
| Event_ID      | Long (PK, auto-generated) |
| Session_ID    | Long (FK -> Driver_Sessions.Session_ID) |
| Driver_ID     | String (FK -> Drivers.Driver_ID) |
| Start_Time    | DateTime |
| End_Time      | DateTime |
| Duration      | Float (seconds) |
| Event_Type    | String ("DROWSY" or "DISTRACTED") |
| Metadata      | CLOB (JSON with additional metrics) |

---

# Two-Level Logging Architecture

The system uses a hierarchical approach to tracking driver behavior:

1. **Session Level**:
   - Created when driver logs in
   - Tracks total driving time
   - Contains all events for that driving period
   - Ended when driver logs out or session times out

2. **Event Level**:
   - Individual incidents within a session
   - Rich JSON metadata for detailed analysis
   - Includes contextual information (EAR values, head position, etc.)
   - Linked to both driver and session for flexible querying

This approach enables:
- Temporal analysis within driving periods
- Pattern identification across multiple sessions
- Rich contextual data without schema changes
- Efficient storage and querying

---

# Performance Requirements
- Limit camera stream to 640x480 resolution.
- Process approximately 1 frame per second for state detection.
- Only distracted or drowsy events are logged (no normal events).
- System should run on standard hardware without specialized GPUs.
- File-based H2 database for persistence between restarts.

---

# Project Structure and Roadmap

## Core Tasks (Priority Implementation)
1. **TASK_01**: Setup Backend and Containerize Project ✅
2. **TASK_02**: Create Entities and Repositories (with two-level structure) ✅
3. **TASK_03**: Create Driver Screen with Session Management ✅
4. **TASK_04**: Create Dispatcher Screen with Session Analysis ✅
5. **TASK_05**: Implement AI-Based Face and Eye Detection ⭐
6. **TASK_06**: Create Event Logging Service with Metadata ✅
7. **TASK_07**: Implement Notification System ✅
8. **TASK_09**: Finalize Containerization ✅

## Extension Tasks (After Core MVP)
1. **TASK_08**: Create Unit Tests
2. **TASK_10**: Implement Predictive Analytics
3. Additional features (localization, enhanced UI, etc.)

⭐ = Critical for AI scoring criterion

---

# Project Standards

- All code must follow the rules in `CODING_STANDARDS.txt`.
- Every file/class must start with a comment:
  - What is this?
  - Why does it exist?
- Code must be simple, explicit, and understandable by junior developers.
- Where applicable, reference Knowledge files for specific implementation details:
  - AI Models ➔ `Knowledge/ai_models_knowledge.txt`
  - Event Logging ➔ `Knowledge/event_logging_knowledge.txt`

---

# Success Criteria for the Project
- Application builds and runs inside Docker without errors.
- AI models correctly detect face, eyes, and driver state.
- Driver login creates a session and events are properly logged within it.
- Driver logout correctly ends the session.
- Dispatcher panel shows comprehensive session and event data.
- Code follows all formatting, commenting, and simplicity requirements.

---

# Key Components for Hackathon Evaluation

1. **Working Implementation** (Criterion 1)
   - Stable application with clean UI
   - Error-free demo
   - Full user journey demonstration
   - Session-based event tracking

2. **AI Usage** (Criterion 2 - Critical)
   - SSD model for face detection
   - DLib for facial landmarks
   - EAR calculation algorithm
   - Rich metadata collection in events
   - Optimized processing pipeline

3. **Solution Design** (Criterion 3)
   - Two-level logging architecture
   - Well-documented knowledge base
   - Evidence-based problem description
   - Unique approach to monitoring

4. **Project Potential** (Criterion 4)
   - SaaS business model
   - Clear target audience
   - Scalable technical design
   - Analytics capabilities

5. **Project Presentation** (Criterion 5)
   - Compelling demonstration of complete monitoring workflow
   - Clear explanation of AI components
   - Visualization of collected data and analytics
   - Professional slides and delivery

---

# End of README_project_overview.txt
