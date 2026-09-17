# RoamMate Android UI

RoamMate is a travel planning Android app for Australia. This branch focuses on the front-end UI, screen navigation, and the temporary data flow needed before backend integration.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android Studio
- `compileSdk`: 37
- `targetSdk`: 37
- `minSdk`: 24
- AGP: 9.3.2
- Kotlin: 2.2.10
- Compose BOM: 2026.02.01

## Current UI Status

Most core UI screens are completed, including the virtual travel companion experience.

Completed:

- Login
- Create Account
- Home
- Plan My Trip
- Interests
- Trip
- Adjust Itinerary
- Edit Itinerary
- Add a Stop
- Explore
- Attraction Detail
- Profile
- Edit Profile
- Saved Trips
- Bottom Navigation
- Pet / Companions
- Direct on-page Buddy interactions
- Weather-driven wardrobe
- Pet camera with face tracking and composited gallery saving

Remaining:

- Loading states
- Error states
- Empty states for more backend failure cases
- Final backend integration and real data replacement

## Project Structure

```text
app/src/main/java/com/group5/roammate/
├── MainActivity.kt
├── pet/
│   ├── PetModels.kt
│   ├── PetStateEngine.kt
│   ├── PetPreferences.kt
│   └── OpenMeteoPetWeatherRepository.kt
├── ui/pet/
│   ├── PetAvatar.kt
│   ├── InteractivePetStage.kt
│   ├── CompanionsScreen.kt
│   └── PetCameraScreen.kt
├── ui/screens/
│   ├── LoginScreen.kt
│   ├── CreateAccountScreen.kt
│   ├── HomeScreen.kt
│   ├── PlanMyTripScreen.kt
│   ├── InterestsScreen.kt
│   ├── TripScreen.kt
│   ├── AdjustItineraryScreen.kt
│   ├── EditItineraryScreen.kt
│   ├── AddStopScreen.kt
│   ├── ExploreScreen.kt
│   ├── AttractionDetailScreen.kt
│   ├── ProfileScreen.kt
│   ├── EditProfileScreen.kt
│   ├── SavedTripsScreen.kt
│   └── RoamMateBottomNavigation.kt
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## Completed Screens

### Login

- Email input.
- Password input.
- Log in button.
- Create account navigation.
- Current behavior: mock login goes to Home after non-empty email and password.
- Future integration: Firebase authentication by Yuxiang.

### Create Account

- Full name input.
- Email input.
- Password input.
- Confirm password input.
- Basic front-end validation.
- Mascot image is shared with Login.
- Current behavior: mock registration goes to Home.
- Future integration: Firebase registration and user creation by Yuxiang.

### Home

- Greeting section.
- Leave-now reminder card.
- Smart suggestion card.
- Pet status card.
- Today's trip preview.
- Plan My Trip shortcut.
- Explore shortcut.
- Bottom navigation.
- Current behavior: reminders, suggestion, pet, and trip data are temporary mock data.

### Plan My Trip

- Destination picker for Australian cities.
- Date range picker.
- Start time picker.
- Interest chips.
- `+ More` opens Interests page for current trip interests.
- Transport selection: Walk, Transit, Drive.
- Search places to add opens Add a Stop.
- Generate itinerary button.
- Current behavior: creates a temporary `PlanMyTripRequest` and navigates to Trip.
- Future integration: send request to Zewen's itinerary generation logic.

### Interests

- Reusable interest selection page.
- Entry from Plan My Trip: temporary current-trip interests.
- Entry from Profile Travel preferences: long-term default preferences.
- Current behavior: both are stored in front-end state.
- Future integration: long-term preferences should be stored by Yuxiang; trip interests should be sent to Zewen for the current itinerary only.

### Trip

- Today's trip timeline.
- Weather summary.
- Stop status dots: done / current / upcoming.
- Optional hidden-gem tag.
- Edit itinerary button.
- Start navigation button.
- Bottom navigation.
- Current behavior: itinerary is temporary mock data in `MainActivity`.
- Future integration: itinerary from Zewen, progress/location from Sitao, weather from Yan.

### Adjust Itinerary

- Weather adjustment popup.
- Shows one regenerated plan from Zewen.
- Shows kept / moved / new badges.
- Shows removed outdoor/rain stop.
- Apply this plan button.
- Regenerate another button.
- Back to previous plan button after the second option.
- Keep current plan button.
- Current behavior: two mock regenerated plans.
- Future integration: generated alternatives from Zewen, weather trigger from Yan.

### Edit Itinerary

- Shows current itinerary stops.
- Remove stop button.
- Add a stop button.
- Save button.
- Current behavior: updates temporary front-end itinerary.
- Future integration: add/remove operations should trigger Zewen re-generation or re-ordering.

### Add a Stop

- Shared page for:
  - Plan My Trip search places to add.
  - Edit Itinerary add a stop.
- Search field.
- Confirm search button.
- Popular places in current city.
- Not found state.
- Add button on each result.
- Current behavior: filters local mock results.
- Future integration: search data from Alex/Yan, place details from Leyan/Yan, distance from Sitao, placement from Zewen.

### Explore

- Nearby place discovery page.
- Category filters: Indoor, Outdoor, Cafe, Food.
- Map-style visual area.
- Nearby place cards.
- Bottom navigation.
- Hidden treasure is not shown as a standalone category.
- Current behavior: local mock places and mock map points.
- Future integration: GPS from Alex/Sitao, nearby places from Yan/Leyan, distance calculation from Sitao.

### Attraction Detail

- Back button.
- Attraction image area.
- Attraction name.
- Distance.
- Indoor/outdoor tag.
- Weather suitability tag.
- Opening hours.
- Official website link.
- Add to trip button.
- Navigate button.
- Current behavior: local mock attraction detail.
- Future integration: attraction details/opening hours/website from Leyan/Yan, distance from Sitao, add-to-trip handling from Zewen.

### Profile

- User name.
- Fixed mascot avatar.
- Travel preferences entry.
- Saved trips entry.
- Edit profile button.
- Log out button.
- Bottom navigation.
- Current behavior: user profile is temporary state.
- Future integration: Firebase user profile from Yuxiang.

### Edit Profile

- Back button.
- Fixed mascot avatar.
- Name input.
- Save changes button.
- Current behavior: updates temporary front-end user name.
- Future integration: save user name to Yuxiang/Firebase.

### Saved Trips

- Back button.
- Saved trip cards.
- Empty state when data is empty.
- Create new trip / Plan a trip button.
- Current behavior: local mock saved trip history.
- Future integration: saved trips from Yuxiang/Firebase.

### Pet / Virtual Travel Companion

- One active Buddy with four switchable travel looks: corgi, koala, penguin, and kangaroo.
- Desktop-pet pixel art uses 480 transparent frames: six four-frame loops for every companion and
  fitted weather outfit.
- Auto wardrobe reacts to temperature, WMO weather condition, and wind speed.
- Manual wardrobe override supports everyday, sun, rain, wind, and cold modes.
- Mood and fatigue react to `activeMinutes` and `stepsSinceBreak`.
- A mood remains stable while breathing, blinking, tail/ear motion and other micro-animation loop
  continuously inside it; Buddy no longer changes emotion at random.
- Head/body taps, holds, drags, treats and accelerometer shakes temporarily interrupt the current
  loop and then return to the same mood.
- All interaction happens directly on the Pet page; there is no separate Play with Buddy screen.
- Travel check-ins unlock additional companion looks.
- Selected look and wardrobe mode persist locally with `SharedPreferences`.
- Current weather uses Open-Meteo as a key-free development fallback. Production can pass Yan's
  weather payload into `PetEnvironmentSnapshot` and remove the fallback call.

### Pet Camera

- Front/rear camera switch using CameraX.
- ML Kit face tracking positions Buddy near the traveller.
- Buddy can be hidden, tracking can be disabled, and the overlay can always be dragged manually.
- The captured file includes the camera preview, Buddy, outfit, weather, and location stamp.
- JPEG output is saved to `Pictures/RoamMate` on Android 10+.
- Camera permission is requested only when the camera experience opens.

## Navigation Flow

```text
Login
├── Log in -> Home
└── Create account -> Create Account -> Home

