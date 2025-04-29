# TASK_07_Notification_System_MediaPipe.txt

# Task Title
Implement Driver Notification System with MediaPipe Integration

---

# Goal
Develop a frontend notification system that:
- Показывает визуальные уведомления, когда MediaPipe обнаруживает сонливость или отвлечение водителя
- Меняет внешний вид интерфейса в зависимости от текущего состояния
- Помогает водителю мгновенно осознать свое состояние
- Ведет журнал уведомлений с временными метками для последующего анализа

---

# Why This Task Is Important
- Мгновенная обратная связь может предотвратить аварии
- Простые предупреждения напоминают о важности сохранения внимания
- Критический компонент человеко-ориентированного дизайна
- История уведомлений может анализироваться в контексте сессии

---

# Prerequisites
Before starting this task:
- Complete `TASK_05_Face_and_Eye_Detection_MediaPipe.txt` with MediaPipe integration.
- Complete `TASK_06_Event_Logging_Service.txt`.
- Complete `TASK_03_Create_Driver_Screen.txt` with session support.
- Review `CODING_STANDARDS.txt`.
- Understand basic Bootstrap alerts.

---

# Detailed Instructions

## Step 1: Расширьте UI для отображения уведомлений
Добавьте или улучшите существующие стили в файле CSS для различных состояний:

```css
/* Стили для разных состояний */
.status-normal {
    background-color: rgba(40, 167, 69, 0.9);
    color: white;
    border: 2px solid #28a745;
}

.status-distracted {
    background-color: rgba(255, 193, 7, 0.9);
    color: black;
    border: 2px solid #ffc107;
    animation: pulse-warning 2s infinite;
}

.status-drowsy {
    background-color: rgba(220, 53, 69, 0.9);
    color: white;
    border: 2px solid #dc3545;
    animation: pulse-danger 1s infinite;
}

/* Анимации для привлечения внимания */
@keyframes pulse-warning {
    0% { box-shadow: 0 0 0 0 rgba(255, 193, 7, 0.7); }
    70% { box-shadow: 0 0 0 10px rgba(255, 193, 7, 0); }
    100% { box-shadow: 0 0 0 0 rgba(255, 193, 7, 0); }
}

@keyframes pulse-danger {
    0% { box-shadow: 0 0 0 0 rgba(220, 53, 69, 0.7); }
    70% { box-shadow: 0 0 0 15px rgba(220, 53, 69, 0); }
    100% { box-shadow: 0 0 0 0 rgba(220, 53, 69, 0); }
}

/* Стили для истории уведомлений */
.alert-history {
    max-height: 300px;
    overflow-y: auto;
    margin-top: 20px;
}

.alert-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    padding: 6px 10px;
    border-radius: 4px;
}

.alert-item-normal { background-color: rgba(40, 167, 69, 0.2); }
.alert-item-distracted { background-color: rgba(255, 193, 7, 0.2); }
.alert-item-drowsy { background-color: rgba(220, 53, 69, 0.2); }
```

## Step 2: Улучшите функции уведомлений в JavaScript
Расширьте функции уведомлений, добавив поддержку звуков и вибрации:

