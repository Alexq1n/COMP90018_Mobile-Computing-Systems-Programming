# RoamMate Sensor API Guide

A quick reference for team members integrating sensor data into the RoamMate Android app.

## 1. Available Sensor APIs

All APIs below belong to the **shared `SensorRepository`**. “Unavailable” describes the current implementation when a sensor has not supplied data; it is **not** a guaranteed hardware/permission status indicator.

| Sensor | API / Function | Kotlin type | Data returned | When unavailable / no data | Update method |
|---|---|---|---|---|---|
| **Step Counter** | `stepCountFlow` | `StateFlow<Int?>` | Cumulative steps since last reboot | Initially `null`; a previously received value may remain cached | **StateFlow** — updates on new step-count readings |
| **Step Counter** | `getCurrentSteps()` | `Int?` | Latest cumulative step count | `null` before any reading; may return a cached value after tracking stops | **Manual** — reads once |
| **Steps Last Hour** | `stepsLastHourFlow` | `StateFlow<Int?>` | Last published approximate hourly step count | Initially `null`; previous estimate may remain cached | **StateFlow** — updates when repository publishes a new estimate |
| **Steps Last Hour** | `getStepsLastHour()` | `Int?` | Recalculated approximate hourly steps | Can return `0` when there is insufficient step history, **even if the sensor is unavailable**; do not treat `0` as proof of no walking | **Manual** — recalculates when called |
| **Step Detector** | `stepEvents` | `SharedFlow<Long>` | One timestamp per detected step (milliseconds since device boot) | **No emission**; does not emit `null`, `0`, or `false` | **Event Flow** — one event per detected step |
| **GPS** | `locationFlow` | `StateFlow<LocationMessage?>` | Latest latitude and longitude | Initially `null`; last known value may remain cached after tracking stops | **StateFlow** — updates on location callbacks |
| **GPS** | `getCurrentLocation()` | `LocationMessage?` | Latest cached location (not a fresh GPS request) | `null` before a fix; may return the last known value later | **Manual** — reads once |
| **Shake Detector** | `shakeEvents` | `SharedFlow<Unit>` | One `Unit` event for each detected shake | **No emission**; does not emit `null`, `0`, or `false` | **Event Flow** — one event per shake |

### Permissions and availability

| Sensor | Android permission / requirement | Current behavior if unavailable |
|---|---|---|
| Step Counter | `ACTIVITY_RECOGNITION` runtime permission on Android 10+; device must have `TYPE_STEP_COUNTER` | `startStepCounter()` returns `false` if hardware is missing; foreground service stops its step tracking |
| Step Detector | Same activity-recognition permission; device must have `TYPE_STEP_DETECTOR` | `isStepDetectorAvailable()` returns `false` if hardware is missing; `stepEvents` stays silent |
| GPS | Location permission (`ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` in Android generally), location services and compatible provider | Current `LocationSensor` implementation checks **fine location only**; coarse-only authorization is not sufficient in the current code; no new fix means no new location value |
| Shake Detector | No runtime permission; device must have an accelerometer | `startShakeDetection()` returns `false` if the accelerometer is missing; `shakeEvents` stays silent |

> **Important:** Hardware availability, permission granted, listener successfully started, and a fresh reading received are **different states**. The current API does not expose one unified status for all four sensors. A cached value can persist even when a sensor is no longer running. A missing event is not evidence that a sensor is unavailable.

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
- **Do not infer availability from `0` or silence.** `getStepsLastHour()` may return `0` without enough history; event flows do not emit a special failure value.
- **Availability checks are limited.** `isStepDetectorAvailable()` checks hardware presence only; `startStepCounter()` and `startShakeDetection()` return a Boolean for startup, but the data-reading APIs do not return a dedicated error code.
- **Permissions are separate from data values.** Android 10+ step sensors require `ACTIVITY_RECOGNITION`; location requires location permission. Missing permission does not force every cached value back to `null`.
- **Sensor lifecycle is managed centrally. In the current app setup, `MainActivity` manages foreground Step Detector, GPS, and Shake tracking, while `SensorService` handles background Step Counter tracking.

> **Rule of thumb:** Use **StateFlow** to keep a screen updated, a **Manual Function** for a one-time read, and an **Event Flow** to react to an action.
