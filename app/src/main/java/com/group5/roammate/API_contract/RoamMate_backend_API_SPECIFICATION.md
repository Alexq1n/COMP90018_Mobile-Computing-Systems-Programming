# RoamMate Backend API Specification

---

## Table of Contents
1. [Core Trip Planning API](#1-core-trip-planning-api)
2. [Dynamic Adjustment API](#2-dynamic-adjustment-api)
3. [POI Query API](#3-poi-query-api)
4. [Weather Replanning API](#4-weather-replanning-api)
5. [Fatigue Detection API](#5-fatigue-detection-api)
6. [Itinerary Editing API](#6-itinerary-editing-api)
7. [Data Models](#7-data-models)

---

## 1. Core Trip Planning API

### 1.1 Generate Initial Trip

**Function Name:** `generateInitialTrip`

**Module:** `ITripPlannerEngine`

**Description:** Generates a complete multi-day trip itinerary based on user preferences, destination, and constraints.

#### Input Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `tripId` | String | Yes | - | Unique identifier for the trip |
| `destination` | String | Yes | - | City name (e.g., "Melbourne") |
| `startDate` | String | Yes | - | Trip start date (format: "yyyy-MM-dd") |
| `numberOfDays` | Int | Yes | - | Number of days for the trip |
| `userProfile` | UserProfile | Yes | - | User preferences and profile |
| `startingLocation` | Coordinates | Yes | - | Starting coordinates (e.g., hotel location) |
| `dailyStartTime` | String | No | "09:00" | Daily start time (format: "HH:mm") |
| `dailyEndTime` | String | No | "18:00" | Daily end time (format: "HH:mm") |
| `weatherStatus` | WeatherStatus | No | SUNNY | Initial weather status |
| `includeFillers` | Boolean | No | true | Whether to inject hidden gems |
| `pinnedPOIs` | List<PinnedPOI> | No | [] | User-specified POIs for specific days |

#### Output

**Type:** `Itinerary`

```json
{
  "tripId": "trip_12345",
  "days": [
    {
      "dayNumber": 1,
      "date": "2026-10-15",
      "pois": [
        {
          "poi": {
            "id": "poi_001",
            "name": "Queen Victoria Market",
            "baseScore": 8.5,
            "category": "SHOPPING",
            "budgetLevel": "MEDIUM",
            "environment": "OUTDOOR",
            "coordinates": {
              "latitude": -37.8076,
              "longitude": 144.9568
            },
            "recommendedVisitDuration": 90,
            "operatingHours": {
              "openTime": "06:00",
              "closeTime": "15:00"
            },
            "isFiller": false,
            "city": "Melbourne",
            "description": "Historic market with fresh produce and souvenirs"
          },
          "startTime": "09:00",
          "endTime": "10:30",
          "travelTimeFromPrevious": 0,
          "expectedValue": 8.2,
          "isFiller": false,
          "isUserPinned": false
        }
      ],
      "totalDuration": 540,
      "totalTravelTime": 45,
      "totalVisitTime": 495
    }
  ],
  "totalEstimatedCost": 250.0,
  "totalExpectedValue": 42.5
}
```

#### Example Request

```kotlin
val itinerary = tripPlannerEngine.generateInitialTrip(
    tripId = "trip_001",
    destination = "Melbourne",
    startDate = "2026-10-15",
    numberOfDays = 3,
    userProfile = UserProfile(
        interests = listOf(POICategory.MUSEUM, POICategory.PARK, POICategory.RESTAURANT),
        budgetPreference = BudgetLevel.MEDIUM,
        transportMode = TransportMode.PUBLIC_TRANSIT,
        transportPreference = 1.2f
    ),
    startingLocation = Coordinates(latitude = -37.8136, longitude = 144.9631),
    dailyStartTime = "09:00",
    dailyEndTime = "18:00",
    weatherStatus = WeatherStatus.SUNNY,
    includeFillers = true,
    pinnedPOIs = listOf(
        PinnedPOI(poiId = "poi_123", dayNumber = 2, preferredStartTime = "14:00")
    )
)
```

---

## 2. Dynamic Adjustment API

### 2.1 Adjust Trip State

**Function Name:** `adjustTripState`

**Module:** `ITripPlannerEngine`

**Description:** Dynamically adjusts the trip based on real-time context changes (delays, location, weather). Returns two strategic options for user selection.

#### Input Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `currentItinerary` | List<ItineraryPOI> | Yes | - | The current day's itinerary |
| `contextPayload` | ContextPayload | Yes | - | Real-time context (location, time, weather, delays) |
| `destination` | String | Yes | - | City name |
| `userProfile` | UserProfile | Yes | - | User preferences |
| `dailyEndTime` | String | No | "18:00" | Daily end time (format: "HH:mm") |

#### Output

**Type:** `AdjustmentResponse`

```json
{
  "optionA": {
    "strategy": "EXPERIENCE_FIRST",
    "remainingItinerary": [
      {
        "poi": {
          "id": "poi_002",
          "name": "National Gallery of Victoria",
          "baseScore": 9.0,
          "category": "MUSEUM",
          "budgetLevel": "LOW",
          "environment": "INDOOR",
          "coordinates": {
            "latitude": -37.8226,
            "longitude": 144.9689
          },
          "recommendedVisitDuration": 120,
          "operatingHours": {
            "openTime": "10:00",
            "closeTime": "17:00"
          },
          "isFiller": false,
          "city": "Melbourne"
        },
        "startTime": "14:30",
        "endTime": "16:30",
        "travelTimeFromPrevious": 15,
        "expectedValue": 8.8,
        "isFiller": false,
        "isUserPinned": false
      }
    ],
    "droppedPOIs": [
      {
        "id": "poi_005",
        "name": "Brighton Beach",
        "baseScore": 7.5,
        "category": "BEACH",
        "budgetLevel": "LOW",
        "environment": "OUTDOOR",
        "coordinates": {
          "latitude": -37.9265,
          "longitude": 145.0018
        },
        "recommendedVisitDuration": 60,
        "operatingHours": {
          "openTime": "00:00",
          "closeTime": "23:59"
        },
        "isFiller": false,
        "city": "Melbourne"
      }
    ],
    "totalExpectedValue": 8.8,
    "totalTravelTime": 15,
    "description": "Maximize experience value by prioritizing high-EV indoor activities"
  },
  "optionB": {
    "strategy": "EFFICIENCY_FIRST",
    "remainingItinerary": [
      {
        "poi": {
          "id": "poi_003",
          "name": "Federation Square",
          "baseScore": 7.8,
          "category": "LANDMARK",
          "budgetLevel": "LOW",
          "environment": "OUTDOOR",
          "coordinates": {
            "latitude": -37.8180,
            "longitude": 144.9692
          },
          "recommendedVisitDuration": 45,
          "operatingHours": {
            "openTime": "00:00",
            "closeTime": "23:59"
          },
          "isFiller": false,
          "city": "Melbourne"
        },
        "startTime": "14:15",
        "endTime": "15:00",
        "travelTimeFromPrevious": 8,
        "expectedValue": 7.5,
        "isFiller": false,
        "isUserPinned": false
      }
    ],
    "droppedPOIs": [
      {
        "id": "poi_005",
        "name": "Brighton Beach",
        "baseScore": 7.5,
        "category": "BEACH",
        "budgetLevel": "LOW",
        "environment": "OUTDOOR",
        "coordinates": {
          "latitude": -37.9265,
          "longitude": 145.0018
        },
        "recommendedVisitDuration": 60,
        "operatingHours": {
          "openTime": "00:00",
          "closeTime": "23:59"
        },
        "isFiller": false,
        "city": "Melbourne"
      }
    ],
    "totalExpectedValue": 7.5,
    "totalTravelTime": 8,
    "description": "Maximize efficiency by prioritizing nearby POIs with best EV/travel-time ratio"
  }
}
```

#### Example Request

```kotlin
val adjustmentResponse = tripPlannerEngine.adjustTripState(
    currentItinerary = listOf(
        ItineraryPOI(
            poi = poi1,
            startTime = "09:00",
            endTime = "11:00",
            travelTimeFromPrevious = 0,
            expectedValue = 8.5,
            isFiller = false,
            isUserPinned = false
        )
    ),
    contextPayload = ContextPayload(
        currentLocation = Coordinates(latitude = -37.8136, longitude = 144.9631),
        currentTime = "2026-10-15T14:30:00",
        weatherStatus = WeatherStatus.SUNNY,
        currentWeather = WeatherStatus.RAINY,
        itineraryProgress = ItineraryProgress(
            completedPOIs = listOf("poi_001", "poi_002"),
            delayMinutes = 45
        ),
        recentStepCount = 4200
    ),
    destination = "Melbourne",
    userProfile = userProfile,
    dailyEndTime = "18:00"
)
```

---

## 3. POI Query API

### 3.1 Get POIs for Destination

**Function Name:** `getPOIsForDestination`

**Module:** `ITripPlannerEngine`

**Description:** Retrieves all POIs for a specific destination, optionally including filler POIs (hidden gems).

#### Input Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `destination` | String | Yes | - | City name |
| `includeFillers` | Boolean | No | false | Whether to include filler POIs |

#### Output

**Type:** `List<POI>`

```json
[
  {
    "id": "poi_001",
    "name": "Queen Victoria Market",
    "baseScore": 8.5,
    "category": "SHOPPING",
    "budgetLevel": "MEDIUM",
    "environment": "OUTDOOR",
    "coordinates": {
      "latitude": -37.8076,
      "longitude": 144.9568
    },
    "recommendedVisitDuration": 90,
    "operatingHours": {
      "openTime": "06:00",
      "closeTime": "15:00"
    },
    "isFiller": false,
    "city": "Melbourne",
    "description": "Historic market with fresh produce and souvenirs"
  },
  {
    "id": "filler_001",
    "name": "Hosier Lane Street Art",
    "baseScore": 7.2,
    "category": "CULTURAL",
    "budgetLevel": "LOW",
    "environment": "OUTDOOR",
    "coordinates": {
      "latitude": -37.8172,
      "longitude": 144.9689
    },
    "recommendedVisitDuration": 15,
    "operatingHours": {
      "openTime": "00:00",
      "closeTime": "23:59"
    },
    "isFiller": true,
    "city": "Melbourne",
    "description": "Famous laneway with vibrant street art"
  }
]
```

#### Example Request

```kotlin
val allPOIs = tripPlannerEngine.getPOIsForDestination(
    destination = "Melbourne",
    includeFillers = true
)
```

### 3.2 Calculate POI Value

**Function Name:** `calculatePOIValue`

**Module:** `ITripPlannerEngine`

**Description:** Calculates the Expected Value (EV) score for a specific POI based on user preferences and weather conditions.

#### Input Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `poi` | POI | Yes | - | The POI to evaluate |
| `userProfile` | UserProfile | Yes | - | User preferences |
| `weatherStatus` | WeatherStatus | No | SUNNY | Current weather |

#### Output

**Type:** `Double`

```json
8.42
```

#### Example Request

```kotlin
val evScore = tripPlannerEngine.calculatePOIValue(
    poi = poi,
    userProfile = userProfile,
    weatherStatus = WeatherStatus.RAINY
)
```

---

## 4. Weather Replanning API

### 4.1 Handle Rainy Day Scenario

**Function Name:** `handleRainyDayScenario`

**Module:** `WeatherReplanningService`

**Description:** Generates three alternative itineraries when rainy weather is detected: original plan, experience-first alternative (indoor activities), and efficiency-first alternative.

#### Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `currentItinerary` | DayItinerary | Yes | Current day's planned itinerary |
| `contextPayload` | ContextPayload | Yes | Real-time context with weather data |
| `allPOIs` | List<POI> | Yes | All available POIs for the destination |
| `startLocation` | Coordinates | Yes | Current location |
| `availableTimeMinutes` | Int | Yes | Remaining time available in minutes |

#### Output

**Type:** `RainyDayOptions`

```json
{
  "original": {
    "dayNumber": 1,
    "date": "2026-10-15",
    "pois": [
      {
        "poi": {
          "id": "poi_004",
          "name": "Royal Botanic Gardens",
          "baseScore": 8.0,
          "category": "PARK",
          "budgetLevel": "LOW",
          "environment": "OUTDOOR",
          "coordinates": {
            "latitude": -37.8304,
            "longitude": 144.9796
          },
          "recommendedVisitDuration": 90,
          "operatingHours": {
            "openTime": "07:30",
            "closeTime": "17:30"
          },
          "isFiller": false,
          "city": "Melbourne"
        },
        "startTime": "14:00",
        "endTime": "15:30",
        "travelTimeFromPrevious": 10,
        "expectedValue": 7.8,
        "isFiller": false,
        "isUserPinned": false
      }
    ],
    "totalDuration": 90,
    "totalTravelTime": 10,
    "totalVisitTime": 90
  },
  "experienceFirst": {
    "dayNumber": 1,
    "date": "2026-10-15",
    "pois": [
      {
        "poi": {
          "id": "poi_002",
          "name": "National Gallery of Victoria",
          "baseScore": 9.0,
          "category": "MUSEUM",
          "budgetLevel": "LOW",
          "environment": "INDOOR",
          "coordinates": {
            "latitude": -37.8226,
            "longitude": 144.9689
          },
          "recommendedVisitDuration": 120,
          "operatingHours": {
            "openTime": "10:00",
            "closeTime": "17:00"
          },
          "isFiller": false,
          "city": "Melbourne"
        },
        "startTime": "14:00",
        "endTime": "16:00",
        "travelTimeFromPrevious": 12,
        "expectedValue": 8.9,
        "isFiller": false,
        "isUserPinned": false
      }
    ],
    "totalDuration": 120,
    "totalTravelTime": 12,
    "totalVisitTime": 120
  },
  "efficiencyFirst": {
    "dayNumber": 1,
    "date": "2026-10-15",
    "pois": [
      {
        "poi": {
          "id": "poi_006",
          "name": "Melbourne Aquarium",
          "baseScore": 7.5,
          "category": "ENTERTAINMENT",
          "budgetLevel": "MEDIUM",
          "environment": "INDOOR",
          "coordinates": {
            "latitude": -37.8206,
            "longitude": 144.9580
          },
          "recommendedVisitDuration": 90,
          "operatingHours": {
            "openTime": "09:30",
            "closeTime": "18:00"
          },
          "isFiller": false,
          "city": "Melbourne"
        },
        "startTime": "14:00",
        "endTime": "15:30",
        "travelTimeFromPrevious": 8,
        "expectedValue": 7.3,
        "isFiller": false,
        "isUserPinned": false
      }
    ],
    "totalDuration": 90,
    "totalTravelTime": 8,
    "totalVisitTime": 90
  },
  "triggerReason": "Rainy weather detected with 2 outdoor activities scheduled"
}
```

#### Example Request

```kotlin
val weatherReplanningService = WeatherReplanningService(userProfile, travelDurationMatrix)

val rainyOptions = weatherReplanningService.handleRainyDayScenario(
    currentItinerary = currentDay,
    contextPayload = ContextPayload(
        currentLocation = Coordinates(-37.8136, 144.9631),
        currentTime = "2026-10-15T13:45:00",
        weatherStatus = WeatherStatus.SUNNY,
        currentWeather = WeatherStatus.RAINY,
        itineraryProgress = ItineraryProgress(
            completedPOIs = listOf("poi_001"),
            delayMinutes = 15
        ),
        recentStepCount = 2500
    ),
    allPOIs = allPOIs,
    startLocation = Coordinates(-37.8136, 144.9631),
    availableTimeMinutes = 240
)
```

---

## 5. Fatigue Detection API

### 5.1 Analyze Fatigue Status

**Function Name:** `analyzeFatigueStatus`

**Module:** `FatigueDetectionService`

**Description:** Analyzes user fatigue based on recent step count and provides recommendations.

#### Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `contextPayload` | ContextPayload | Yes | Context payload containing recent step count |

#### Output

**Type:** `FatigueStatus`

```json
{
  "level": "MEDIUM",
  "recommendedAction": "Moderate activity detected. Consider shorter visits or rest breaks.",
  "adjustedItinerary": null
}
```

#### Example Request

```kotlin
val fatigueDetectionService = FatigueDetectionService(userProfile, travelDurationMatrix)

val fatigueStatus = fatigueDetectionService.analyzeFatigueStatus(
    contextPayload = ContextPayload(
        currentLocation = Coordinates(-37.8136, 144.9631),
        currentTime = "2026-10-15T15:30:00",
        weatherStatus = WeatherStatus.SUNNY,
        currentWeather = null,
        itineraryProgress = ItineraryProgress(
            completedPOIs = listOf("poi_001", "poi_002"),
            delayMinutes = 0
        ),
        recentStepCount = 4200
    )
)
```

### 5.2 Adjust Itinerary for Fatigue

**Function Name:** `adjustItineraryForFatigue`

**Module:** `FatigueDetectionService`

**Description:** Adjusts the remaining itinerary based on detected fatigue level, prioritizing shorter, nearby, and less intense activities.

#### Input Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `remainingItinerary` | List<ItineraryPOI> | Yes | - | Remaining planned activities |
| `allPOIs` | List<POI> | Yes | - | All available POIs |
| `fatigueLevel` | FatigueLevel | Yes | - | Detected fatigue level (LOW, MEDIUM, HIGH) |
| `currentLocation` | Coordinates | Yes | - | Current location |
| `availableMinutes` | Int | Yes | - | Remaining time in minutes |
| `weatherStatus` | WeatherStatus | No | SUNNY | Current weather |
| `dayNumber` | Int | Yes | - | Current day number |
| `date` | String | Yes | - | Current date (format: "yyyy-MM-dd") |

#### Output

**Type:** `DayItinerary`

```json
{
  "dayNumber": 1,
  "date": "2026-10-15",
  "pois": [
    {
      "poi": {
        "id": "poi_007",
        "name": "Degrave Street Cafe",
        "baseScore": 7.0,
        "category": "CAFE",
        "budgetLevel": "MEDIUM",
        "environment": "INDOOR",
        "coordinates": {
          "latitude": -37.8149,
          "longitude": 144.9685
        },
        "recommendedVisitDuration": 45,
        "operatingHours": {
          "openTime": "07:00",
          "closeTime": "19:00"
        },
        "isFiller": false,
        "city": "Melbourne"
      },
      "startTime": "16:00",
      "endTime": "16:45",
      "travelTimeFromPrevious": 5,
      "expectedValue": 6.5,
      "isFiller": false,
      "isUserPinned": false
    }
  ],
  "totalDuration": 50,
  "totalTravelTime": 5,
  "totalVisitTime": 45
}
```

#### Example Request

```kotlin
val adjustedItinerary = fatigueDetectionService.adjustItineraryForFatigue(
    remainingItinerary = remainingPOIs,
    allPOIs = allPOIs,
    fatigueLevel = FatigueLevel.HIGH,
    currentLocation = Coordinates(-37.8136, 144.9631),
    availableMinutes = 120,
    weatherStatus = WeatherStatus.SUNNY,
    dayNumber = 1,
    date = "2026-10-15"
)
```

---

## 6. Itinerary Editing API

### 6.1 Add POI to Itinerary

**Function Name:** `addPoiToItinerary`

**Module:** `ItineraryEditService`

**Description:** Adds a POI to a specific day at a specific position, recalculating subsequent timings.

#### Input Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `itinerary` | Itinerary | Yes | - | Current complete itinerary |
| `poi` | POI | Yes | - | POI to add |
| `targetDayIndex` | Int | Yes | - | Zero-based day index |
| `insertPosition` | Int | Yes | - | Position in the day's POI list |
| `weatherStatus` | WeatherStatus | No | SUNNY | Current weather |

#### Output

**Type:** `EditResult`

```json
{
  "success": true,
  "validationMessage": "POI added successfully",
  "updatedDay": {
    "dayNumber": 2,
    "date": "2026-10-16",
    "pois": [
      {
        "poi": {
          "id": "poi_008",
          "name": "Eureka Skydeck",
          "baseScore": 8.3,
          "category": "LANDMARK",
          "budgetLevel": "MEDIUM",
          "environment": "INDOOR",
          "coordinates": {
            "latitude": -37.8212,
            "longitude": 144.9646
          },
          "recommendedVisitDuration": 60,
          "operatingHours": {
            "openTime": "10:00",
            "closeTime": "22:00"
          },
          "isFiller": false,
          "city": "Melbourne"
        },
        "startTime": "10:00",
        "endTime": "11:00",
        "travelTimeFromPrevious": 0,
        "expectedValue": 8.1,
        "isFiller": false,
        "isUserPinned": true
      }
    ],
    "totalDuration": 60,
    "totalTravelTime": 0,
    "totalVisitTime": 60
  },
  "freedTimeMinutes": 0
}
```

#### Example Request

```kotlin
val itineraryEditService = ItineraryEditService(userProfile, travelDurationMatrix)

val editResult = itineraryEditService.addPoiToItinerary(
    itinerary = currentItinerary,
    poi = newPOI,
    targetDayIndex = 1,
    insertPosition = 2,
    weatherStatus = WeatherStatus.SUNNY
)
```

### 6.2 Remove POI from Itinerary

**Function Name:** `removePoiFromItinerary`

**Module:** `ItineraryEditService`

**Description:** Removes a POI from the itinerary and recalculates timings for subsequent POIs.

#### Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `itinerary` | Itinerary | Yes | Current complete itinerary |
| `poiId` | String | Yes | ID of the POI to remove |
| `dayIndex` | Int | Yes | Zero-based day index |

#### Output

**Type:** `EditResult`

```json
{
  "success": true,
  "validationMessage": "POI removed successfully",
  "updatedDay": {
    "dayNumber": 2,
    "date": "2026-10-16",
    "pois": [],
    "totalDuration": 0,
    "totalTravelTime": 0,
    "totalVisitTime": 0
  },
  "freedTimeMinutes": 105
}
```

#### Example Request

```kotlin
val editResult = itineraryEditService.removePoiFromItinerary(
    itinerary = currentItinerary,
    poiId = "poi_008",
    dayIndex = 1
)
```

### 6.3 Reoptimize Day

**Function Name:** `reoptimizeDay`

**Module:** `ItineraryEditService`

**Description:** Reoptimizes a day's itinerary after manual edits, preserving user-pinned POIs and filling remaining time with optimal selections.

#### Input Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `dayItinerary` | DayItinerary | Yes | - | Day to reoptimize |
| `allPOIs` | List<POI> | Yes | - | All available POIs |
| `availableTimeMinutes` | Int | Yes | - | Total available time in minutes |
| `weatherStatus` | WeatherStatus | No | SUNNY | Current weather |

#### Output

**Type:** `DayItinerary`

```json
{
  "dayNumber": 2,
  "date": "2026-10-16",
  "pois": [
    {
      "poi": {
        "id": "poi_009",
        "name": "Melbourne Museum",
        "baseScore": 8.7,
        "category": "MUSEUM",
        "budgetLevel": "LOW",
        "environment": "INDOOR",
        "coordinates": {
          "latitude": -37.8033,
          "longitude": 144.9717
        },
        "recommendedVisitDuration": 150,
        "operatingHours": {
          "openTime": "10:00",
          "closeTime": "17:00"
        },
        "isFiller": false,
        "city": "Melbourne"
      },
      "startTime": "10:00",
      "endTime": "12:30",
      "travelTimeFromPrevious": 0,
      "expectedValue": 8.5,
      "isFiller": false,
      "isUserPinned": false
    }
  ],
  "totalDuration": 150,
  "totalTravelTime": 0,
  "totalVisitTime": 150
}
```

#### Example Request

```kotlin
val optimizedDay = itineraryEditService.reoptimizeDay(
    dayItinerary = dayToOptimize,
    allPOIs = allPOIs,
    availableTimeMinutes = 540,
    weatherStatus = WeatherStatus.SUNNY
)
```

---

## 7. Data Models

### 7.1 UserProfile

```kotlin
data class UserProfile(
    val interests: List<POICategory>,
    val budgetPreference: BudgetLevel,
    val transportMode: TransportMode,
    val transportPreference: Float = 1.0f
)
```

**Example:**
```json
{
  "interests": ["MUSEUM", "PARK", "RESTAURANT"],
  "budgetPreference": "MEDIUM",
  "transportMode": "PUBLIC_TRANSIT",
  "transportPreference": 1.2
}
```

### 7.2 ContextPayload

```kotlin
data class ContextPayload(
    val currentLocation: Coordinates,
    val currentTime: String,
    val weatherStatus: WeatherStatus,
    val currentWeather: WeatherStatus?,
    val itineraryProgress: ItineraryProgress,
    val recentStepCount: Int?
)
```

**Example:**
```json
{
  "currentLocation": {
    "latitude": -37.8136,
    "longitude": 144.9631
  },
  "currentTime": "2026-10-15T14:30:00",
  "weatherStatus": "SUNNY",
  "currentWeather": "RAINY",
  "itineraryProgress": {
    "completedPOIs": ["poi_001", "poi_002"],
    "delayMinutes": 30
  },
  "recentStepCount": 4200
}
```

### 7.3 PinnedPOI

```kotlin
data class PinnedPOI(
    val poiId: String,
    val dayNumber: Int,
    val preferredStartTime: String?
)
```

**Example:**
```json
{
  "poiId": "poi_123",
  "dayNumber": 2,
  "preferredStartTime": "14:00"
}
```

### 7.4 Enumerations

#### POICategory
```
MUSEUM, PARK, RESTAURANT, SHOPPING, ENTERTAINMENT, HISTORICAL, 
NATURE, BEACH, SPORTS, CULTURAL, NIGHTLIFE, CAFE, LANDMARK, SCENIC_SPOT
```

#### BudgetLevel
```
LOW, MEDIUM, HIGH
```

#### TransportMode
```
WALKING, PUBLIC_TRANSIT, DRIVING, CYCLING
```

#### WeatherStatus
```
SUNNY, CLOUDY, RAINY, STORMY, SNOWY
```

#### Environment
```
INDOOR, OUTDOOR, MIXED
```

#### FatigueLevel
```
LOW, MEDIUM, HIGH
```

#### AdjustmentStrategy
```
EXPERIENCE_FIRST, EFFICIENCY_FIRST
```

---

## Integration Flow Examples

### Example 1: Complete Trip Planning Flow

```kotlin
// Step 1: Initialize user profile
val userProfile = UserProfile(
    interests = listOf(POICategory.MUSEUM, POICategory.PARK),
    budgetPreference = BudgetLevel.MEDIUM,
    transportMode = TransportMode.PUBLIC_TRANSIT,
    transportPreference = 1.0f
)

// Step 2: Generate initial trip
val itinerary = tripPlannerEngine.generateInitialTrip(
    tripId = "trip_001",
    destination = "Melbourne",
    startDate = "2026-10-15",
    numberOfDays = 3,
    userProfile = userProfile,
    startingLocation = Coordinates(-37.8136, 144.9631),
    includeFillers = true
)

// Step 3: During the trip - adjust based on context
val contextPayload = ContextPayload(
    currentLocation = Coordinates(-37.8200, 144.9700),
    currentTime = "2026-10-15T14:30:00",
    weatherStatus = WeatherStatus.SUNNY,
    currentWeather = WeatherStatus.RAINY,
    itineraryProgress = ItineraryProgress(
        completedPOIs = listOf("poi_001"),
        delayMinutes = 45
    ),
    recentStepCount = 5200
)

val adjustment = tripPlannerEngine.adjustTripState(
    currentItinerary = itinerary.days[0].pois,
    contextPayload = contextPayload,
    destination = "Melbourne",
    userProfile = userProfile
)

// User selects optionA or optionB
```

### Example 2: Manual Itinerary Editing Flow

```kotlin
// Step 1: User wants to add a POI
val editService = ItineraryEditService(userProfile)

val addResult = editService.addPoiToItinerary(
    itinerary = currentItinerary,
    poi = selectedPOI,
    targetDayIndex = 1,
    insertPosition = 2
)

// Step 2: User wants to remove a POI
val removeResult = editService.removePoiFromItinerary(
    itinerary = currentItinerary,
    poiId = "poi_005",
    dayIndex = 1
)

// Step 3: Reoptimize the day
val allPOIs = tripPlannerEngine.getPOIsForDestination("Melbourne")
val optimized = editService.reoptimizeDay(
    dayItinerary = currentItinerary.days[1],
    allPOIs = allPOIs,
    availableTimeMinutes = 540
)
```

---

## Error Handling

All API functions may return validation errors embedded in response structures. Common error scenarios:

1. **Invalid day index**: When `dayIndex` is out of bounds
2. **POI not found**: When a referenced POI ID doesn't exist
3. **Time constraint violation**: When adding a POI exceeds available time
4. **Invalid date format**: When date strings don't match "yyyy-MM-dd" format
5. **Invalid time format**: When time strings don't match "HH:mm" format

Example error response:
```json
{
  "success": false,
  "validationMessage": "Not enough time to add POI. Required: 120 min, Available: 90 min",
  "updatedDay": null,
  "freedTimeMinutes": 0
}
```