```javascript
// Глобальные переменные для отслеживания состояния
let alertHistory = [];
let alertCount = 0;
let notificationSound = true;
let vibrationEnabled = true;
let lastAlertTime = 0;
const ALERT_COOLDOWN = 5000; // 5 секунд между уведомлениями

// Функция обновления UI состояния с интеграцией MediaPipe
function updateDriverStateUI(state, metadata = {}) {
    // Текущее время
    const now = Date.now();
    
    // Обновляем UI только если прошло достаточно времени с последнего уведомления
    // или если состояние изменилось на более критичное
    const isMoreCritical = 
        (state === 'DROWSY' && currentState !== 'DROWSY') || 
        (state === 'DISTRACTED' && currentState === 'NORMAL');
    
    if (now - lastAlertTime > ALERT_COOLDOWN || isMoreCritical) {
        lastAlertTime = now;
        
        // Обновляем статус индикатор
        const statusIndicator = document.getElementById('statusIndicator');
        statusIndicator.className = 'status-indicator';
        
        // Добавляем уведомление в соответствии с состоянием
        switch(state) {
            case 'DROWSY':
                showDrowsyNotification();
                addToAlertHistory('DROWSY', metadata);
                break;
            case 'DISTRACTED':
                showDistractedNotification();
                addToAlertHistory('DISTRACTED', metadata);
                break;
            case 'NORMAL':
            default:
                showNormalNotification();
                break;
        }
    }
}

// Показывает уведомление о нормальном состоянии
function showNormalNotification() {
    document.getElementById('notificationArea').innerHTML = `
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <strong>Status:</strong> Normal - Stay alert!
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>`;
    
    document.getElementById('statusIndicator').classList.add('status-normal');
    document.getElementById('statusIndicator').textContent = 'Status: Normal';
}

// Показывает уведомление о отвлечении
function showDistractedNotification() {
    document.getElementById('notificationArea').innerHTML = `
        <div class="alert alert-warning alert-dismissible fade show" role="alert">
            <strong>Warning:</strong> Eyes on the road! You appear distracted.
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>`;
    
    document.getElementById('statusIndicator').classList.add('status-distracted');
    document.getElementById('statusIndicator').textContent = 'Status: Distracted - Focus on the road';
    
    // Проигрываем звук и вибрацию
    playAlert('distracted');
}

// Показывает уведомление о сонливости
function showDrowsyNotification() {
    document.getElementById('notificationArea').innerHTML = `
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <strong>Warning:</strong> You appear drowsy! Consider taking a break.
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>`;
    
    document.getElementById('statusIndicator').classList.add('status-drowsy');
    document.getElementById('statusIndicator').textContent = 'Status: Drowsy - Take a break!';
    
    // Проигрываем звук и вибрацию с высоким приоритетом
    playAlert('drowsy', true);
}

// Воспроизводит звуковое и вибро уведомление
function playAlert(type, highPriority = false) {
    // Воспроизводим звук, если включено
    if (notificationSound) {
        try {
            const audioFile = type === 'drowsy' ? 'drowsy_alert.mp3' : 'distracted_alert.mp3';
            const audio = new Audio(`/sounds/${audioFile}`);
            audio.volume = highPriority ? 0.8 : 0.6;
            const playPromise = audio.play();
            
            if (playPromise !== undefined) {
                playPromise.catch(error => {
                    console.warn('Ошибка воспроизведения звука:', error);
                });
            }
        } catch (e) {
            console.warn('Звуковое уведомление не поддерживается:', e);
        }
    }
    
    // Вибрация устройства, если поддерживается и включена
    if (vibrationEnabled && 'vibrate' in navigator) {
        try {
            if (highPriority) {
                // Длинная вибрация для высокоприоритетных уведомлений
                navigator.vibrate([200, 100, 200, 100, 200]);
            } else {
                // Короткая вибрация для обычных уведомлений
                navigator.vibrate(200);
            }
        } catch (e) {
            console.warn('Вибрация не поддерживается:', e);
        }
    }
}

// Добавляет уведомление в историю
function addToAlertHistory(alertType, metadata = {}) {
    // Увеличиваем счетчик уведомлений
    alertCount++;
    document.getElementById('alertCount').textContent = alertCount;
    
    // Создаем запись для истории
    const alertEntry = {
        id: alertCount,
        type: alertType,
        timestamp: new Date().toISOString(),
        metadata: metadata
    };
    
    // Добавляем в начало массива (новые уведомления вверху)
    alertHistory.unshift(alertEntry);
    
    // Ограничиваем размер истории (опционально)
    if (alertHistory.length > 50) {
        alertHistory.pop();
    }
    
    // Обновляем отображение истории
    updateAlertHistoryDisplay();
}

// Обновляет отображение истории уведомлений
function updateAlertHistoryDisplay() {
    const historyContainer = document.getElementById('alertHistoryTable');
    if (!historyContainer) return;
    
    historyContainer.innerHTML = '';
    
    alertHistory.forEach(alert => {
        const row = document.createElement('tr');
        
        // Форматируем время уведомления
        const timestamp = new Date(alert.timestamp);
        const formattedTime = timestamp.toLocaleTimeString();
        
        // Определяем класс и текст типа уведомления
        let typeText, typeClass;
        if (alert.type === 'DROWSY') {
            typeText = 'Drowsy';
            typeClass = 'badge bg-danger';
        } else {
            typeText = 'Distracted';
            typeClass = 'badge bg-warning text-dark';
        }
        
        // Создаем строку таблицы
        row.innerHTML = `
            <td>${alert.id}</td>
            <td>${formattedTime}</td>
            <td><span class="${typeClass}">${typeText}</span></td>
            <td>
                <button class="btn btn-sm btn-secondary" 
                        onclick="showAlertDetails(${alert.id})">
                    Details
                </button>
            </td>
        `;
        
        historyContainer.appendChild(row);
    });
}

// Показывает детали уведомления
function showAlertDetails(alertId) {
    const alert = alertHistory.find(a => a.id === alertId);
    if (!alert) return;
    
    // Создаем модальное окно с деталями
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.id = 'alertDetailsModal';
    modal.setAttribute('tabindex', '-1');
    
    // Форматируем метаданные для отображения
    let metadataHtml = '';
    if (alert.metadata) {
        const metadata = alert.metadata;
        Object.keys(metadata).forEach(key => {
            if (key !== 'timestamp') {
                let value = metadata[key];
                // Форматируем числовые значения
                if (typeof value === 'number') {
                    value = value.toFixed(4);
                }
                metadataHtml += `<tr><td>${key}</td><td>${value}</td></tr>`;
            }
        });
    }
    
    // Создаем содержимое модального окна
    modal.innerHTML = `
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Alert Details #${alert.id}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <p><strong>Type:</strong> ${alert.type}</p>
                    <p><strong>Time:</strong> ${new Date(alert.timestamp).toLocaleString()}</p>
                    
                    <h6>Additional Data:</h6>
                    <table class="table table-sm">
                        <thead>
                            <tr>
                                <th>Parameter</th>
                                <th>Value</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${metadataHtml}
                        </tbody>
                    </table>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    `;
    
    // Добавляем модальное окно на страницу
    document.body.appendChild(modal);
    
    // Инициализируем и показываем модальное окно Bootstrap
    const modalInstance = new bootstrap.Modal(modal);
    modalInstance.show();
    
    // Удаляем модальное окно после закрытия
    modal.addEventListener('hidden.bs.modal', function () {
        document.body.removeChild(modal);
    });
}
```

