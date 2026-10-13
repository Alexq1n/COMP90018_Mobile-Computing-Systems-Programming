# RoamMate UI API Guide

This guide explains the UI-side integration contracts used by `MainActivity.kt`.

Use this guide when connecting backend, Firebase, sensor, weather, search, pet, and attraction data into the UI.

## Basic Rules

- Search function names directly in `MainActivity.kt`.
- Each integration function has one main owner.
- UI can use `name` as the place key for the current demo.
- If backend/database needs a stable `id`, keep it inside the backend/database layer.
- Add Stop does not display distance for now.
- Explore and Attraction Detail also hide exact distance from the UI.
- Distance/GPS can still be used internally for ranking or sorting.
- Empty result should return an empty list.
- Missing optional data should use `null`.
- Date format: `yyyy-MM-dd`.
- Time format: `HH:mm`.

## Ownership Table

| Function | Owner | Other input provider | Used by | Status |
|---|---|---|---|---|
| `uiLoginWithFirebase` | Yuxiang | - | Login | Mock in `MainActivity` |
| `uiCreateAccountWithFirebase` | Yuxiang | - | Create account | Mock in `MainActivity` |
| `uiLoadUserProfile` | Yuxiang | - | Home, Profile, Edit profile | Mock in `MainActivity` |
| `uiSaveUserProfile` | Yuxiang | - | Edit profile | Mock in `MainActivity` |
| `uiSaveProfileInterests` | Yuxiang | - | Interests from Profile | Mock in `MainActivity` |
| `uiSaveTripOnlyInterests` | Zewen | - | Interests from Plan My Trip | Helper in `MainActivity` |
| `uiLoadSavedTrips` | Yuxiang | Zewen trip summary shape | Saved trips | Mock in `MainActivity` |
| `uiGetCurrentLocation` | Alex | Sensor GPS | Home, Explore, Add stop, Trip | Sensor connected |
| `uiObserveShakeEvents` | Alex | ShakeDetector | Explore, Add stop, Trip | Sensor connected |
| `uiSearchPlacesByName` | Alex | Leyan POI data | Add stop | Mock/search hook |
| `uiExploreNearbyPlaces` | Alex | Leyan POI data + GPS | Explore | Mock/search hook |
| `uiLoadAttractionDetail` | Leyan | Yuxiang database if needed | Attraction detail | Mock in `MainActivity` |
| `uiGenerateItinerary` | Zewen | Plan request | Plan My Trip | Mock in `MainActivity` |
| `uiLoadTripSummary` | Zewen | Itinerary result | Trip | Mock in `MainActivity` |
| `uiUpdateItineraryAfterEdit` | Zewen | Yuxiang save after update | Edit itinerary | Mock in `MainActivity` |
| `uiAdjustItineraryForWeather` | Zewen | LeYan weather | Adjust itinerary | Mock in `MainActivity` |
| `uiLoadWeatherSummary` | LeYan | Weather API | Home, Trip | Mock in `MainActivity` |
| `uiIsWeatherAvailableForDate` | LeYan | Weather API | Plan My Trip | Mock in `MainActivity` |
| `uiPetStatusCard` | Xiajie | Pet module | Home, Pet | In progress |
| `uiLoadingState` | Yufei | Any async request | Shared loading UI | Done |
| `uiOpenExternalMap` | Yufei | Android map intent | Home, Trip, Detail | Done |

## UI Data Models

These models match the current UI files.

### Add Stop

```kotlin
data class AddStopPlace(
    val name: String,
    val environmentType: String,
)
```

UI shows:

| Field | UI display |
|---|---|
| `name` | Place title |
| `environmentType` | Indoor / Outdoor / Mixed |

Notes:

- No `distanceText` in Add Stop for now.
- Alex can still use GPS/distance internally for ranking.

### Explore

```kotlin
enum class ExplorePlaceCategory {
    Indoor,
    Outdoor,
    Cafes,
    Food,
}

data class ExplorePlace(
    val name: String,
    val distanceText: String,
    val category: ExplorePlaceCategory,
    val environmentLabel: String,
    val tagText: String,
    val mapX: Float,
    val mapY: Float,
)
```