Home
├── Plan My Trip -> Plan My Trip
├── Today's trip -> Trip
├── Navigate -> external map
├── Smart suggestion -> Adjust Itinerary
├── Pet card -> Pet / Companions
└── Bottom tabs -> Home / Trip / Explore / Pet / Profile

Plan My Trip
├── Back -> Home
├── Destination -> city picker dialog
├── Date -> date range picker dialog
├── Start time -> time picker dialog
├── + More -> Interests (current trip only)
├── Search places to add -> Add a Stop
└── Generate itinerary -> Trip

Interests
├── Back -> previous page
└── Done -> Plan My Trip or Profile

Trip
├── Timeline stop -> Attraction Detail
├── Edit itinerary -> Edit Itinerary
├── Start navigation -> external map
└── Bottom tabs -> Home / Trip / Explore / Pet / Profile

Adjust Itinerary
├── Apply this plan -> Trip
├── Regenerate another -> next generated plan
├── Back to previous plan -> previous generated plan
└── Keep current plan -> Trip

Edit Itinerary
├── Back -> Trip
├── Remove stop -> update temporary itinerary
├── + Add a stop -> Add a Stop
└── Save -> Trip

Add a Stop
├── Back -> Plan My Trip or Edit Itinerary
├── Search confirm -> search results
└── Add result -> return to previous flow