## Step 3: Добавьте компонент истории уведомлений
Добавьте компонент для отображения истории уведомлений:

```html
<div class="card mt-4">
    <div class="card-header">
        <h5 class="mb-0">
            <button class="btn btn-link" type="button" data-bs-toggle="collapse" data-bs-target="#alertHistoryContent">
                Alert History <span class="badge bg-secondary" id="alertCount">0</span>
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
                            <th>Type</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="alertHistoryTable">
                        <!-- История уведомлений будет добавлена динамически -->
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
```

## Step 4: Добавьте настройки уведомлений
Добавьте компонент для настройки уведомлений:

```html
<div class="card mt-4">
    <div class="card-header">
        <h5 class="mb-0">Alert Settings</h5>
    </div>
    <div class="card-body">
        <div class="form-check form-switch mb-2">
            <input class="form-check-input" type="checkbox" id="soundToggle" checked>
            <label class="form-check-label" for="soundToggle">Sound Notifications</label>
        </div>
        <div class="form-check form-switch">
            <input class="form-check-input" type="checkbox" id="vibrationToggle" checked>
            <label class="form-check-label" for="vibrationToggle">Vibration Alerts</label>
        </div>
    </div>
</div>

<script>
// Функционал для переключения настроек
document.addEventListener('DOMContentLoaded', function() {
    // Загружаем настройки из localStorage (если есть)
    notificationSound = localStorage.getItem('notificationSound') !== 'false';
    vibrationEnabled = localStorage.getItem('vibrationEnabled') !== 'false';
    
    // Устанавливаем начальные значения переключателей
    document.getElementById('soundToggle').checked = notificationSound;
    document.getElementById('vibrationToggle').checked = vibrationEnabled;
    
    // Обработчики событий для переключателей
    document.getElementById('soundToggle').addEventListener('change', function(e) {
        notificationSound = e.target.checked;
        localStorage.setItem('notificationSound', notificationSound);
    });
    
    document.getElementById('vibrationToggle').addEventListener('change', function(e) {
        vibrationEnabled = e.target.checked;
        localStorage.setItem('vibrationEnabled', vibrationEnabled);
    });
});
</script>
```

