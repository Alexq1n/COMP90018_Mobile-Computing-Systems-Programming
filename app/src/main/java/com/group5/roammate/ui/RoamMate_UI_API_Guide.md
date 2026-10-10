# RoamMate UI API Guide

A quick reference for connecting RoamMate UI screens with backend, sensor, weather, database and attraction data.

This guide is for **UI integration contracts**. It does not replace the real backend API guide. It tells teammates what each UI screen expects, which function name to search in `MainActivity.kt`, who owns the data, and what input/output shape is easiest for the UI.

## 1. Integration Rules

- Keep screen composables simple: pass data in, pass click events out.
- Use stable `id` fields for places, trips and stops. UI text can change, but ids should not.
- Use `suspend` functions for Firebase/backend requests.
- Use `Flow` / `StateFlow` for live sensor or loading updates.
- Return an empty list when there are no results. Do not return fake UI text from backend.
- `distanceMeters` may be used for sorting, but the current UI does **not** display exact distance.
- Missing optional data should be nullable, for example `photoUrl: String?` or `todayHours: OpeningHoursUiData?`.
- Dates should use ISO format: `yyyy-MM-dd`. Time should use 24-hour format: `HH:mm`.

## 2. Ownership Table

| UI function label | API owner | Data / input provider | Consumer | Used by screen | Status |
|---|---|---|---|---|---|
| `uiLoginWithFirebase` | Yuxiang | Firebase Auth | UI | Login | Mock in `MainActivity` |
| `uiCreateAccountWithFirebase` | Yuxiang | Firebase Auth | UI | Create account | Mock in `MainActivity` |
| `uiLoadUserProfile` | Yuxiang | Firebase user profile | UI | Home, Profile, Edit profile | Mock in `MainActivity` |
| `uiSaveUserProfile` | Yuxiang | Firebase user profile | UI | Edit profile | Mock in `MainActivity` |
| `uiSaveProfileInterests` | Yuxiang | Firebase user preference | UI | Interests from Profile | Mock in `MainActivity` |
| `uiSaveTripOnlyInterests` | Zewen | current trip request | UI | Interests from Plan | Mock in `MainActivity` |
| `uiLoadSavedTrips` | Yuxiang | Zewen trip summary shape | UI | Saved trips | Mock in `MainActivity` |
| `uiGetCurrentLocation` | Alex | sensor GPS | UI | Explore, Add stop, Trip | Sensor connected |
| `uiObserveShakeEvents` | Alex | ShakeDetector event | UI | Explore, Add stop, Trip | Sensor connected, UI action mapped |
| `uiSearchPlacesByName` | Alex | Leyan attraction names | UI | Add stop | Mock in `MainActivity` |
| `uiExploreNearbyPlaces` | Alex | Leyan attraction list + GPS | UI | Explore | Mock in `MainActivity` |
| `uiLoadAttractionDetail` | Leyan | Yuxiang database if needed | UI | Attraction detail | Mock in `MainActivity` |
| `uiGenerateItinerary` | Zewen | plan request | UI | Plan My Trip | Mock in `MainActivity` |
| `uiUpdateItineraryAfterEdit` | Zewen | Yuxiang save after recalculation | UI | Edit itinerary, Add stop | Mock in `MainActivity` |
| `uiAdjustItineraryForWeather` | Zewen | LeYan weather forecast | UI | Adjust itinerary | Mock in `MainActivity` |
| `uiLoadWeatherSummary` | LeYan | weather API | UI | Home, Trip, Adjust itinerary | Partly pet weather now |
| `uiOpenExternalMap` | Yufei | Android map intent | UI | Home, Trip, Attraction detail | Done |
| `uiPetStatusCard` | Xiajie | pet module | UI | Home, Pet | In progress |

## 3. Shared UI Data Types

These are suggested integration shapes. They can be implemented as Kotlin data classes or converted from Firebase/backend models.

```kotlin
data class UserProfileUiData(
    val userId: String,
    val displayName: String,
    val defaultInterestIds: List<String>,
)

data class PlaceBriefUiData(
    val id: String,
    val name: String,
    val category: String,
    val environmentType: String, // "Indoor" or "Outdoor"
    val latitude: Double?,
    val longitude: Double?,
    val distanceMeters: Int? = null, // sorting only, not displayed
)

data class AttractionDetailUiData(
    val id: String,
    val name: String,
    val environmentType: String,
    val photoUrl: String?,
    val todayHours: OpeningHoursUiData?,
    val weeklyHours: List<OpeningHoursUiData>,
    val websiteUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
)

data class OpeningHoursUiData(
    val dayLabel: String,
    val timeRange: String,
)

data class ItineraryRequestUiData(
    val destinationCity: String,
    val startDate: String, // yyyy-MM-dd
    val endDate: String,   // yyyy-MM-dd
    val startTime: String, // HH:mm
    val interestIds: List<String>,
    val transportMode: String, // Walk, Transit, Drive
    val requiredPlaceIds: List<String>,
)

data class TripStopUiData(
    val id: String,
    val placeId: String?,
    val title: String,
    val time: String, // HH:mm
    val status: String, // Done, Current, Upcoming
    val hiddenTag: String? = null,
)

data class TripSummaryUiData(
    val title: String, // example: "3 days in Melbourne"
    val description: String, // one or two sentences
)

data class ItineraryUiData(
    val tripId: String,
    val destinationCity: String,
    val summary: TripSummaryUiData,
    val stops: List<TripStopUiData>,
)
```

## 4. API Contracts by Function

### `uiSearchPlacesByName`

Used by: `AddStopScreen`

Purpose: user types a place name and taps the confirm/search button. The search should rank by **name match first**.

Input:

