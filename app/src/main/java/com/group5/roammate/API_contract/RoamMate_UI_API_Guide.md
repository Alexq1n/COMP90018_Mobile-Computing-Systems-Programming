# RoamMate UI API Guide

A quick reference for connecting RoamMate UI screens with backend, sensor, weather, database and attraction data.

This guide is for **UI integration contracts**. It does not replace the real backend API guide. It tells teammates what each UI screen expects, which function name to search in `MainActivity.kt`, who owns the data, and what input/output shape is easiest for the UI.

## 1. Integration Rules

- Keep screen composables simple: pass data in, pass click events out.
- Use stable `id` fields for places, trips and stops. UI text can change, but ids should not.
- Use `suspend` functions for Firebase/backend requests.
- Use `Flow` / `StateFlow` for live sensor or loading updates.
- Return an empty list when there are no results. Do not return fake UI text from backend.
- Exact `distanceMeters` is no longer displayed in the UI. It can still be used internally for ranking.
- Missing optional data should be nullable, for example `photoUrl: String?` or `website: String?`.
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
| `uiSearchPlacesByName` | Alex | Leyan POI source, Yuxiang database if needed | UI | Add stop | Mock in `MainActivity` |
| `uiExploreNearbyPlaces` | Alex | Leyan POI source, Alex GPS | UI | Explore | Mock in `MainActivity` |
| `uiLoadAttractionDetail` | Leyan | POI detail, Yuxiang database if needed | UI | Attraction detail | Mock in `MainActivity` |
| `uiGenerateItinerary` | Zewen | plan request | UI | Plan My Trip | Mock in `MainActivity` |
| `uiUpdateItineraryAfterEdit` | Zewen | Yuxiang save after recalculation | UI | Edit itinerary, Add stop | Mock in `MainActivity` |
| `uiAdjustItineraryForWeather` | Zewen | LeYan weather forecast | UI | Adjust itinerary | Mock in `MainActivity` |
| `uiLoadWeatherSummary` | LeYan | weather API | UI | Home, Trip, Adjust itinerary | Partly pet weather now |
| `uiOpenExternalMap` | Yufei | Android map intent | UI | Home, Trip, Attraction detail | Done |
| `uiPetStatusCard` | Xiajie | pet module | UI | Home, Pet | In progress |

## 3. Latest POI Source Model

Leyan's latest attraction source uses `POI`. Search and detail functions should return this model directly, or return a model that can be mapped from it without losing fields.

```kotlin
@Serializable
data class POI(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    val website: String? = null,
    val phone: String? = null,
    val baseScore: Double? = null,
    val category: POICategory,
    val environment: Environment,
    val coordinates: Coordinates,
    val recommendedVisitDuration: Int,
    val operatingHours: OperatingHours,
    val isFiller: Boolean,
    val city: String,
    val address: String?,
    val description: String?,
)

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class OperatingHours(
    val openTime: String,
    val closeTime: String,
)

@Serializable
enum class POICategory {
    MUSEUM, PARK, RESTAURANT, SHOPPING, ENTERTAINMENT,
    HISTORICAL, NATURE, BEACH, SPORTS, CULTURAL,
    NIGHTLIFE, CAFE, LANDMARK, SCENIC_SPOT
}

@Serializable
enum class Environment {
    INDOOR,
    OUTDOOR,
    MIXED
}
```

Important notes:

- `category` is `POICategory`, not a display `String`.
- `environment` is `Environment`, not a display `String`.
- UI can convert enums to display text, for example `MUSEUM -> Museum`, `INDOOR -> Indoor`.
- `id` is the key used to open `AttractionDetailScreen` and add places to a trip.
- `photoUrl`, `website`, `phone`, `address`, and `description` can be null.

## 4. Shared UI Data Types

These are suggested integration shapes for UI-only screens. Backend/search can return `POI` directly, then UI can map it into smaller display models.

