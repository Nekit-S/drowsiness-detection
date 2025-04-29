# CODING_STANDARDS.txt

## Purpose
These standards are mandatory for every task and file created in this project.
The goal is to ensure that all code is simple, consistent, easy to read, and understandable by any junior developer.

---

# 1. General Philosophy
- **Always write the simplest working code**.
- **Avoid "smart" solutions**. Prefer straightforward, explicit implementations.
- **Each class or file must do only one thing** (Single Responsibility Principle).
- **Prefer clarity over conciseness**.

**Reminder:** If something can be done in a simpler way, choose the simpler way.

---

# 2. Code Formatting Rules

## File and Class Headers
At the top of **every Java file**, you must include the following comment:

```java
// What is this file?
// Briefly describe what this class is for (2-3 sentences).
// Why is this class needed?
// Explain the role this class plays in the overall project.
```

## Indentation and Braces
- Use **4 spaces** for indentation.
- Always use **curly braces `{}`** even for one-line if/else blocks.

```java
if (isDrowsy) {
    triggerAlert();
}
```

## Naming Conventions
- Class names: **PascalCase** (e.g., `DriverService`, `EventLogger`)
- Variable and method names: **camelCase** (e.g., `driverName`, `calculateEar`)
- Constants: **UPPER_SNAKE_CASE** (e.g., `MAX_ALLOWED_FPS`)

## Commenting Inside Methods
- Add a short comment for every logical block inside a method.

```java
// Check if eyes are closed for more than 2 seconds
if (eyeClosureDuration > 2.0) {
    eventLogger.logDrowsyEvent(driverId);
}
```

---

# 3. Class and File Structure

Each Java file should be structured in the following order:
1. **Imports**
2. **Class header comment** (see above)
3. **Class declaration**
4. **Fields/Attributes**
5. **Constructor(s)**
6. **Public methods**
7. **Private helper methods**

---

# 4. Error Handling
- Always check for **null** values where appropriate.
- Handle exceptions gracefully: log errors or show a meaningful message.

```java
try {
    detectionService.startCamera();
} catch (IOException e) {
    // Log error with meaningful description
    logger.error("Failed to start camera: " + e.getMessage());
}
```

---

# 5. Knowledge References
When encountering:
- Face or eye detection logic ➔ **See Knowledge/face_detection_knowledge.txt**
- Event logging logic ➔ **See Knowledge/event_logging_knowledge.txt**

You **must** read the relevant Knowledge file before implementing!

---

# 6. Task Completion Check
Each task is considered complete when:
- Code compiles without errors.
- Functionality works as described.
- Code follows these Coding Standards.
- Meaningful comments are included in every file and important method.

If these conditions are not met, the task must be revised before moving forward.

---

# 7. Final Note
If you are ever in doubt:
- **Choose the simpler solution**.
- **Write a small comment explaining your choice**.

The goal is: any junior developer should be able to pick up any part of the codebase, read it, and immediately understand what it does and why it exists.

---

# END OF CODING_STANDARDS.txt