Explore
├── Filter chips -> refresh nearby list
├── Place card/map marker -> Attraction Detail
└── Bottom tabs -> Home / Trip / Explore / Pet / Profile

Attraction Detail
├── Back -> Explore or Trip
├── Add to trip -> add and return Trip
├── Navigate -> external map
└── Visit official website -> external browser

Profile
├── Travel preferences -> Interests (long-term default)
├── Saved trips -> Saved Trips
├── Edit profile -> Edit Profile
└── Log out -> Login

Pet / Companions
├── Select Buddy look or weather wardrobe mode
├── Refresh live weather
├── Tap / hold / drag / shake -> immediate Buddy reaction
├── Photo -> Pet Camera -> system gallery
└── Bottom tabs -> Home / Trip / Explore / Pet / Profile

Saved Trips
├── Back -> Profile
└── Create new trip / Plan a trip -> Plan My Trip

Edit Profile
├── Back -> Profile
└── Save changes -> Profile
```

## Current Temporary Data

Most data is currently stored in `MainActivity.kt` as front-end state so the UI can be tested before backend integration.

```kotlin
userName                 // temporary user name
profileDefaultInterests  // long-term profile preference placeholder
tripInterests            // current trip interests
planRequiredPlaces       // must-visit places added from Plan My Trip
tripTimelineStops        // temporary itinerary stops
adjustPlanIndex          // selected adjust itinerary candidate
selectedAttractionDetail // current detail page data
```

These should be replaced by real data sources later.

## Data Needed By Page

### Login

Yuxiang: authLogin

```kotlin
data class AuthLoginRequest(
    val email: String,
    val password: String,
)

data class AuthLoginResult(
    val success: Boolean,
    val userId: String?,
    val errorMessage: String?,
)
```

Example:

```json
{
  "email": "yufei@example.com",
  "password": "123456"
}
```

### Create Account

Yuxiang: authRegister

```kotlin
data class AuthRegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
)

