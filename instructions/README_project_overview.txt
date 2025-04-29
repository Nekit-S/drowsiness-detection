# README_project_overview.txt

# Project Title: Driver Monitoring System (Prototype)

---

# Purpose
The goal of this project is to build a simple, working prototype of a Driver Monitoring System to detect driver fatigue and distraction, optimized for low-resource hardware like Raspberry Pi.

Initially, the system will be developed and tested on a standard PC and must be designed to consume minimal CPU/GPU resources.

---

# Target Users
- Transportation companies in Kazakhstan
- Dispatchers monitoring multiple drivers
- Drivers themselves (simple interface to start monitoring)

---

# Main Features

## Driver Screen
- Input Driver Name and 6-digit Driver ID
- Start video-based face and eye detection after input
- Show driver's real-time video feed
- Detect and classify states:
  - **Normal**
  - **Distracted** (face turned away)
  - **Drowsy** (long eye closure or frequent blinking)
- Simple alert notifications on screen
- Button to exit session
- Save driver event data (only distracted or drowsy events)

## Dispatcher Screen
- List of all available drivers (Name + ID)
- Click on a driver to view:
  - Graph showing events over time
  - Table of logged distraction and drowsiness events

---

# System Architecture

| Component           | Technology     |
|---------------------|-----------------|
| Backend             | Java 17 + Spring Boot |
| Frontend            | Thymeleaf + Bootstrap 5 + jQuery |
| Computer Vision     | OpenCV via JavaCV |
| Database            | H2 in-memory (for prototype) |
| Charting Library    | Chart.js |
| Containerization    | Docker + docker-compose |

---

# Database Structure

## Drivers Table
| Field       | Type     |
|-------------|----------|
| Driver_ID   | String (6 digits, PK) |
| Driver_Name | String |

## Events Table
| Field         | Type     |
|---------------|----------|
| Event_ID      | Long (PK, auto-generated) |
| Driver_ID     | String (FK -> Drivers.Driver_ID) |
| Timestamp     | DateTime |
| Event_Type    | Enum ("Distracted", "Drowsy") |
| Event_Duration| Float (seconds) |

---

# Performance Requirements
- Limit camera stream to 640x480 resolution.
- Target ~20 FPS for video analysis.
- Analyze one frame every 1 second for state detection.
- Only distracted or drowsy events are logged (no normal events).

---

# Project Standards

- All code must follow the rules in `CODING_STANDARDS.txt`.
- Every file/class must start with a comment:
  - What is this?
  - Why does it exist?
- Code must be simple, explicit, and understandable by junior developers.
- Where applicable, reference Knowledge files for specific implementation details:
  - Face and Eye Detection ➔ `Knowledge/face_detection_knowledge.txt`
  - Event Logging ➔ `Knowledge/event_logging_knowledge.txt`

---

# Task Management

The work is broken into independent tasks ("mini-projects") for easy assignment and tracking.
Each task document (TASK_XX) contains:
- Goal of the task
- Step-by-step actions
- Success criteria
- References to Knowledge and Standards

---

# Success Criteria for the Project
- Application builds and runs inside Docker without errors.
- Driver login, face tracking, and state detection work.
- Dispatcher panel shows correct event data.
- Code follows all formatting, commenting, and simplicity requirements.
- All major functionality covered by basic unit tests.

---

# End of README_project_overview.txt

