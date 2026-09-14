# Product Requirements Document (PRD)

## Project overview

    This document outlines the architecture and logic implementation for the **Context-Aware Logic and Trip Planning** module of the Smart Travel Companion application. The module is a dedicated core logic engine responsible for generating dynamic, multi-day travel itineraries and adapting them in real-time based on contextual changes (user delays, weather shifts). The core objective is to maximize the Expected Value (EV) of a trip within constrained time budgets, functioning as a flexible, intelligent routing engine.

### Scope and Boundaries

- **In-Scope:** The algorithmic logic, mathematical models (Expected Value calculations), dynamic routing heuristics, multi-day itinerary generation, and strategic adjustment algorithms (Option A vs. Option B).
- **Out of Scope (Strictly Handled by Other Teams):**
  - **Frontend / UI:** All visual elements, virtual pet rendering, map UI, and screen navigation.
  - **User Authentication:** Login, registration, and session management.
  - **Database Infrastructure:** Database hosting, schema migrations, and network-level data persistence (the logic module will rely on provided mock data or data interfaces/repositories).
  - **Hardware Integration:** Direct calls to device GPS, pedometer, or OS-level sensor APIs (the logic module consumes pre-parsed context payloads).

- ## Core requirements

  - **Performance Optimization:** Must utilize geographic clustering and heuristic algorithms rather than standard global TSP to ensure low latency on mobile devices.
  - **Time Window Strictness:** Must respect hard time constraints. If a Point of Interest's (POI) remaining operating hours are less than its `recommended_visit_duration`, it must be pruned (EV = 0).
  - **Path Topology:** Generates "Open Path" itineraries (no mandatory return to the starting point/hotel required at the end of the day).
  - **Cost Calculation:** Must support variable travel costs based on user-predefined `transport_mode` (e.g., walking, public transit, driving).

  ## Core features

  - **Multi-Day Geographic Clustering:** Pre-filters the global POI database into geographically grouped daily candidate pools to prevent inefficient cross-city commuting.
  - **Dynamic EV-Based Routing:** Calculates optimal daily routes by maximizing total EV based on user interests, budget, and real-time weather constraints.
  - **User-Editable Itinerary:** Supports manual addition/removal of specific POIs by users, with automatic reoptimization of the affected day while preserving user-pinned POIs.
  - **Real-Time Weather Adaptation:** When user opens app on a rainy day with outdoor activities scheduled, provides three options:
    - Continue with original plan
    - Experience First strategy with indoor alternatives
    - Efficiency First strategy with nearby indoor alternatives
  - **Fatigue Detection & Response:** Monitors user step count (last 1 hour) to detect fatigue levels and adjusts remaining itinerary by penalizing long-duration POIs and boosting nearby low-intensity options.
  - **Strategic Dynamic Adjustment:** Triggered by severe delays or weather changes. Recalculates the remaining itinerary and outputs exactly two distinct choices:
    - **Option A (Experience First):** Maximizes remaining total EV, retaining high-score POIs even if travel cost is higher.
    - **Option B (Efficiency First):** Maximizes EV/Cost ratio, prioritizing nearby POIs to reduce travel fatigue.
  - **Dynamic Fillers (Hidden Gems):** Injects micro-POIs (e.g., ice cream shops, boutiques) seamlessly into the existing route via spatial queries along the path buffer zone, strictly when time surpluses exist.
  - **High-EV Rollover Mechanism:** Captures high-EV POIs dropped due to daily constraints and places them in a priority queue for potential insertion into subsequent travel days.

  ## Core components

  ### 1. Data Models

  - **POI Schema:** `id`, `name`, `base_score` (0-10), `category`, `environment` (indoor/outdoor), `coordinates` (lat, lng), `recommended_visit_duration` (minutes), `operating_hours` (open/close time), `is_filler` (boolean).
  - **User Profile Schema:** `interests` (list of categories), `transport_mode` (WALKING or DRIVING only).
  - **Context Payload:** `current_location`, `current_time`, `weather_status`, `current_weather` (real-time weather when app opens, required), `itinerary_progress` (delay_minutes), `recent_step_count` (steps in last 1 hour, required).
  - **Travel Duration Schema:** `from_poi_id`, `to_poi_id`, `walking_minutes`, `driving_minutes` (real travel time data from upstream services).

  ### 2. Expected Value (EV) Engine

  - **Formula:** `Final_EV = base_score * interest_weight * weather_modifier`
  - **Logic:** Modifiers apply positive multipliers for matches (e.g., category matches interest) and penalties for mismatches. Weather strictly penalizes outdoor POIs during rain and boosts indoor ones.

  ### 3. Routing Engine

  - **Cost Evaluator:** Accepts real travel duration data (walking_minutes, driving_minutes) from upstream services between POI pairs. Uses the selected transport mode (WALKING or DRIVING) to calculate actual travel costs.
  - **Heuristic Re-ranker:** Processes the EV and Cost to sequence POIs efficiently within the `available_time_window`.

  ## App/user flow

  1. **Initialization:** UI module passes user destination (e.g., Melbourne), dates, interests, and transport mode to the Logic engine.
  2. **Pre-processing:** Engine clusters POIs geographically and groups them by day. Receives travel duration matrix (walking/driving times between POI pairs) from upstream services.
  3. **Initial Generation:** Routing Engine calculates `Final_EV` with preference-adjusted travel costs and returns the baseline itinerary to the UI.
  4. **User Editing:** User can manually add or remove POIs. Engine validates the edit, updates the itinerary, and reoptimizes the affected day while keeping user-pinned POIs fixed.
  5. **App Opening Weather Check:** When user opens app, UI sends current weather in `Context Payload`. If rainy weather detected AND today's itinerary contains outdoor POIs, engine returns three options:
     - Original plan (continue as scheduled)
     - Indoor-focused Experience First alternative
     - Indoor-focused Efficiency First alternative
  6. **In-Trip Delay Trigger:** UI detects a significant delay, prompts the user, and sends updated `Context Payload` to the logic engine.
  7. **Dynamic Adjustment:**
     - Engine prunes POIs that violate operating hours.
     - Calculates Option A (Max EV) and Option B (Max EV/Cost).
     - Pushes dropped high-EV POIs to the Rollover Queue.
  8. **Fatigue Detection:** Engine monitors `recent_step_count` from Context Payload. If high fatigue detected, adjusts remaining itinerary to reduce intensity.
  9. **Output:** Engine returns adjustment options or fatigue-adjusted itinerary payloads for the UI to render.
  10. **Micro-Exploration:** During normal transit, if the engine detects a time surplus > filler duration, it triggers a Hidden Gem recommendation.

  ## Techstack

  - **Language:** Kotlin (Version 1.9.x or later)
  - **Module Type:** Android Library / Pure Kotlin Business Logic Module
  - **Android Build Configuration:**
    - `compileSdk`: 34
    - `minSdk`: 26
    - `targetSdk`: 34
    - `AGP` (Android Gradle Plugin): 8.2.x (or project standard)
    - `Gradle`: 8.4+
  - **Algorithm Dependencies:** Standard Kotlin collections (`List`, `Sequence`) and math libraries. External heavy routing algorithms (like Map APIs) will be abstracted via Kotlin interfaces (Dependency Injection).
  - **Data Format:** `kotlinx.serialization` or `Gson` for parsing Context Payloads and outputting Itinerary data classes.

