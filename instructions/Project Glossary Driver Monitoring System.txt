# Project Glossary: Driver Monitoring System

This glossary provides a quick navigation guide to all project files, with a brief description of each file's purpose.
Use it to easily find and understand where everything is located.

---

# 1. Main Documents

## CODING_STANDARDS.txt
Defines strict rules for writing code:
- Simple, clear code
- Required file/class/method comments
- Consistent formatting and structure

## README_project_overview.txt
Overview of the whole project:
- Purpose, technologies, and architecture
- Main features
- Development phases
- Reference to standards and knowledge files

---

# 2. Knowledge Files

## Knowledge/face_detection_knowledge.txt
Describes:
- How to use JavaCV + OpenCV for face and eye detection
- Which models to use
- How to detect Normal, Distracted, and Drowsy states
- Example JavaCV code snippets

## Knowledge/event_logging_knowledge.txt
Describes:
- Rules for logging driver events (only critical events)
- Data model for Events
- How to properly save logs into the database
- Best practices and examples

---

# 3. Task Documents
(Each one corresponds to a logical development phase)

## TASK_01_Setup_Backend.txt
- How to create a basic Spring Boot project
- How to containerize it with Docker
- "Hello World" validation

## TASK_02_Create_Entities_and_Repositories.txt
- Create Driver and Event entities
- Set up Spring Data JPA repositories
- Database schema definition

## TASK_03_Create_Driver_Screen.txt
- Build the driver login page and monitoring screen
- Start webcam tracking after login
- Display live video feed
- Set up Exit button

## TASK_04_Create_Dispatcher_Screen.txt
- Build dispatcher panel
- List drivers and select a driver
- Show graphs and event logs for selected driver

## TASK_05_Face_and_Eye_Detection.txt
- Implement face and eye detection logic
- Determine driver states
- Process video frames every second

## TASK_06_Event_Logging_Service.txt
- Implement service to log distraction and drowsiness events into the database
- Only critical states are logged

## TASK_07_Notification_System.txt
- Implement visual Bootstrap alerts based on driver state
- Show warning messages for distraction or drowsiness

## TASK_08_Unit_Tests.txt
- Create basic unit tests for detection and logging services
- Validate critical paths automatically

## TASK_09_Containerization.txt
- Finalize Docker and docker-compose setup
- Enable full project launch with one command

---

# 4. Important Navigation Notes

- Each task depends on completing previous tasks.
- Knowledge files must be read carefully before working on detection or logging.
- Coding Standards must be followed for every file and commit.
- After all tasks are completed, the entire system can be tested via Docker.

---

# End of Project Glossary

