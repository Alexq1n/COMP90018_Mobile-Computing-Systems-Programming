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
| `uiLoginWithFirebase` | Yuxiang | - | Login | Firebase Auth connected |
| `uiCreateAccountWithFirebase` | Yuxiang | - | Create account | Firebase Auth + Firestore connected |
| `uiLoadUserProfile` | Yuxiang | - | Home, Profile, Edit profile | Firebase connected |
| `uiSaveUserProfile` | Yuxiang | - | Edit profile | Name stored in Firebase Authentication; no extra Firestore field |
| `uiSaveProfileInterests` | Yuxiang | - | Interests from Profile | Firestore connected |
| `uiSaveTripOnlyInterests` | Zewen | - | Interests from Plan My Trip | Helper in `MainActivity` |
| `uiLoadSavedTrips` | Yuxiang | Zewen trip summary shape | Saved trips | Firestore connected |
| `uiSaveGeneratedTrip` | Yufei | Yuxiang repository + Zewen itinerary | Generation result | Existing saveTrip API connected |
| `uiLoadSavedTrip` | Yufei | Yuxiang repository + Zewen itinerary | Saved trip card | Existing loadTrip API connected |
| `uiGetCurrentLocation` | Alex | Sensor GPS | Home, Explore, Add stop, Trip | Sensor connected |
| `uiObserveShakeEvents` | Alex | ShakeDetector | Explore, Add stop, Trip | Sensor connected |
| `uiSearchPlacesByName` | Alex | Leyan POI data | Add stop | Mock/search hook |
| `uiExploreNearbyPlaces` | Alex | Leyan POI data + GPS | Explore | Mock/search hook |
| `uiLoadAttractionDetail` | Leyan | Enriched POI resource + SQLite | Attraction detail | Actual source fields connected; missing metadata stays null |
| `uiGenerateItinerary` | Zewen | Plan request + GPS | Plan My Trip | Planner and GPS connected; per-day weather input pending |
| `uiLoadTripSummary` | Zewen | Itinerary result + shared UI timeline | Trip | Connected to generated trip and saved UI edits |
| `uiUpdateItineraryAfterEdit` | Zewen | Yuxiang save after update | Reserved for edit integration | Route recalculation and cloud saving pending |
| `uiAdjustItineraryForWeather` | Zewen | LeYan weather + current generated context | Adjust itinerary | Today's day, request profile, clock, GPS and completed IDs connected; edited/restored context pending |
| `uiLoadWeatherSummary` | LeYan | Weather API | Home, Trip | Mock in `MainActivity` |
| `uiIsWeatherAvailableForDate` | LeYan | Weather API | Plan My Trip | Mock in `MainActivity` |
| `uiPetStatusCard` | Xiajie | Pet module | Home, Pet | In progress |
| `uiLoadingState` | Yufei | Any async request | Shared loading UI | Done |
| `uiOpenExternalMap` | Yufei | Android map intent | Trip, Detail | Trip uses its next unfinished stop |

## UI Data Models

These models match the current UI files.

### Add Stop

```kotlin
data class AddStopPlace(
    val name: String,
    val environmentType: String,
    val poiId: String? = null,
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
    val poiId: String? = null,
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
    val date: String? = null, // yyyy-MM-dd, inherited from the generated day
    val poiId: String? = null, // inherited from the source POI; never required from the UI user
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

data class TripDayPlan(
    val dayTitle: String,
    val dateText: String,
    val stops: List<TripTimelineStop>,
    val date: String? = null, // yyyy-MM-dd, used to select the day being edited
)
```

Trip displays the complete generated itinerary, including future days. Without a generated itinerary, it displays an empty state. Treasure stops remain in the timeline, but their tags are not rendered. Home's Today's trip selects only stops whose `date` matches today; it never falls back to the first day.

Each day has an edit action. `MainActivity` owns the shared timeline and a separate draft for the selected day. `EditItineraryScreen.onStopsChanged` updates the draft, including drag order; Add Stop updates the same draft. Save commits only that day, preserving completed stops and all other days. Back discards the draft. Trip, Home and navigation all read the committed timeline. These changes do not introduce a backend API; route/time recalculation and cloud persistence remain separate integration work.

Navigation prefers today's current stop, then today's next unfinished stop, then the earliest future day with an unfinished stop. Past and completed stops are excluded. If no destination remains, the navigation button is disabled.

### Saved Trips

```kotlin
data class SavedTrip(
    val destination: String,
    val durationText: String,
    val dateText: String,
    val tripId: String = "",
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

Reserved for backend integration after the frontend edit is saved.

Input:

```kotlin
editedStops: List<TripTimelineStop>
```

Output:

```kotlin
List<TripTimelineStop>
```

Notes:

- The current frontend commits edits with `applyTripDayEdits`; this placeholder is not called by the Save callback.
- Backend recalculation of visit times/routes and cloud persistence are not connected yet.

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

Callbacks:

```kotlin
onSuccess: (List<SavedTrip>) -> Unit
onError: (Exception) -> Unit
```

### `uiLoginWithFirebase`

Owner: Yuxiang

Input:

```kotlin
email: String
password: String
```

Callbacks:

```kotlin
onSuccess: (CloudUserSession) -> Unit
onError: (Exception) -> Unit
```

Only `onSuccess` may navigate to Home. Invalid credentials stay on Login.

### `uiCreateAccountWithFirebase`

Owner: Yuxiang

Input:

```kotlin
fullName: String // optional; an empty string is accepted
email: String
password: String
```

Callbacks:

```kotlin
onSuccess: (CloudUserSession) -> Unit
onError: (Exception) -> Unit
```

Firebase creates the account first. The UI then signs out and returns to Login so the user can authenticate with the new credentials.

The UI requires email, password and matching confirmation; confirmation is not sent to Firebase. The optional name is stored in Firebase Authentication. Registration writes only `email`, `createdAt` and `updatedAt` to Firestore, matching the database rules.

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

Callbacks:

```kotlin
onSuccess: (CloudUserSession) -> Unit
onError: (Exception) -> Unit
```

### `uiSaveProfileInterests`

Owner: Yuxiang

Used by: Interests page from Profile.

Input and callbacks:

```kotlin
interests: List<String>
onSuccess: (List<String>) -> Unit
onError: (Exception) -> Unit
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

## Remaining Integration Boundaries

- `uiUpdateItineraryAfterEdit` still needs a backend result that preserves the selected day, manual POI order and completed stops while recalculating visit/route times. Update the backend snapshot before saving edited plans to the cloud.
- `weatherForecasts` is fetched but the generator accepts a single weather status. Zewen and LeYan must connect date-specific weather and define the unavailable-forecast policy.
- GPS is available, but arrival/completion thresholds and deduplication are not defined. Shake-to-advance and the automatic fake-rain injection are disabled.
- Stored Itinerary JSON does not contain the original request profile or actual progress. Restored plans can be viewed and navigated; personalized replanning requires restoring that context. The UI does not invent a replacement profile.
- Display names are stored in Firebase Authentication. Firestore registration keeps its existing three-field payload. Auth names take precedence over legacy stored display names on sign-in.
- Generated plans use `saveTrip`; saved cards retain `tripId` and use `loadTrip`. Both Kotlin model packages share the existing JSON schema, covered by adapter round-trip tests. Navigating back cancels the UI request token, so late responses cannot replace a newer selection.

## Related Guides

- Sensor guide: `app/src/main/java/com/group5/roammate/API_contract/RoamMate_Sensor_API_Guide.md`
