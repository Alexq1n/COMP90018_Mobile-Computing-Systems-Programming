# RoamMate Android UI

RoamMate is a travel planning Android app for Australia. This branch currently focuses on the front-end UI, screen navigation, and temporary data flow for the main travel planning workflow.

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

## Project Structure

```text
app/src/main/java/com/group5/roammate/
├── MainActivity.kt
├── ui/screens/
│   ├── LoginScreen.kt
│   ├── CreateAccountScreen.kt
│   ├── HomeScreen.kt
│   ├── PlanMyTripScreen.kt
│   ├── InterestsScreen.kt
│   ├── ProfileScreen.kt
│   ├── TripScreen.kt
│   ├── EditItineraryScreen.kt
│   ├── AddStopScreen.kt
│   └── RoamMateBottomNavigation.kt
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## Completed UI Screens

### Login

- Email and password input.
- Log in button.
- Create account navigation.
- Current behavior: successful mock login goes to Home.
- Future integration: Firebase authentication by Yuxiang.

### Create Account

- Full name, email, password, and confirm password fields.
- Basic front-end validation.
- Uses the shared RoamMate color style and mascot image.
- Future integration: Firebase registration and user profile creation by Yuxiang.

### Home

- Greeting section.
- Leave-now reminder card.
- Smart suggestion card.
- Pet status card.
- Today's trip preview.
- Plan My Trip and Explore shortcuts.
- Bottom navigation.
- Current behavior: trip data and pet data are temporary mock data.

### Plan My Trip

- Destination picker for major Australian cities.
- Date range picker.
- Start time picker.
- Interest selection.
- Transport mode selection: Walk, Transit, Drive.
- Add specific places entry.
- Generate itinerary button.
- Current behavior: creates a `PlanMyTripRequest` object and navigates to Trip.
- Future integration: send request data to Zewen's itinerary generation logic.

### Interests

- Reusable interest selection page.
- Entry 1: Plan My Trip `+ More`, saves temporary interests for the current trip only.
- Entry 2: Profile `Travel preferences`, saves long-term default interests.
- Future integration: profile preferences should be stored by Yuxiang; trip interests should be sent to Zewen only for the current itinerary.

### Profile

- User profile summary.
- Travel preferences entry.
- Saved trips entry.
- Edit profile button.
- Log out button.
- Bottom navigation.
- Current behavior: user data is mock data.
- Future integration: user information and saved trips from Yuxiang/Firebase.

### Trip

- Timeline for today's itinerary.
- Weather summary.
- Edit itinerary button.
- Start navigation button.
- Bottom navigation.
- Current behavior: itinerary data is temporary mock data in `MainActivity`.
- Future integration: itinerary list from Zewen, real-time progress/location from Sitao, weather data from Yan.

### Edit Itinerary

- Shows current and upcoming stops.
- Allows removing stops.
- Has an Add a stop entry.
- Save button returns to Trip.
- Current behavior: updates temporary front-end state.
- Future integration: add/remove operations should trigger Zewen's itinerary re-generation or re-ordering logic.

### Add a Stop

- Shared page used by two entry points:
  - Plan My Trip `Search places to add`
  - Edit Itinerary `+ Add a stop`
- Search field filters local mock results.
- Shows `Not found` when there is no match.
- Displays popular places in the current city instead of "results near you".
- Current behavior:
  - From Plan My Trip: added places are stored as required places for the current trip request.
  - From Edit Itinerary: added places are temporarily inserted into the current itinerary.
- Future integration: search/place data from Yan, distance/location from Sitao, itinerary placement from Zewen.

## Navigation Flow

```text
Login
├── Log in -> Home
└── Create account -> Create Account -> Home

Home
├── Plan My Trip -> Plan My Trip
├── Today's trip -> Trip
├── Navigate -> external map
├── Smart suggestion -> Adjust Itinerary (future)
└── Bottom tabs -> Home / Trip / Explore / Pet / Profile

Plan My Trip
├── Back -> Home
├── + More -> Interests (current trip only)
├── Search places to add -> Add a Stop
└── Generate itinerary -> Trip

Profile
├── Travel preferences -> Interests (long-term default preferences)
├── Saved trips -> Saved Trips (future)
├── Edit profile -> Edit Profile (future)
└── Log out -> Login

Trip
├── Timeline stop -> Attraction Detail (future)
├── Edit itinerary -> Edit Itinerary
├── Start navigation -> external map
└── Bottom tabs -> Home / Trip / Explore / Pet / Profile

Edit Itinerary
├── Back -> Trip
├── Remove stop -> update temporary itinerary
├── + Add a stop -> Add a Stop
└── Save -> Trip
```

## Current Temporary Data

Most data is currently stored in `MainActivity.kt` as front-end state so the UI can be tested before backend integration.

```kotlin
profileDefaultInterests // long-term profile preference placeholder
tripInterests           // current trip interests
planRequiredPlaces      // must-visit places added from Plan My Trip
tripTimelineStops       // temporary itinerary stops
```

These should be replaced by real data sources later.

## Main Data Models Used by UI

### PlanMyTripRequest

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

This object should be passed to Zewen's itinerary generation logic.

### TripTimelineStop

```kotlin
data class TripTimelineStop(
    val time: String,
    val title: String,
    val status: TripStopStatus,
)
```

This should later come from the generated itinerary and progress state.

### AddStopPlace

```kotlin
data class AddStopPlace(
    val name: String,
    val distanceText: String,
    val environmentType: String,
)
```

This should later come from the attraction search/place dataset.

## Future Integration Plan

### Yuxiang

- Firebase log in.
- Firebase create account.
- User profile data.
- Saved trips.
- Long-term travel preferences.
- Save itinerary after generation/editing.

### Zewen

- Generate itinerary from `PlanMyTripRequest`.
- Use destination, date range, start time, interests, transport mode, and specific places.
- Re-generate or re-order itinerary after adding/removing stops.
- Return itinerary stops in the same shape needed by `TripTimelineStop` or an agreed backend model.

### Yan

- Attraction/place data.
- Place search result data.
- Indoor/outdoor type.
- Weather data for Home and Trip.
- Smart suggestion/weather adjustment trigger.

### Sitao

- Current location.
- Distance to places.
- Route duration.
- Current itinerary progress.
- Decide which stop is done/current/upcoming.

### Pet Module

- Replace temporary mascot/pet status data.
- Provide real pet state and image according to weather, walking time, and fatigue.

## Known Limitations

- No real authentication yet.
- No persistent storage yet.
- Itinerary and place data are mock data.
- Search only filters local mock places.
- External map navigation uses a simple Android map intent.
- Explore, Pet, Saved Trips, Edit Profile, Attraction Detail, and Adjust Itinerary are future pages.

## Run Locally

1. Open the project in Android Studio.
2. Sync Gradle.
3. Select an emulator or Android device.
4. Run the `app` configuration.

Optional command line build:

```bash
./gradlew :app:assembleDebug
```

## Git Notes

- Work on the `yufei_ui` branch for UI changes.
- Do not commit `.idea/`.
- Before pushing, run:

```bash
git status
git pull --rebase origin yufei_ui
git push origin yufei_ui
```