UI shows:

| Field | UI display |
|---|---|
| `name` | Place title |
| `environmentLabel` | Indoor / Outdoor / Mixed |

Notes:

- `distanceText`, `tagText`, `mapX`, and `mapY` may stay for internal/demo use.
- Exact distance is hidden from the Explore UI.
- Map is not required for final UI unless the team has time.

### Attraction Detail

```kotlin
data class AttractionDetail(
    val name: String,
    val distanceText: String,
    val environmentLabel: String,
    val weatherTag: String?,
    val todayHours: AttractionOpeningHours?,
    val weeklyHours: List<AttractionOpeningHours>,
    val websiteUrl: String?,
    val photoUrl: String?,
    val imageSymbol: String,
)

data class AttractionOpeningHours(
    val dayLabel: String,
    val timeRange: String,
)
```

UI shows:

| Field | UI display |
|---|---|
| `name` | Detail title |
| `environmentLabel` | Indoor / Outdoor / Mixed |
| `weatherTag` | Weather suitability chip |
| `todayHours` | Open today row |
| `weeklyHours` | Expanded opening hours |
| `websiteUrl` | Official website action |
| `photoUrl` | Attraction photo if available |

Notes:

- Exact distance is hidden from the detail UI.
- If `todayHours == null`, UI should show unavailable.

### Plan My Trip

```kotlin
data class PlanMyTripRequest(
    val destination: String,
    val date: String,
    val startTime: String,
    val interests: List<String>,
    val transportMode: String,
    val specificPlaces: List<String>,
)
```

UI sends this when the user taps `Generate itinerary`.

### Trip

```kotlin
data class TripTimelineStop(
    val time: String,
    val title: String,
    val status: TripStopStatus,
    val hiddenTag: String? = null,
)

enum class TripStopStatus {
    Done,
    Current,
    Upcoming,
}

data class TripSummary(
    val title: String,
    val description: String,
)

data class TripWeatherSummary(
    val temperature: String,
    val condition: String,
)
```

### Saved Trips

```kotlin
data class SavedTrip(
    val destination: String,
    val durationText: String,
    val dateText: String,
)
```

### Adjust Itinerary

```kotlin
data class AdjustItineraryPlan(
    val reasonLabel: String,
    val reasonDescription: String,
    val stops: List<AdjustItineraryStop>,
    val removedStop: AdjustRemovedStop?,
)
```

## Function Contracts

### `uiSearchPlacesByName`

Owner: Alex

Used by: `AddStopScreen`

Purpose: search by place name after the user taps the confirm/search button.

Input:

```kotlin
query: String
currentCity: String?
userLatitude: Double?
userLongitude: Double?
limit: Int
```

Output:

```kotlin
List<AddStopPlace>
```

Example:

```kotlin
AddStopPlace(
    name = "Melbourne Museum",
    environmentType = "Indoor",
)
```

Notes:

- Ranking should prioritize name match first.
- Do not require UI to pass the full POI list.
- Distance can be used internally, but do not return it to Add Stop UI for now.

### `uiExploreNearbyPlaces`

Owner: Alex

Used by: `ExploreScreen`

Purpose: load nearby places for the selected category.

Input:

```kotlin
category: ExplorePlaceCategory
userLatitude: Double?
userLongitude: Double?
limit: Int
```

Output:

```kotlin
List<ExplorePlace>
```

Notes:

- Ranking should prioritize current location and selected category.
- UI displays name and environment only.
- Hidden treasure should not appear as a separate Explore item.

### `uiLoadAttractionDetail`

Owner: Leyan

Used by: `AttractionDetailScreen`

Purpose: load full attraction content after clicking a place.

Input:

```kotlin
attractionIdOrName: String
```

Output:

```kotlin
AttractionDetail
```

Notes:

- Current demo can use place name as input.
- If database later uses id, the database layer can map name to id internally.

### `uiGenerateItinerary`

Owner: Zewen

