# TASK_07_Notification_System.txt

# Task Title
Implement Driver Notification System: Show Alerts Based on Driver State

---

# Goal
Develop a frontend notification system that:
- Shows a visual alert when the driver becomes distracted or drowsy
- Changes the visual appearance based on the current state
- Helps the driver immediately realize their condition
- Logs notification events with timestamps for later analysis

---

# Why This Task Is Important
- Immediate feedback can prevent accidents
- Simple alerts reinforce the importance of maintaining attention
- Critical part of human-centered design
- Notification history can be analyzed in session context

---

# Prerequisites
Before starting this task:
- Complete `TASK_06_Event_Logging_Service.txt`.
- Complete `TASK_03_Create_Driver_Screen.txt` with session support.
- Review `CODING_STANDARDS.txt`.
- Understand basic Bootstrap alerts.

---

# Detailed Instructions

## Step 1: Update Driver Monitoring Page HTML
- File: `src/main/resources/templates/driver_monitoring.html`

Add or update:
- A placeholder `<div>` element with an ID `notificationArea` where alerts will be shown
- Add session status indicator
- Example:

```html
<div id="notificationArea" style="position: absolute; top: 10px; left: 10px; z-index: 1000; width: 80%;"></div>

<div class="card mt-4">
    <div class="card-header">
        <h5>Session Information</h5>
    </div>
    <div class="card-body">
        <p><strong>Session ID:</strong> <span th:text="${sessionId}"></span></p>
        <p><strong>Started:</strong> <span th:text="${sessionStart}"></span></p>
        <p><strong>Duration:</strong> <span id="sessionDuration">00:00:00</span></p>
        <p><strong>Alert Count:</strong> <span id="alertCount">0</span></p>
    </div>
</div>
```

## Step 2: Create Notification Functions (JS)
In the same page (`driver_monitoring.html`), inside a `<script>` tag:

Add JavaScript functions to display alerts and log them:

```javascript
// State tracking variables
let lastState = 'NORMAL';
let alertCount = 0;
let alertHistory = [];

function showNormalNotification() {
    if (lastState !== 'NORMAL') {
        document.getElementById('notificationArea').innerHTML = `
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                <strong>Status:</strong> Normal
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>`;
        
        document.getElementById('statusIndicator').className = 'status-indicator bg-success text-white';
        document.getElementById('statusIndicator').textContent = 'Status: Normal';
        
        lastState = 'NORMAL';
    }
}

function showDistractedNotification() {
    if (lastState !== 'DISTRACTED') {
        document.getElementById('notificationArea').innerHTML = `
            <div class="alert alert-warning alert-dismissible fade show" role="alert">
                <strong>Warning:</strong> Eyes on the road! You appear distracted.
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>`;
        
        document.getElementById('statusIndicator').className = 'status-indicator bg-warning text-dark';
        document.getElementById('statusIndicator').textContent = 'Status: Distracted';
        
        // Play alert sound
        playAlertSound('distracted');
        
        // Log the alert for history
        logAlert('DISTRACTED');
        
        lastState = 'DISTRACTED';
    }
}

function showDrowsyNotification() {
    if (lastState !== 'DROWSY') {
        document.getElementById('notificationArea').innerHTML = `
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <strong>Warning:</strong> You appear drowsy! Consider taking a break.
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>`;
        
        document.getElementById('statusIndicator').className = 'status-indicator bg-danger text-white';
        document.getElementById('statusIndicator').textContent = 'Status: Drowsy';
        
        // Play alert sound
        playAlertSound('drowsy');
        
        // Log the alert for history
        logAlert('DROWSY');
        
        lastState = 'DROWSY';
    }
}

function logAlert(alertType) {
    // Increment alert counter
    alertCount++;
    document.getElementById('alertCount').textContent = alertCount;
    
    // Save to history
    alertHistory.push({
        type: alertType,
        timestamp: new Date().toISOString()
    });
    
    // Optional: If you want to send this to server
    // sendAlertToServer(alertType);
}

function playAlertSound(type) {
    // Different sounds for different alert types
    const sound = type === 'drowsy' ? 'drowsy_alert.mp3' : 'distracted_alert.mp3';
    
    // Create audio element
    const audio = new Audio('/sounds/' + sound);
    audio.volume = 0.7;
    audio.play().catch(e => console.log('Could not play alert sound:', e));
}

// Function to send alert to server (optional, for more advanced implementations)
function sendAlertToServer(alertType) {
    fetch('/api/alerts', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            sessionId: document.getElementById('sessionId').value,
            driverId: document.getElementById('driverId').value,
            alertType: alertType,
            timestamp: new Date().toISOString()
        })
    }).catch(error => console.error('Error sending alert to server:', error));
}
```