```kotlin
data class UserProfileUiData(
    val userId: String,
    val displayName: String,
    val defaultInterestIds: List<String>,
)

data class AttractionDetailUiData(
    val id: String,
    val name: String,
    val category: POICategory,
    val environment: Environment,
    val photoUrl: String?,
    val websiteUrl: String?,
    val phone: String?,
    val address: String?,
    val description: String?,
    val coordinates: Coordinates,
    val operatingHours: OperatingHours,
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

## 5. API Contracts by Function

### `uiSearchPlacesByName`

Used by: `AddStopScreen`

Purpose: user types a place name and taps the confirm/search button. The search should rank by **name match first**.

Input:

```kotlin
data class PoiNameSearchRequest(
    val query: String,
    val currentCity: String?,
    val userLatitude: Double?,
    val userLongitude: Double?,
    val limit: Int,
)
```

Output:

```kotlin
List<POI>
```

Notes:

- UI does not search on every typed character.
- UI calls this only after the user taps the confirm/search button.
- The search function should search the POI source or database. UI should not pass the full POI list.
- If the search module cannot access the repository directly, an internal helper can accept `allPois: List<POI>`.
- Ranking should prioritize name match first.
- UI displays `name` and `environment` only in Add stop.
- UI uses `id` to add the selected POI to the trip and to open detail later.

Example:

```kotlin
POI(
    id = "51002a0b15d21e624059f9f884ecbce742c0f00103f9012693de300300000092031153686f7420546f776572204d757365756d",
    name = "Shot Tower Museum",
    photoUrl = "https://staticmap.openstreetmap.de/staticmap.php?center=-37.810453,144.9631448&zoom=16&size=800x500&markers=-37.810453,144.9631448,red-pushpin",
    website = null,
    phone = null,
    baseScore = 8.1,
    category = POICategory.MUSEUM,
    environment = Environment.INDOOR,
    coordinates = Coordinates(latitude = -37.810453, longitude = 144.9631448),
    recommendedVisitDuration = 120,
    operatingHours = OperatingHours(openTime = "09:00", closeTime = "18:00"),
    isFiller = false,
    city = "Melbourne",
    address = "McIntyre Alley, Melbourne Victoria 3000, Australia",
    description = "Small museum covering the shot tower structure and Melbourne history.",
)
```

### `uiExploreNearbyPlaces`

Used by: `ExploreScreen`

Purpose: show recommended nearby places based on GPS and selected category. This is **not** name search.

Input:

```kotlin
data class NearbyPoiRequest(
    val currentCity: String?,
    val userLatitude: Double?,
    val userLongitude: Double?,
    val categoryFilter: POICategory?,
    val environmentFilter: Environment?,
    val limit: Int,
)
```

Output:

```kotlin
List<POI>
```

Notes:

- Ranking should prioritize nearby/category relevance.
- Distance can be calculated internally, but UI does not display exact distance.
- UI displays `name` and `environment` only.
- The map was removed from Explore; the page only needs the nearby list.

### `uiLoadAttractionDetail`

Used by: `AttractionDetailScreen`

Purpose: load full attraction content after clicking a place from Explore, Trip or Add stop.

Input:

```kotlin
val attractionId: String
```

Output:

```kotlin
POI
```

Required fields:

| Name | Type | Required | Provider |
|---|---|---|---|
| `id` | `String` | Yes | Leyan / database |
| `name` | `String` | Yes | Leyan |
| `category` | `POICategory` | Yes | Leyan |
| `environment` | `Environment` | Yes | Leyan |
| `coordinates` | `Coordinates` | Yes | Leyan |
| `operatingHours` | `OperatingHours` | Yes | Leyan if available, fallback allowed |
| `photoUrl` | `String?` | No | Leyan / database |
| `website` | `String?` | No | Leyan / database |
| `phone` | `String?` | No | Leyan / database |
| `address` | `String?` | No | Leyan / database |
| `description` | `String?` | No | Leyan / database |

Notes:

- Attraction detail no longer displays exact distance.
- If opening hours are missing, UI should show unavailable content instead of crashing.

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


## 6. Search Edge Cases

Recommended behavior for Alex/search integration:

| Case | Expected output |
|---|---|
| `query` is blank | Return popular/current-city places or `emptyList()`; do not return null. |
| `query` is too short | Return a small popular list, or wait until confirm button is tapped. |
| `query` is very long | Trim input, cap length around 50 characters. |
| `currentCity` is null | Search all supported Australia POIs, or default to Melbourne during demo. |
| GPS is null | Skip distance ranking; use name/category/popularity. |
| `limit <= 0` | Return `emptyList()`. |
| `limit > 20` | Cap to 20 results. |
| No result | Return `emptyList()` so UI can show "Not found". |
| Missing optional fields | Keep nullable values as null. |
| Missing location | The place can appear in name search, but should not be distance-ranked. |

## 7. Sensor Use in UI

See `sensor/RoamMate_Sensor_API_Guide.md` for the full sensor API.

UI currently uses:

| Sensor function | UI function label | Screen behavior |
|---|---|---|
| `locationFlow` / `getCurrentLocation()` | `uiGetCurrentLocation` | Explore area, Add stop recommendations, Trip progress |
| `shakeEvents` | `uiObserveShakeEvents` | Explore refresh, Add stop refresh, Trip progress refresh |

## 8. Loading and Error States

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

## 9. Search Labels in `MainActivity.kt`

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