Used by: `PlanMyTripScreen`

Input:

```kotlin
request: PlanMyTripRequest
```

Output:

```kotlin
List<TripTimelineStop>
```

Notes:

- `specificPlaces` are must-visit places added from Add Stop.
- `interests` are current-trip interests only.

### `uiUpdateItineraryAfterEdit`

Owner: Zewen

Used by: `EditItineraryScreen`

Input:

```kotlin
editedStops: List<TripTimelineStop>
```

Output:

```kotlin
List<TripTimelineStop>
```

Notes:

- Used after delete/add/reorder.
- Backend can recalculate time/order.

### `uiAdjustItineraryForWeather`

Owner: Zewen

Used by: `AdjustItineraryScreen`

Input:

```kotlin
reason: String
```

Output:

```kotlin
List<AdjustItineraryPlan>
```

Notes:

- Weather reason comes from LeYan weather data.
- Current design supports two plans only.

### `uiGetCurrentLocation`

Owner: Alex

Used by: `HomeScreen`, `ExploreScreen`, `AddStopScreen`, `TripScreen`

Output:

```kotlin
SensorLocation(
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float?,
    areaName: String?,
    isSample: Boolean,
)
```

Notes:

- `areaName` can be shown near the top of Explore.
- GPS can be used for nearby ranking, leave-now logic, and trip progress.

### `uiObserveShakeEvents`

Owner: Alex

Used by: `ExploreScreen`, `AddStopScreen`, `TripScreen`

Output:

```kotlin
onShake: () -> Unit
```

UI behavior:

| Screen | Shake action |
|---|---|
| Explore | refresh nearby places |
| Add Stop | refresh nearby suggestions |
| Trip | update trip progress |

### `uiLoadSavedTrips`

Owner: Yuxiang

Used by: `SavedTripsScreen`

Output:

```kotlin
List<SavedTrip>
```

### `uiLoginWithFirebase`

Owner: Yuxiang

Input:

```kotlin
email: String
password: String
```

Output:

```kotlin
Boolean
```

### `uiCreateAccountWithFirebase`

Owner: Yuxiang

Input:

```kotlin
fullName: String
email: String
password: String
```

Output:

```kotlin
Boolean
```

### `uiLoadUserProfile`

Owner: Yuxiang

Output:

```kotlin
String // displayName
```

### `uiSaveUserProfile`

Owner: Yuxiang

Input:

```kotlin
updatedName: String
```

Output:

```kotlin
String // saved displayName
```

### `uiSaveProfileInterests`

Owner: Yuxiang

Used by: Interests page from Profile.

Input/Output:

```kotlin
List<String>
```

Notes:

- Permanent user preference.
- Save to Firebase/user profile.

### `uiSaveTripOnlyInterests`

Owner: Zewen

Used by: Interests page from Plan My Trip.

Input/Output:

```kotlin
List<String>
```

Notes:

- Temporary current-trip interest.
- Do not save as permanent profile preference.

## Cross-Team Notes

### Alex

- `uiSearchPlacesByName`: Add Stop, name-first search.
- `uiExploreNearbyPlaces`: Explore, nearby/category-first ranking.
- `uiGetCurrentLocation`: GPS and area name.
- `uiObserveShakeEvents`: shake trigger.

### Leyan

- POI raw data: name, environment, category, opening hours, website, photo.
- `uiLoadAttractionDetail`: full detail data.
- Weather data: one-week availability and weather summary.

### Zewen

- `uiGenerateItinerary`: create itinerary from `PlanMyTripRequest`.
- `uiLoadTripSummary`: short summary for Trip screen.
- `uiUpdateItineraryAfterEdit`: recalculate after add/delete/reorder.
- `uiAdjustItineraryForWeather`: regenerate plan for weather changes.

### Yuxiang

- Firebase auth.
- User profile.
- Permanent interests.
- Saved trips.
- Final saved itinerary if needed.

## Related Guides

- Sensor guide: `app/src/main/java/com/group5/roammate/API_contract/RoamMate_Sensor_API_Guide.md`