## Step 3: Connect Notifications to Driver State
When analyzing frames in JavaScript, update to check driver state and show notifications:

```javascript
// Function that will be called periodically to process frames
function processVideoFrame() {
    if (!video.paused && !video.ended) {
        // Capture current frame from video
        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        
        // Get image data for analysis
        const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
        
        // Send to server for analysis (simplified example)
        const driverState = analyzeDriverState(imageData);
        
        // Update notification based on state
        if (driverState === 'DROWSY') {
            showDrowsyNotification();
        } else if (driverState === 'DISTRACTED') {
            showDistractedNotification();
        } else {
            showNormalNotification();
        }
    }
    
    // Process next frame
    requestAnimationFrame(processVideoFrame);
}

// Mock analysis function (will be replaced by actual backend call)
function analyzeDriverState(imageData) {
    // In a real implementation, this would send the frame to the backend
    // and get the result. For testing, we're using a mock.
    
    // Example of how to make an actual call:
    /*
    fetch('/api/analyze-frame', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            frameData: imageData.data,
            width: imageData.width,
            height: imageData.height,
            sessionId: document.getElementById('sessionId').value,
            driverId: document.getElementById('driverId').value
        })
    })
    .then(response => response.json())
    .then(data => {
        return data.driverState;
    })
    .catch(error => {
        console.error('Error analyzing frame:', error);
        return 'NORMAL';
    });
    */
    
    // For testing, return random states occasionally
    const rand = Math.random();
    if (rand < 0.05) return 'DROWSY';
    if (rand < 0.10) return 'DISTRACTED';
    return 'NORMAL';
}
```

## Step 4: Add Audio Alert Files
Create a directory for sounds:
- Create `src/main/resources/static/sounds/`
- Add two sound files:
  - `drowsy_alert.mp3` - Alert sound for drowsiness detection
  - `distracted_alert.mp3` - Alert sound for distraction detection

Sound files can be obtained from free sound libraries online.

## Step 5: Create Alert History Component
Add a collapsible component to show alert history:

```html
<div class="card mt-4">
    <div class="card-header" id="alertHistoryHeader">
        <h5 class="mb-0">
            <button class="btn btn-link" data-bs-toggle="collapse" data-bs-target="#alertHistoryContent">
                Alert History
            </button>
        </h5>
    </div>
    <div id="alertHistoryContent" class="collapse">
        <div class="card-body">
            <div class="table-responsive">
                <table class="table table-striped table-sm">
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>Time</th>
                            <th>Alert Type</th>
                        </tr>
                    </thead>
                    <tbody id="alertHistoryTable">
                        <!-- Alert history will be populated here -->
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script>
// Function to update alert history table
function updateAlertHistoryTable() {
    const tableBody = document.getElementById('alertHistoryTable');
    tableBody.innerHTML = '';
    
    alertHistory.forEach((alert, index) => {
        const row = document.createElement('tr');
        
        // Convert ISO timestamp to readable local time
        const time = new Date(alert.timestamp).toLocaleTimeString();
        
        row.innerHTML = `
            <td>${index + 1}</td>
            <td>${time}</td>
            <td><span class="badge ${alert.type === 'DROWSY' ? 'bg-danger' : 'bg-warning'}">${alert.type}</span></td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Update history table whenever a new alert is added
const originalLogAlert = logAlert;
logAlert = function(alertType) {
    originalLogAlert(alertType);
    updateAlertHistoryTable();
};
</script>
```

---

# Important Details
- Use simple Bootstrap alert classes: `alert-success`, `alert-warning`, `alert-danger`
- Only change notification when state changes (don't repeatedly show same alert)
- Make sure alert sounds are not too loud or annoying
- Maintain history of alerts for the current session only

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Simple and clean JS functions
- Proper code comments explaining each function's purpose
- Follow camelCase naming for JavaScript functions and variables

---

# Success Criteria
- Correct alert is shown within 1 second after the driver's state changes
- Notification color matches driver state
- Alert sound plays when alert state changes
- Alert count and history are maintained
- No error messages in browser console
- Code is simple, clean, and properly commented

---

# References
- [Bootstrap Alerts](https://getbootstrap.com/docs/5.0/components/alerts/)
- [Bootstrap Collapse](https://getbootstrap.com/docs/5.0/components/collapse/)
- [Web Audio API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API)

---

# End of TASK_07_Notification_System.txt
