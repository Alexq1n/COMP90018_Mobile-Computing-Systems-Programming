# Upstream Data Contract

This document specifies all data required from upstream services for the trip planning engine.

## Data Interface Table

| Data Name | Description | Data Type | Required | Example Value | Notes |
|-----------|-------------|-----------|----------|---------------|-------|
| `destination` | Target city/region for the trip | String | Yes | "Melbourne" | Used for POI filtering |
| `trip_dates.start` | Trip start date | String (yyyy-MM-dd) | Yes | "2026-09-20" | ISO date format |
| `trip_dates.end` | Trip end date | String (yyyy-MM-dd) | Yes | "2026-09-25" | ISO date format |
| `user_profile.interests` | User's interest categories | List\<POICategory\> | Yes | ["MUSEUM", "NATURE", "RESTAURANT"] | Enum values from POICategory |
| `user_profile.transport_mode` | Primary transport method | TransportMode (Enum) | Yes | "WALKING" | WALKING or DRIVING only |
| `poi_master_data` | Complete POI database for destination | List\<POI\> | Yes | See POI Schema below | All available POIs including fillers |
| `travel_duration_matrix` | Pre-calculated travel times between POI pairs | List\<TravelDuration\> | Yes | See TravelDuration Schema | Walking and driving times |
| `context_payload.current_location.latitude` | User's current GPS latitude | Double | Yes | -37.8136 | Decimal degrees |
| `context_payload.current_location.longitude` | User's current GPS longitude | Double | Yes | 144.9631 | Decimal degrees |
| `context_payload.current_time` | Current timestamp | String (ISO 8601) | Yes | "2026-09-20T14:30:00Z" | ISO 8601 format |
| `context_payload.weather_status` | Historical/planned weather | WeatherStatus (Enum) | Yes | "SUNNY" | SUNNY, CLOUDY, RAINY, STORMY, SNOWY |
| `context_payload.current_weather` | Real-time weather when app opens | WeatherStatus (Enum) | Yes | "RAINY" | Triggers rainy day replanning if outdoor activities scheduled |
| `context_payload.itinerary_progress.completed_pois` | List of completed POI IDs | List\<String\> | Yes | ["poi_001", "poi_005"] | POI IDs user has already visited |
| `context_payload.itinerary_progress.delay_minutes` | Detected delay from original schedule | Int | Yes | 120 | Positive = behind schedule, 0 = on time |
| `context_payload.recent_step_count` | Steps in last 1 hour | Int | Yes | 4500 | Used for fatigue detection |
| `edit_action` | User edit operation type | EditAction (Enum) | Yes | "ADD_POI" | ADD_POI, REMOVE_POI, REOPTIMIZE |
| `edit_params.poi_id` | Target POI ID for edit operation | String | Yes | "poi_123" | Required for ADD_POI and REMOVE_POI |
| `edit_params.day_index` | Target day index (0-based) | Int | Yes | 2 | Day 1 = index 0 |
| `edit_params.insert_position` | Position for POI insertion | Int | Yes | 3 | Index within day's POI list, required for ADD_POI |
| `edit_params.available_time_minutes` | Available time budget for reoptimization | Int | Yes | 540 | Minutes available, required for REOPTIMIZE |

## Data Schemas

### POI Schema

```kotlin
data class POI(
    val id: String,                          // Unique identifier, e.g., "poi_001"
    val name: String,                        // Display name, e.g., "National Gallery of Victoria"
    val baseScore: Double,                   // Base quality score: 0.0 - 10.0
    val category: POICategory,               // MUSEUM, PARK, RESTAURANT, etc.
    val environment: Environment,            // INDOOR, OUTDOOR, MIXED
    val coordinates: Coordinates,            // GPS location
    val recommendedVisitDuration: Int,       // Minutes, e.g., 120
    val operatingHours: OperatingHours,      // Open/close times
    val isFiller: Boolean,                   // true for Hidden Gems, false for main POIs
    val city: String,                        // City name for filtering
    val description: String?                 // Optional description
)

data class Coordinates(
    val latitude: Double,                    // Decimal degrees, e.g., -37.8136
    val longitude: Double                    // Decimal degrees, e.g., 144.9631
)

data class OperatingHours(
    val openTime: String,                    // Format: "HH:mm", e.g., "09:00"
    val closeTime: String                    // Format: "HH:mm", e.g., "18:00"
)

enum class POICategory {
    MUSEUM, PARK, RESTAURANT, SHOPPING, ENTERTAINMENT, 
    HISTORICAL, NATURE, BEACH, SPORTS, CULTURAL, 
    NIGHTLIFE, CAFE, LANDMARK, SCENIC_SPOT
}

enum class Environment {
    INDOOR,   // Fully indoors
    OUTDOOR,  // Fully outdoors
    MIXED     // Partially indoors/outdoors
}
```

### TravelDuration Schema

```kotlin
data class TravelDuration(
    val fromPoiId: String,           // Source POI ID, e.g., "poi_001"
    val toPoiId: String,             // Destination POI ID, e.g., "poi_002"
    val walkingMinutes: Int,         // Walking time in minutes, e.g., 15
    val drivingMinutes: Int          // Driving time in minutes, e.g., 5
)
```

### ContextPayload Schema

```kotlin
data class ContextPayload(
    val currentLocation: Coordinates,
    val currentTime: String,                      // ISO 8601 format
    val weatherStatus: WeatherStatus,             // Historical/planned weather
    val currentWeather: WeatherStatus,           // Real-time weather
    val itineraryProgress: ItineraryProgress,
    val recentStepCount: Int                     // Steps in last hour
)

data class ItineraryProgress(
    val completedPOIs: List<String>,              // List of completed POI IDs
    val delayMinutes: Int                         // Delay in minutes (positive = behind)
)

enum class WeatherStatus {
    SUNNY, CLOUDY, RAINY, STORMY, SNOWY
}
```

### UserProfile Schema

```kotlin
data class UserProfile(
    val interests: List<POICategory>,             // User's interest categories
    val transportMode: TransportMode              // Primary transport mode (WALKING or DRIVING only)
)

enum class TransportMode {
    WALKING, DRIVING
}
```

## Error Handling

If required data is missing or invalid, the engine will return an error response:

```kotlin
data class ErrorResponse(
    val success: false,
    val errorCode: String,
    val errorMessage: String
)
```

Common error codes:
- `MISSING_REQUIRED_FIELD`: Required field is null or empty
- `INVALID_DATE_RANGE`: End date before start date
- `INVALID_COORDINATES`: Coordinates out of valid range
- `POI_NOT_FOUND`: Referenced POI ID does not exist
- `INVALID_TIME_FORMAT`: Time string not in expected format
