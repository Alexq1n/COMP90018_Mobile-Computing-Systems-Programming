# RoamMate Sensor API Guide

A quick reference for team members integrating sensor data into the RoamMate Android app.

## 1. Available Sensor APIs

| Sensor | API / Function | Data returned | Update method |
|---|---|---|---|
| **Step Counter** | `stepCountFlow` | Cumulative steps since the device last rebooted | **StateFlow** — emits when the step count is updated |
| **Step Counter** | `getCurrentSteps()` | Latest available cumulative step count | **Manual** — reads the value once when called |
| **Steps Last Hour** | `stepsLastHourFlow` | Most recently published estimate of steps in the last hour | **StateFlow** — emits when the repository publishes an updated estimate |
| **Steps Last Hour** | `getStepsLastHour()` | Hourly step estimate calculated using the current system time | **Manual** — recalculates when called in the current implementation |
| **Step Detector** | `stepEvents` | An event (with timestamp) for each detected step | **Event Flow** — triggers on each detected step |
| **GPS** | `locationFlow` | Latest location (latitude and longitude) | **StateFlow** — emits when location is updated |
| **GPS** | `getCurrentLocation()` | Latest cached GPS location | **Manual** — reads the value once when called |
| **Shake Detector** | `shakeEvents` | A phone-shake event | **Event Flow** — triggers when a shake is detected |

## 2. Which API Style Should I Use?

| API style | What it does | Example use case |
|---|---|---|
| **Manual Function** | Retrieves a value on demand | Travel Strategy reads the current location or step count before running its algorithm |
| **StateFlow** | Observes the latest published value and updates the UI when it changes | Pet UI continuously displays steps or GPS location |
| **Event Flow** | Runs logic when an event occurs | Shake to refresh suggestions, or react to an individual step |

## 3. Quick Usage Examples

Use the **shared `SensorRepository` instance** provided by the app. Do not create a separate repository inside a screen.

### Manual Functions — read once

```kotlin
val steps = sensorRepository.getCurrentSteps()
val hourlySteps = sensorRepository.getStepsLastHour()
val location = sensorRepository.getCurrentLocation()

val latitude = location?.latitude
val longitude = location?.longitude
```

### StateFlow — observe values in Jetpack Compose

```kotlin
val steps by sensorRepository.stepCountFlow
    .collectAsStateWithLifecycle()

val hourlySteps by sensorRepository.stepsLastHourFlow
    .collectAsStateWithLifecycle()

val location by sensorRepository.locationFlow
    .collectAsStateWithLifecycle()
```

### Event Flow — react to sensor events in Jetpack Compose

```kotlin
LaunchedEffect(sensorRepository) {
    sensorRepository.stepEvents.collect { timestamp ->
        // Handle each detected step.
    }
}

LaunchedEffect(sensorRepository) {
    sensorRepository.shakeEvents.collect {
        // Handle each detected shake.
    }
}
```

## 4. Important Notes

- **Cumulative steps are not daily steps.** `stepCountFlow` and `getCurrentSteps()` report the step counter's cumulative value since the device last rebooted.
- **Last-hour steps are estimates.** `stepsLastHourFlow` and `getStepsLastHour()` do not represent an exact count over a complete rolling 60-minute history.
- **StateFlow does not recalculate as time passes.** `stepsLastHourFlow` changes only when the repository publishes a new value; the passage of time alone does not trigger a recalculation.
- **Event Flows represent occurrences, not stored totals.** `stepEvents` and `shakeEvents` are for reacting to individual sensor events.
- **Sensor lifecycle is managed centrally.** In the current app setup, `MainActivity` manages foreground Step Detector, GPS, and Shake tracking, while `SensorService` handles background Step Counter tracking.

> **Rule of thumb:** Use **StateFlow** to keep a screen updated, a **Manual Function** for a one-time read, and an **Event Flow** to react to an action.