## Step 5: Добавьте файлы звуковых уведомлений
Создайте директорию для звуков и добавьте звуковые файлы:
- Создайте `src/main/resources/static/sounds/`
- Добавьте два звуковых файла:
  - `drowsy_alert.mp3` - для уведомлений о сонливости (должен быть настойчивым)
  - `distracted_alert.mp3` - для уведомлений о отвлечении (должен быть более мягким)

Вы можете найти бесплатные звуковые файлы на сайтах:
- https://freesound.org/
- https://mixkit.co/free-sound-effects/
- https://www.zapsplat.com/

## Step 6: Интеграция с MediaPipe
Обновите функции MediaPipe для связи с системой уведомлений:

```javascript
// Добавьте в файл с кодом MediaPipe

// Обновленная функция onResults для вызова функций уведомлений
function onResults(results) {
    // ... существующий код обработки результатов ...
    
    if (results.multiFaceLandmarks && results.multiFaceLandmarks.length > 0) {
        // Лицо обнаружено
        // ... существующий код определения EAR ...
        
        // Проверяем, закрыты ли глаза
        if (avgEAR < EAR_THRESHOLD) {
            // Глаза закрыты
            if (lastEyeCloseTime === null) {
                lastEyeCloseTime = Date.now();
            } else {
                const closeDuration = Date.now() - lastEyeCloseTime;
                
                // Обнаружена сонливость
                if (closeDuration > DROWSY_TIME) {
                    // Создаем метаданные для уведомления
                    const metadata = {
                        earValue: avgEAR,
                        closeDuration: closeDuration,
                        leftEAR: leftEAR,
                        rightEAR: rightEAR
                    };
                    
                    // Обновляем UI и отправляем данные на сервер
                    updateDriverStateUI('DROWSY', metadata);
                    sendEventToServer('DROWSY', closeDuration / 1000, metadata);
                }
            }
        } else {
            // Глаза открыты
            lastEyeCloseTime = null;
            
            // Если предыдущее состояние было не NORMAL, обновляем на NORMAL
            if (currentState !== 'NORMAL') {
                updateDriverStateUI('NORMAL');
            }
        }
    } else {
        // Лицо не обнаружено - водитель отвлечен
        const metadata = {
            faceDetected: false,
            timestamp: Date.now()
        };
        
        updateDriverStateUI('DISTRACTED', metadata);
        sendEventToServer('DISTRACTED', 1.0, metadata);
    }
}
```

---

# Preventing Common Errors

## Проблемы совместимости браузеров
- **Проверяйте поддержку API**: Используйте проверки для Audio API и Vibration API
- **Обеспечьте резервные варианты**: Если API не поддерживается, предоставляйте альтернативы

```javascript
// Правильная проверка поддержки API
function checkAPISupport() {
    // Проверка поддержки Vibration API
    const hasVibration = 'vibrate' in navigator;
    if (!hasVibration) {
        console.warn('Vibration API не поддерживается');
        document.getElementById('vibrationToggle').disabled = true;
        document.getElementById('vibrationToggle').nextElementSibling.innerHTML += ' (Не поддерживается)';
    }
    
    // Проверка поддержки Audio API
    let audioTest;
    try {
        audioTest = new Audio();
        if (!audioTest.canPlayType) {
            throw new Error('Audio not supported');
        }
    } catch (e) {
        console.warn('Audio API не поддерживается полностью');
        document.getElementById('soundToggle').disabled = true;
        document.getElementById('soundToggle').nextElementSibling.innerHTML += ' (Не поддерживается)';
    }
}
```