data class AuthRegisterResult(
    val success: Boolean,
    val userId: String?,
    val errorMessage: String?,
)
```

Example:

```json
{
  "fullName": "Yufei",
  "email": "yufei@example.com",
  "password": "123456"
}
```

### Profile

Yuxiang: userProfile

```kotlin
data class UserProfile(
    val userId: String,
    val name: String,
    val email: String,
    val defaultInterests: List<String>,
)
```

Example:

```json
{
  "userId": "user_001",
  "name": "Yufei",
  "email": "yufei@example.com",
  "defaultInterests": ["Museums", "Parks", "Food"]
}
```

### Edit Profile

Yuxiang: updateUserProfile

```kotlin
data class UpdateUserProfileRequest(
    val userId: String,
    val name: String,
)
```

Example:

```json
{
  "userId": "user_001",
  "name": "Yufei"
}
```

### Saved Trips

Yuxiang: savedTrips

```kotlin
data class SavedTrip(
    val tripId: String,
    val userId: String,
    val destination: String,
    val durationDays: Int,
    val dateRangeText: String,
    val createdAt: String,
)
```

Example:

```json
{
  "tripId": "trip_001",
  "userId": "user_001",
  "destination": "Melbourne",
  "durationDays": 3,
  "dateRangeText": "12 Sep 2026 - 14 Sep 2026",
  "createdAt": "2026-09-16T00:40:00+10:00"
}
```

### Interests

Yuxiang: profileDefaultInterests

```kotlin
data class ProfileInterestsData(
    val userId: String,
    val interests: List<String>,
)
```

Example:

```json
{
  "userId": "user_001",
  "interests": ["Museums", "Parks", "Food", "Art", "Wildlife"]
}
```

Zewen: tripInterests

```kotlin
data class TripInterestsData(
    val tripDraftId: String,
    val interests: List<String>,
)
```

Example:

```json
{
  "tripDraftId": "draft_001",
  "interests": ["Museums", "Parks", "Food"]
}
```

### Plan My Trip

Zewen: planMyTripRequest

```kotlin
data class PlanMyTripRequest(
    val destination: String,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val interests: List<String>,
    val transportMode: String,
    val specificPlaces: List<String>,
)
```

Example:

```json
{
  "destination": "Melbourne",
  "startDate": "2026-09-12",
  "endDate": "2026-09-13",
  "startTime": "10:00",
  "interests": ["Museums", "Parks", "Food"],
  "transportMode": "Transit",
  "specificPlaces": ["Melbourne Museum"]
}
```

Yan/Leyan: availablePlaces

```kotlin
data class PlaceSummary(
    val placeId: String,
    val name: String,
    val city: String,
    val category: String,
    val environmentType: String,
    val latitude: Double,
    val longitude: Double,
)
```

Example:

```json
{
  "placeId": "place_melbourne_museum",
  "name": "Melbourne Museum",
  "city": "Melbourne",
  "category": "Museum",
  "environmentType": "Indoor",
  "latitude": -37.8033,
  "longitude": 144.9717
}
```

### Add a Stop

Alex/Yan: placeSearch

```kotlin
data class PlaceSearchRequest(
    val query: String,
    val city: String,
    val latitude: Double?,
    val longitude: Double?,
)