## Implementation plan

- **Phase 1: Data Modeling & Base Engine:** Define POI, User, and Context schemas. Implement the core `Final_EV` calculation function, including interest, budget, and weather modifiers.
- **Phase 2: Baseline Routing:** Implement geographic clustering for multi-day pre-filtering. Build the heuristic routing loop that maximizes EV against travel cost and time limits.
- **Phase 3: Dynamic Adjustment Logic:** Develop the `adjust_itinerary` endpoint. Implement the dual-strategy output (Option A vs. Option B) based on partial day completion and current constraints.
- **Phase 4: Edge Cases & Fillers:** Build the spatial query logic for inserting `is_filler=true` POIs (Hidden Gems). Implement the Rollover Queue for missed high-EV POIs to integrate into the next day's cluster.
- **Phase 5: Enhanced Travel Cost & User Preferences:** Update travel cost algorithm to use real walking/driving duration data from upstream. Integrate user transport preference multipliers into routing calculations.
- **Phase 6: User-Editable Itinerary:** Implement add/remove POI operations with validation and automatic reoptimization while preserving user-pinned items.
- **Phase 7: Weather-Triggered Replanning:** Build real-time weather context parser, outdoor activity detection, and three-option rainy day response system.
- **Phase 8: Fatigue Detection & Response:** Implement step count-based fatigue detection and adjust itinerary intensity accordingly.
- **Phase 9: Data Interface Specification:** Document complete upstream input and downstream output data contracts for seamless integration.

## Data interface specifications

### Upstream Input Data (from UI/Backend to Logic Engine)

