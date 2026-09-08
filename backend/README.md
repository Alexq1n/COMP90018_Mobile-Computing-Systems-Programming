# RoamMate Logic Engine

Context-Aware Trip Planning Logic Module - Kotlin Android Library

## Overview

This is a pure Kotlin business logic module that implements intelligent, context-aware trip planning algorithms. It generates dynamic multi-day travel itineraries and adapts them in real-time based on contextual changes (user delays, weather shifts).

## Key Features

- **Multi-Day Geographic Clustering**: Pre-filters POIs into geographically grouped daily candidate pools
- **Dynamic EV-Based Routing**: Calculates optimal daily routes by maximizing Expected Value (EV)
- **Strategic Dynamic Adjustment**: Provides two distinct choices when delays occur:
  - **Option A (Experience First)**: Maximizes total EV
  - **Option B (Efficiency First)**: Maximizes EV/Cost ratio
- **Dynamic Fillers (Hidden Gems)**: Injects micro-POIs seamlessly into existing routes
- **High-EV Rollover Mechanism**: Captures high-value POIs for next-day insertion

## Architecture

```
com.roammate.logic
├── models/              # Data models (POI, UserProfile, ContextPayload, etc.)
├── engine/              # Core algorithms
│   ├── EVCalculator.kt             # Expected Value calculation
│   ├── RoutingEngine.kt            # Multi-day itinerary generation
│   ├── DynamicAdjustmentEngine.kt  # Real-time adjustment
│   ├── GeoClusteringService.kt     # Geographic clustering
│   ├── TravelCostService.kt        # Travel time/cost calculation
│   └── FillerService.kt            # Hidden gems injection
├── interfaces/          # Public APIs
│   ├── ITripPlannerEngine.kt       # Main interface
│   └── IPoiRepository.kt           # POI data access
├── utils/               # Utilities
│   ├── GeoUtils.kt                 # Haversine distance calculation
│   └── MockPoiRepository.kt        # In-memory POI dataset
└── TripPlannerEngine.kt # Facade implementation
```

## Usage

### 1. Setup

Add the module to your Android project:

```kotlin
// In your app's build.gradle.kts
dependencies {
    implementation(project(":roammate-logic"))
}
```

### 2. Initialize the Engine

```kotlin
import com.roammate.logic.TripPlannerEngine
import com.roammate.logic.utils.MockPoiRepository

// Create POI repository (use real implementation in production)
val poiRepository = MockPoiRepository()

// Initialize engine
val tripPlanner = TripPlannerEngine(poiRepository)
```

### 3. Generate Initial Trip

```kotlin
import com.roammate.logic.models.*

// Define user profile
val userProfile = UserProfile(
    interests = listOf(POICategory.MUSEUM, POICategory.PARK, POICategory.LANDMARK),
    budgetPreference = BudgetLevel.MEDIUM,
    transportMode = TransportMode.WALKING
)

// Generate itinerary
val itinerary = tripPlanner.generateInitialTrip(
    tripId = "trip_001",
    destination = "Melbourne",
    startDate = "2026-09-15",
    numberOfDays = 3,
    userProfile = userProfile,
    startingLocation = Coordinates(-37.8136, 144.9631),
    dailyStartTime = "09:00",
    dailyEndTime = "18:00",
    weatherStatus = WeatherStatus.SUNNY,
    includeFillers = true
)

// Access daily plans
itinerary.days.forEach { day ->
    println("Day ${day.dayNumber}: ${day.pois.size} POIs")
    day.pois.forEach { poi ->
        println("  - ${poi.poi.name} (${poi.startTime} - ${poi.endTime})")
    }
}
```

### 4. Dynamic Adjustment

```kotlin
// When delay or weather change occurs
val contextPayload = ContextPayload(
    currentLocation = Coordinates(-37.8200, 144.9700),
    currentTime = "2026-09-15T13:30:00",
    weatherStatus = WeatherStatus.RAINY,
    itineraryProgress = ItineraryProgress(
        completedPOIs = listOf("poi_001", "poi_002"),
        delayMinutes = 120
    )
)

// Get adjustment options
val adjustment = tripPlanner.adjustTripState(
    currentItinerary = itinerary.days[0].pois,
    contextPayload = contextPayload,
    destination = "Melbourne",
    userProfile = userProfile
)

// Present options to user
println("Option A (Experience First):")
println("  Total EV: ${adjustment.optionA.totalExpectedValue}")
println("  Travel Time: ${adjustment.optionA.totalTravelTime} min")
println("  ${adjustment.optionA.description}")

println("\nOption B (Efficiency First):")
println("  Total EV: ${adjustment.optionB.totalExpectedValue}")
println("  Travel Time: ${adjustment.optionB.totalTravelTime} min")
println("  ${adjustment.optionB.description}")
```

## Expected Value (EV) Formula

```
Final_EV = base_score × interest_weight × budget_weight × weather_modifier
```

### Modifiers

- **Interest Weight**:
  - Match: 1.5×
  - No match: 1.0×

- **Budget Weight**:
  - Exact match: 1.2×
  - One level off: 1.0×
  - Two levels off: 0.7×

- **Weather Modifier**:
  - Rainy + Outdoor: 0.2×
  - Rainy + Indoor: 1.15×
  - Sunny + Outdoor: 1.1×
  - Default: 1.0×

## Testing

Run unit tests:

```bash
./gradlew test
```

Run specific test:

```bash
./gradlew test --tests "EVCalculatorTest"
./gradlew test --tests "TripPlannerEngineTest"
```

## Configuration

### Android Configuration

- **compileSdk**: 34
- **minSdk**: 26
- **targetSdk**: 34
- **Kotlin**: 1.9.24
- **AGP**: 8.2.2

### Dependencies

- `kotlinx.serialization`: JSON serialization
- `kotlinx.coroutines`: Async operations (future use)
- `junit`: Unit testing

## Data Models

### POI (Point of Interest)

```kotlin
data class POI(
    val id: String,
    val name: String,
    val baseScore: Double,              // 0-10 scale
    val category: POICategory,
    val budgetLevel: BudgetLevel,
    val environment: Environment,
    val coordinates: Coordinates,
    val recommendedVisitDuration: Int,  // minutes
    val operatingHours: OperatingHours,
    val isFiller: Boolean,
    val city: String,
    val description: String?
)
```

### UserProfile

```kotlin
data class UserProfile(
    val interests: List<POICategory>,
    val budgetPreference: BudgetLevel,
    val transportMode: TransportMode
)
```

### ContextPayload

```kotlin
data class ContextPayload(
    val currentLocation: Coordinates,
    val currentTime: String,            // ISO format
    val weatherStatus: WeatherStatus,
    val itineraryProgress: ItineraryProgress
)
```

## Algorithms

### Geographic Clustering

Uses radius-based clustering to group POIs into daily zones (default 5km radius). High-score POIs are prioritized as cluster centers.

### Routing Heuristic

Greedy algorithm that selects POIs with the highest EV/Cost ratio while respecting:
- Operating hours
- Time windows
- Transport mode constraints

### Dynamic Adjustment Strategies

- **Option A**: Sorts by pure EV descending
- **Option B**: Sorts by EV/Travel-Time ratio

Both respect remaining time budget and operating hours.

## Future Enhancements

- Integration with real map APIs (Google Maps, Mapbox)
- Machine learning for personalized EV weights
- Multi-objective optimization (cost, time, experience)
- Social recommendations (friend preferences)