data class AddStopPlace(
    val placeId: String,
    val name: String,
    val distanceText: String,
    val environmentType: String,
)
```

Example:

```json
{
  "query": "gallery",
  "city": "Melbourne",
  "latitude": -37.8136,
  "longitude": 144.9631
}
```

Zewen: addRequiredPlace

```kotlin
data class AddRequiredPlaceRequest(
    val tripDraftId: String,
    val placeId: String,
    val name: String,
)
```

Example:

```json
{
  "tripDraftId": "draft_001",
  "placeId": "place_melbourne_museum",
  "name": "Melbourne Museum"
}
```

### Trip

Zewen: itinerary

```kotlin
data class ItineraryStop(
    val stopId: String,
    val placeId: String,
    val title: String,
    val startTime: String,
    val endTime: String?,
    val order: Int,
    val latitude: Double?,
    val longitude: Double?,
    val hiddenTag: String?,
)
```

Example:

```json
{
  "stopId": "stop_001",
  "placeId": "place_melbourne_museum",
  "title": "Melbourne Museum",
  "startTime": "10:00",
  "endTime": "11:30",
  "order": 1,
  "latitude": -37.8033,
  "longitude": 144.9717,
  "hiddenTag": null
}
```

Sitao: itineraryProgress

```kotlin
data class ItineraryProgress(
    val currentStopId: String,
    val doneStopIds: List<String>,
    val nextStopId: String?,
)
```

Example:

```json
{
  "currentStopId": "stop_001",
  "doneStopIds": ["stop_000"],
  "nextStopId": "stop_002"
}
```

Yan: tripWeatherSummary

```kotlin
data class TripWeatherSummary(
    val temperature: String,
    val condition: String,
)
```

Example:

```json
{
  "temperature": "18°C",
  "condition": "Partly cloudy"
}
```

### Home

Zewen/Sitao: leaveNowReminder

```kotlin
data class HomeLeaveNowReminder(
    val placeName: String,
    val scheduledTime: String,
    val delayMinutes: Int,
    val navigationQuery: String,
)
```

Example:

```json
{
  "placeName": "Melbourne Museum",
  "scheduledTime": "10:00",
  "delayMinutes": 3,
  "navigationQuery": "Melbourne Museum"
}
```

Yan/Zewen: smartSuggestion

```kotlin
data class HomeSmartSuggestion(
    val label: String,
    val message: String,
    val reasonType: String,
)
```

Example:

```json
{
  "label": "Rain",
  "message": "Smart suggestion · Rain 2-4 PM, tap to adjust your plan",
  "reasonType": "weather_rain"
}
```

Zewen/Sitao: todayTripPreview

```kotlin
data class HomeTripStop(
    val time: String,
    val title: String,
    val subtitle: String,
    val status: String,
)
```

Example:

```json
{
  "time": "10:00",
  "title": "Melbourne Museum",
  "subtitle": "Now",
  "status": "Current"
}
```

Jie/Xiajie: pet context (implemented by `PetEnvironmentSnapshot`)

```kotlin
data class PetEnvironmentSnapshot(
    val condition: PetWeatherCondition,
    val conditionLabel: String,
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val activeMinutes: Int,
    val stepsSinceBreak: Int,
    val locationLabel: String,
    val source: String,
)
```

Example:

```json
{
  "condition": "Rain",
  "conditionLabel": "Rain",
  "temperatureC": 14.0,
  "windSpeedKmh": 18.0,
  "activeMinutes": 46,
  "stepsSinceBreak": 4300,
  "locationLabel": "Melbourne Museum",
  "source": "Team weather service"
}
```

### Adjust Itinerary

Yan: weatherAlert

```kotlin
data class WeatherAlert(
    val type: String,
    val timeRange: String,
    val description: String,
)
```

Example:

```json
{
  "type": "rain",
  "timeRange": "14:00-16:00",
  "description": "Heavy rain expected"
}
```

Zewen: adjustedItineraryPlan

```kotlin
data class AdjustItineraryPlan(
    val reasonLabel: String,
    val reasonDescription: String,
    val stops: List<AdjustItineraryStop>,
    val removedStop: AdjustRemovedStop?,
)

data class AdjustItineraryStop(
    val time: String,
    val title: String,
    val changeLabel: String,
    val changeTone: String,
)

data class AdjustRemovedStop(
    val title: String,
    val reason: String,
)
```

Example:

```json
{
  "reasonLabel": "Adjust for rain?",
  "reasonDescription": "Heavy rain 2-4 PM — here's a rewritten plan for today:",
  "stops": [
    {
      "time": "10:00",
      "title": "Melbourne Museum",
      "changeLabel": "Kept",
      "changeTone": "Neutral"
    },
    {
      "time": "12:30",
      "title": "State Library",
      "changeLabel": "Moved earlier",
      "changeTone": "Positive"
    }
  ],
  "removedStop": {
    "title": "Royal Botanic Gardens",
    "reason": "outdoor · rain"
  }
}
```

### Edit Itinerary

Zewen: editItineraryRequest

```kotlin
data class EditItineraryRequest(
    val tripId: String,
    val removedStopIds: List<String>,
    val addedPlaceIds: List<String>,
)
```

Example:

```json
{
  "tripId": "trip_001",
  "removedStopIds": ["stop_003"],
  "addedPlaceIds": ["place_state_library"]
}
```

### Explore

Alex/Sitao: currentLocation

```kotlin
data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)
```

Example:

```json
{
  "latitude": -37.8136,
  "longitude": 144.9631,
  "accuracyMeters": 12.0
}
```

Yan/Leyan/Sitao: nearbyPlace

```kotlin
data class ExplorePlace(
    val placeId: String,
    val name: String,
    val distanceText: String,
    val category: String,
    val environmentLabel: String,
    val tagText: String,
    val latitude: Double,
    val longitude: Double,
)
```

Example:

```json
{
  "placeId": "place_melbourne_museum",
  "name": "Melbourne Museum",
  "distanceText": "0.8 km",
  "category": "Indoor",
  "environmentLabel": "Indoor",
  "tagText": "Good for rain",
  "latitude": -37.8033,
  "longitude": 144.9717
}
```

### Attraction Detail

Leyan/Yan: attractionDetail

```kotlin
data class AttractionDetail(
    val placeId: String,
    val name: String,
    val distanceText: String,
    val environmentLabel: String,
    val weatherTag: String?,
    val todayHours: AttractionOpeningHours?,
    val weeklyHours: List<AttractionOpeningHours>,
    val websiteUrl: String?,
    val imageUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
)