| Data Name | Description | Data Type | Required | Example Value |
|-----------|-------------|-----------|----------|---------------|
| `destination` | Target city/region | String | Yes | "Melbourne" |
| `trip_dates` | Start and end dates | DateRange | Yes | "2026-09-20" to "2026-09-25" |
| `user_profile.interests` | User interest categories | List<String> | Yes | ["history", "nature", "food"] |
| `user_profile.budget_preference` | Budget level | Enum | Yes | "MEDIUM" |
| `user_profile.transport_mode` | Primary transport method | Enum | Yes | "WALKING" / "DRIVING" / "PUBLIC_TRANSIT" |
| `user_profile.transport_preference` | Walking vs driving preference weight | Float (0.0-2.0) | Yes | 1.5 (prefers walking) |
| `poi_master_data` | Complete POI database | List<POI> | Yes | See POI Schema |
| `travel_duration_matrix` | Pre-calculated travel times between POI pairs | List<TravelDuration> | Yes | [{from: "poi1", to: "poi2", walking_min: 15, driving_min: 5}] |
| `context_payload.current_location` | User's current GPS coordinates | LatLng | Yes | (-37.8136, 144.9631) |
| `context_payload.current_time` | Current timestamp | DateTime | Yes | "2026-09-20T14:30:00Z" |
| `context_payload.weather_status` | Historical/planned weather | String | Yes | "sunny" / "rainy" / "cloudy" |
| `context_payload.current_weather` | Real-time weather when app opens | String | Yes | "rainy" / "sunny" |
| `context_payload.delay_minutes` | Detected delay from schedule | Int | Optional | 120 |
| `context_payload.recent_step_count` | Steps in last 1 hour | Int | Optional | 4500 |
| `edit_action` | User edit operation type | Enum | Optional | "ADD_POI" / "REMOVE_POI" |
| `edit_params.poi_id` | Target POI for edit | String | Optional | "poi_123" |
| `edit_params.day_index` | Target day index (0-based) | Int | Optional | 2 |
| `edit_params.insert_position` | Position for POI insertion | Int | Optional | 3 |

### Downstream Output Data (from Logic Engine to UI/ViewModel)

| Function Name | Return Type | Data Fields | Usage Scenario |
|---------------|-------------|-------------|----------------|
| `generateInitialTrip()` | `TripItinerary` | `days: List<DayItinerary>`, `total_ev: Float`, `total_duration_minutes: Int` | Initial trip generation |
| `adjustTripState()` | `AdjustmentOptions` | `option_a: DayItinerary`, `option_b: DayItinerary`, `rollover_queue: List<POI>` | Delay-triggered adjustment |
| `handleRainyDayScenario()` | `RainyDayOptions` | `original: DayItinerary`, `experience_first: DayItinerary`, `efficiency_first: DayItinerary`, `trigger_reason: String` | Weather-triggered replanning |
| `editItinerary()` | `EditResult` | `updated_day: DayItinerary`, `success: Boolean`, `validation_message: String`, `freed_time_minutes: Int` | User manual edit |
| `detectFatigue()` | `FatigueStatus` | `level: FatigueLevel (LOW/MEDIUM/HIGH)`, `recommended_action: String`, `adjusted_itinerary: DayItinerary?` | Fatigue detection check |
| `insertHiddenGem()` | `FillerRecommendation` | `filler_poi: POI`, `insert_after_poi_id: String`, `time_window_minutes: Int` | Gap-filling during transit |

### Common Data Structures

**POI Schema:**
```kotlin
data class POI(
    val id: String,
    val name: String,
    val base_score: Float,           // 0.0 - 10.0
    val category: String,             // "museum", "park", "restaurant", etc.
    val budget_level: BudgetLevel,    // LOW, MEDIUM, HIGH
    val is_outdoor: Boolean,
    val coordinates: LatLng,
    val visit_duration_minutes: Int,
    val operating_hours: OperatingHours,
    val is_filler: Boolean
)
```

**DayItinerary Schema:**
```kotlin
data class DayItinerary(
    val day_index: Int,
    val date: LocalDate,
    val pois: List<ScheduledPOI>,
    val total_ev: Float,
    val total_travel_time_minutes: Int,
    val total_visit_time_minutes: Int
)

data class ScheduledPOI(
    val poi: POI,
    val arrival_time: LocalTime,
    val departure_time: LocalTime,
    val ev_score: Float,
    val is_user_pinned: Boolean       // True if manually added by user
)
```

**TravelDuration Schema:**
```kotlin
data class TravelDuration(
    val from_poi_id: String,
    val to_poi_id: String,
    val walking_minutes: Int,
    val driving_minutes: Int
)
```