## Проблемы с воспроизведением звука
- **Обрабатывайте ошибки воспроизведения**: Современные браузеры требуют взаимодействия пользователя
- **Используйте переменную громкость**: Не делайте звуки слишком громкими

```javascript
// Подготовка звуков заранее
let drowsySound, distractedSound;

function preloadSounds() {
    try {
        drowsySound = new Audio('/sounds/drowsy_alert.mp3');
        distractedSound = new Audio('/sounds/distracted_alert.mp3');
        
        // Предзагрузка
        drowsySound.load();
        distractedSound.load();
        
        console.log('Sounds preloaded successfully');
    } catch (e) {
        console.warn('Could not preload sounds:', e);
    }
}

// Запускаем предзагрузку при первом взаимодействии пользователя
document.addEventListener('click', function initAudio() {
    preloadSounds();
    document.removeEventListener('click', initAudio);
});
```

## Слишком частые уведомления
- **Добавьте задержку между уведомлениями**: Избегайте раздражения пользователя
- **Приоритизируйте уведомления**: Сонливость важнее отвлечения

```javascript
// Правильная обработка приоритетов и задержек
let alertCooldowns = {
    DROWSY: 0,
    DISTRACTED: 0
};

const COOLDOWN_TIMES = {
    DROWSY: 5000,      // 5 секунд между уведомлениями о сонливости
    DISTRACTED: 10000  // 10 секунд между уведомлениями об отвлечении
};

function shouldShowNotification(state) {
    const now = Date.now();
    
    // Всегда показываем первое уведомление
    if (alertCount === 0) return true;
    
    // Проверяем, прошло ли достаточно времени с последнего уведомления этого типа
    if (now - alertCooldowns[state] < COOLDOWN_TIMES[state]) {
        return false;
    }
    
    // Сонливость имеет приоритет над отвлечением
    if (state === 'DROWSY') {
        alertCooldowns[state] = now;
        return true;
    }
    
    // Отвлечение показываем только если нет недавних уведомлений о сонливости
    if (state === 'DISTRACTED' && now - alertCooldowns['DROWSY'] > COOLDOWN_TIMES['DROWSY']) {
        alertCooldowns[state] = now;
        return true;
    }
    
    return false;
}
```

## Утечки памяти и ресурсов
- **Очищайте ресурсы**: Особенно при переключении между экранами
- **Ограничивайте размер истории**: Не храните слишком много уведомлений

```javascript
// Правильная очистка ресурсов
function cleanupResources() {
    // Останавливаем звуки, если они воспроизводятся
    if (drowsySound && !drowsySound.paused) drowsySound.pause();
    if (distractedSound && !distractedSound.paused) distractedSound.pause();
    
    // Ограничиваем размер истории
    if (alertHistory.length > 100) {
        alertHistory = alertHistory.slice(0, 100);
    }
}

// Вызываем очистку при выходе со страницы
window.addEventListener('beforeunload', cleanupResources);
```

---

# Coding Standards
You must follow all rules defined in `CODING_STANDARDS.txt`:
- Simple and clean JS functions
- Proper code comments explaining each function's purpose
- Follow camelCase naming for JavaScript functions and variables

---

# Success Criteria
- Правильное уведомление появляется в течение 1 секунды после изменения состояния водителя
- Цвет уведомления соответствует состоянию водителя
- Звуковое уведомление воспроизводится при изменении состояния
- История уведомлений ведется и отображается
- Отсутствуют ошибки в консоли браузера
- Код простой, чистый и правильно прокомментирован
- Настройки уведомлений сохраняются между перезагрузками страницы

---

# References
- [Bootstrap Alerts](https://getbootstrap.com/docs/5.0/components/alerts/)
- [Web Audio API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API)
- [Vibration API](https://developer.mozilla.org/en-US/docs/Web/API/Vibration_API)
- [LocalStorage API](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)

---

# End of TASK_07_Notification_System_MediaPipe.txt