data class AttractionOpeningHours(
    val dayLabel: String,
    val timeRange: String,
)
```

Example:

```json
{
  "placeId": "place_melbourne_museum",
  "name": "Melbourne Museum",
  "distanceText": "0.8 km away",
  "environmentLabel": "Indoor",
  "weatherTag": "Good for rain",
  "todayHours": {
    "dayLabel": "Today",
    "timeRange": "10:00 am - 5:00 pm"
  },
  "weeklyHours": [
    {
      "dayLabel": "Mon",
      "timeRange": "10:00 am - 5:00 pm"
    }
  ],
  "websiteUrl": "https://museumsvictoria.com.au/melbournemuseum/",
  "imageUrl": null,
  "latitude": -37.8033,
  "longitude": 144.9717
}
```

## Member Data Summary

Yuxiang: authentication, user profile, default interests, saved trips, save edited profile, save itinerary.

Zewen: itinerary generation, itinerary stops, add/remove stop re-planning, adjust itinerary alternatives, hidden-gem insertion.

Yan: weather data, weather alerts, weather suitability tags, place category/type data.

Leyan: attraction details, opening hours, official website, attraction image/data source.

Sitao: current GPS/location, distance calculation, route duration, itinerary progress, done/current/upcoming status.

Alex: sensor/GPS input and place search support if search is handled on Alex's side.

Jie/Xiajie: pet/companion state, generated pet assets, interactions, weather wardrobe, and pet camera.

## Database Recommendation

Cloud database:

- User account.
- User profile.
- Long-term default interests.
- Saved trips.
- Saved itineraries.
- Historical trip records.

Backend/API response, not necessarily stored in front-end database:

- Current GPS location.
- Current distance to places.
- Current route duration.
- Weather alert for the current time.
- Smart suggestion message.
- Generated itinerary candidate.
- Adjust itinerary candidate.
- Current loading/error state.

Local front-end state:

- Current selected tab.
- Current input text.
- Temporary Plan My Trip draft before submit.
- Temporary current-trip interests.
- Dialog open/close state.
- Selected attraction detail page item.

Optional local cache:

- Recently viewed attractions.
- Last generated trip draft.
- Recently used city.

## Current Integration Notes

- `MainActivity.kt` is currently the navigation and temporary data center.
- Temporary mock data is clearly marked with `TODO`.
- Once backend is ready, replace mock lists and state in `MainActivity.kt`.
- The UI already has callbacks for most buttons, so backend integration should mostly happen in callback blocks.
- Pet is complete. Replace the demo activity/location fields and Open-Meteo fallback by assigning
  team payloads to `PetEnvironmentSnapshot` in `MainActivity.kt`.

## Pet Integration Contract

The team only needs to populate one object; `PetStateEngine` derives outfit, mood, status copy, and
travel tips. This avoids passing the chat's individual fields through multiple screens.

| Provider | `PetEnvironmentSnapshot` field | Notes |
| --- | --- | --- |
| Yan weather | `condition`, `conditionLabel`, `temperatureC`, `windSpeedKmh` | WMO codes can use `PetWeatherAdapter.fromWmoCode`. |
| Alex sensor | `activeMinutes`, `stepsSinceBreak` | If `TYPE_STEP_COUNTER` is unavailable, pass the accelerometer-derived fallback. |
| Sitao location | `locationLabel` | A place name is sufficient; exact coordinates stay outside the pet UI. |
| Yuxiang database | `PetProfile` | Mirror `selectedStyle`, `outfitMode`, and `checkedInPlaces` when Firebase is ready. |

Weather fallback data is provided by [Open-Meteo](https://open-meteo.com/) and must retain visible
attribution if it remains in the final app.