```kotlin
data class PlaceNameSearchRequest(
    val query: String,
    val currentCity: String?,
    val userLatitude: Double?,
    val userLongitude: Double?,
    val limit: Int,
)
```

Output:

```kotlin
List<PlaceBriefUiData>
```

Notes:

- UI does not search on every typed character.
- UI calls this only after the user confirms search.
- `distanceMeters` can be returned, but UI does not display it.
- At minimum the UI needs `id`, `name`, and `environmentType`.

Example:

```kotlin
PlaceBriefUiData(
    id = "melbourne_museum",
    name = "Melbourne Museum",
    category = "Museum",
    environmentType = "Indoor",
    latitude = -37.8033,
    longitude = 144.9717,
    distanceMeters = 820,
)
```

### `uiExploreNearbyPlaces`

Used by: `ExploreScreen`

Purpose: show nearby places based on GPS and selected category. This is **not** name search.

Input:

```kotlin
data class NearbyPlacesRequest(
    val userLatitude: Double,
    val userLongitude: Double,
    val categoryFilter: String, // Indoor, Outdoor, Cafes, Food
    val limit: Int,
)
```

Output:

```kotlin
List<PlaceBriefUiData>
```

Notes:

- Ranking should be location/category relevance first.
- UI displays `name` and `environmentType` only.
- Distance can be used internally for sorting.

### `uiLoadAttractionDetail`

Used by: `AttractionDetailScreen`

Purpose: load full attraction content after clicking a place from Explore, Trip or Add stop.

Input:

```text
attractionId: String
```

Output:

```kotlin
AttractionDetailUiData
```

Required fields:

| Name | Type | Required | Provider |
|---|---|---|---|
| `id` | `String` | Yes | Leyan / database |
| `name` | `String` | Yes | Leyan |
| `environmentType` | `String` | Yes | Leyan |
| `photoUrl` | `String?` | No | Leyan / database |
| `todayHours` | `OpeningHoursUiData?` | No | Leyan |
| `weeklyHours` | `List<OpeningHoursUiData>` | No | Leyan |
| `websiteUrl` | `String?` | No | Leyan |
| `latitude` / `longitude` | `Double?` | No | Leyan / database |

### `uiGenerateItinerary`

Used by: `PlanMyTripScreen`

Purpose: create a new itinerary from user choices.

Input:

```kotlin
ItineraryRequestUiData
```

Output:

```kotlin
ItineraryUiData
```

Notes:

- Home and Trip should use the same returned itinerary data.
- The UI needs a short `summary` for the Trip page.
- If weather is beyond one week, weather-related fields can be unavailable.

### `uiUpdateItineraryAfterEdit`

Used by: `EditItineraryScreen`, `AddStopScreen`

Purpose: after removing, adding or reordering stops, backend recalculates times and order.

Input:

```kotlin
data class ItineraryEditRequest(
    val tripId: String,
    val orderedStopIds: List<String>,
    val removedStopIds: List<String>,
    val addedPlaceIds: List<String>,
)
```

Output:

```kotlin
ItineraryUiData
```

Notes:

- UI can drag and reorder stops.
- Backend should recalculate time after save.
- Yuxiang can save the final itinerary after Zewen returns it.

### `uiAdjustItineraryForWeather`

Used by: `AdjustItineraryScreen`

Purpose: weather change produces an alternative plan.

Input:

```kotlin
data class WeatherAdjustRequest(
    val tripId: String,
    val reason: String, // example: "rain"
    val currentStops: List<TripStopUiData>,
)
```

Output:

```kotlin
data class AdjustedItineraryUiData(
    val optionId: String,
    val reasonLabel: String,
    val reasonDescription: String,
    val stops: List<TripStopUiData>,
    val removedStops: List<String>,
)
```

Notes:

- Current UI supports two candidate plans.
- After the second candidate, the regenerate button becomes a back/previous action.

### `uiLoadSavedTrips`

Used by: `SavedTripsScreen`

Input:

```text
userId: String
```

Output:

```kotlin
data class SavedTripSummaryUiData(
    val tripId: String,
    val destination: String,
    val durationText: String,
    val dateText: String,
)
```

Use `emptyList()` if there are no saved trips.

## 5. Sensor Use in UI

See `sensor/RoamMate_Sensor_API_Guide.md` for the full sensor API.

UI currently uses:

| Sensor function | UI function label | Screen behavior |
|---|---|---|
| `locationFlow` / `getCurrentLocation()` | `uiGetCurrentLocation` | Explore area, Add stop recommendations, Trip progress |
| `shakeEvents` | `uiObserveShakeEvents` | Explore refresh, Add stop refresh, Trip progress refresh |

## 6. Loading and Error States

Use these states in `MainActivity` or a future ViewModel:

```kotlin
var isGeneratingItinerary: Boolean
var isSearchingPlaces: Boolean
var isLoadingAttractionDetail: Boolean
var errorMessage: String?
```

Recommended behavior:

- search request running: show loading inside Add stop search area.
- itinerary request running: disable Generate itinerary button and show loading.
- detail request failed: show fallback content and a toast/snackbar.
- no search result: show an empty state, not a crash.

## 7. Search Labels in `MainActivity.kt`

Team members can search these exact labels:

- `function: uiSearchPlacesByName`
- `function: uiExploreNearbyPlaces`
- `function: uiLoadAttractionDetail`
- `function: uiGenerateItinerary`
- `function: uiUpdateItineraryAfterEdit`
- `function: uiAdjustItineraryForWeather`
- `function: uiLoadUserProfile`
- `function: uiSaveUserProfile`
- `function: uiLoadSavedTrips`
- `function: uiObserveShakeEvents`
- `function: uiGetCurrentLocation`
