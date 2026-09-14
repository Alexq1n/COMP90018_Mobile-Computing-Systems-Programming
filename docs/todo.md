## Implementation plan

### Phase 1: Project Foundation & Data Layer

- [x] **Task 1: Initialize Kotlin Android Library Module**
  - **Context:** Set up the Android library module specifically for business logic (`build.gradle.kts`), leaving out UI dependencies.
  - **Dependencies:** None
  - **Subtasks:**
    - [x] 1.1 Configure `build.gradle.kts` with `compileSdk 34`, `minSdk 26`, `targetSdk 34`, and standard AGP/Kotlin versions.
    - [x] 1.2 Add `kotlinx.serialization` or `Gson` dependencies.
    - [x] 1.3 Setup the folder structure (e.g., `models`, `engine`, `utils`, `interfaces`).
- [x] **Task 2: Implement Data Classes**
  - **Context:** Translate the PRD's schemas into Kotlin Data Classes.
  - **Dependencies:** Task 1
  - **Subtasks:**
    - [x] 2.1 Define `POI` data class (id, name, base_score, category, lat, lng, visit_duration, etc.).
    - [x] 2.2 Define `UserProfile` data class.
    - [x] 2.3 Define `ContextPayload` data class.
- [x] **Task 3: Build Mock Repositories**
  - **Context:** Since actual DB infrastructure is out of scope, create in-memory mock repositories to test algorithms.
  - **Dependencies:** Task 2
  - **Subtasks:**
    - [x] 3.1 Create an in-memory list of 30+ standard POIs across Melbourne.
    - [x] 3.2 Create an in-memory list of 15+ `is_filler=true` POIs (Hidden Gems).
    - [x] 3.3 Expose these via a `IPoiRepository` interface.

### Phase 2: Expected Value (EV) Engine

- [x] **Task 4: Build Base EV Calculator**
  - **Context:** Create a Kotlin Object/Class that calculates the initial `Final_EV`.
  - **Dependencies:** Task 2
  - **Subtasks:**
    - [x] 4.1 Implement interest multiplier logic.
    - [x] 4.2 Implement budget multiplier/penalty logic.
- [x] **Task 5: Integrate Weather Modifiers**
  - **Context:** Modify the EV dynamically based on real-time weather inputs.
  - **Dependencies:** Task 4
  - **Subtasks:**
    - [x] 5.1 Add strong penalty modifier for `outdoor` POIs if weather is 'rainy'.
    - [x] 5.2 Add slight bonus for `indoor` POIs during bad weather.
- [x] **Task 6: Implement Hard Time Window Pruning**
  - **Context:** Ensure closed places are ignored.
  - **Dependencies:** Task 5
  - **Subtasks:**
    - [x] 6.1 Write an extension function or helper to check remaining operating hours.
    - [x] 6.2 Filter out POIs from the candidate list if `remaining_time < visit_duration` (Force EV to 0).

### Phase 3: Routing Engine & Geo-Clustering

- [x] **Task 7: Implement Travel Cost Service**
  - **Context:** Create a service to estimate travel time.
  - **Dependencies:** Task 2
  - **Subtasks:**
    - [x] 7.1 Implement the Haversine formula to calculate raw distance between coordinates.
    - [x] 7.2 Create `calculateTravelTime(dist, transportMode)` returning estimated minutes based on average speeds.
- [x] **Task 8: Build Geographic Clustering Service**
  - **Context:** Pre-filter POIs into daily zones.
  - **Dependencies:** Task 3, Task 7
  - **Subtasks:**
    - [x] 8.1 Implement a simple radius-bounding or K-Means function to group POIs into spatial clusters.
    - [x] 8.2 Assign each day of the trip a specific cluster/zone.
- [x] **Task 9: Develop Baseline Heuristic Router**
  - **Context:** The core trip generation loop.
  - **Dependencies:** Task 6, Task 8
  - **Subtasks:**
    - [x] 9.1 Filter out `is_filler=true` items from the main loop.
    - [x] 9.2 Implement greedy heuristic: Iterate to select the next reachable POI with the highest `Final_EV / Cost` ratio.
    - [x] 9.3 Halt loop when daily time budget is exhausted.

### Phase 4: Dynamic Adjustment Module

- [x] **Task 10: Implement State Parser**
  - **Context:** Calculate the "new normal" after a delay.
  - **Dependencies:** Task 9
  - **Subtasks:**
    - [x] 10.1 Parse `ContextPayload` to calculate the newly available time window.
    - [x] 10.2 Filter out completed POIs.
- [x] **Task 11: Implement Option A (Experience First)**
  - **Context:** Strategy for maximum absolute value.
  - **Dependencies:** Task 10
  - **Subtasks:**
    - [x] 11.1 Re-run routing loop sorted purely by `Final_EV` descending.
- [x] **Task 12: Implement Option B (Efficiency First)**
  - **Context:** Strategy for lowest travel stress.
  - **Dependencies:** Task 10
  - **Subtasks:**
    - [x] 12.1 Re-run routing loop sorted by `Final_EV / Travel_Cost`.
    - [x] 12.2 Apply heavy penalty to long distances.
- [x] **Task 13: Orchestrate `adjustItinerary` Function**
  - **Context:** Expose the single function that returns both options.
  - **Dependencies:** Task 11, Task 12
  - **Subtasks:**
    - [x] 13.1 Build the public `adjustItinerary()` function.
    - [x] 13.2 Format the output into a response data class containing both `OptionA` and `OptionB` lists.

### Phase 5: Advanced Features & Edge Cases

- [x] **Task 14: Implement Dynamic Fillers (Hidden Gems)**
  - **Context:** Inject micro-POIs into empty time gaps.
  - **Dependencies:** Task 9, Task 13
  - **Subtasks:**
    - [x] 14.1 Scan generated itinerary for time gaps > 20 mins.
    - [x] 14.2 Query `is_filler=true` POIs within a 500m radius of the transit path.
    - [x] 14.3 Insert the highest EV filler into the itinerary.
- [x] **Task 15: Implement High-EV Rollover Queue**
  - **Context:** Save highly desired POIs dropped today for tomorrow.
  - **Dependencies:** Task 13
  - **Subtasks:**
    - [x] 15.1 Identify high `base_score` POIs pruned due to time limits.
    - [x] 15.2 Store in a `RolloverQueue` object.
    - [x] 15.3 Update Task 9 (Baseline Router) to force-inject `RolloverQueue` items into the next day's candidate pool, ignoring geographic zone limits.

### Phase 6: Interface Exposure & Testing

- [x] **Task 16: Expose Public Kotlin UseCases/Interfaces**
  - **Context:** Provide the clean entry points for the UI/ViewModel teams to call this library.
  - **Dependencies:** Task 14, Task 15
  - **Subtasks:**
    - [x] 16.1 Define `ITripPlannerEngine` interface with methods `generateInitialTrip` and `adjustTripState`.
    - [x] 16.2 Implement the interface in a facade class.
- [x] **Task 17: Local Unit & Integration Testing**
  - **Context:** Validate logic purely within the Kotlin module using JUnit.
  - **Dependencies:** Task 16
  - **Subtasks:**
    - [x] 17.1 Write JUnit tests for the EV Engine (ensure rainy weather zeros out outdoor POIs).
    - [x] 17.2 Write integration tests for `adjustItinerary` simulating a 2-hour delay, asserting Option A and B differ as expected.

### Phase 7: Enhanced Travel Cost & User Preferences

- [x] **Task 18: Update Travel Cost Algorithm with Real Data**
  - **Context:** Replace Haversine-based estimation with actual walking/driving durations from upstream services.
  - **Dependencies:** Task 7
  - **Subtasks:**
    - [x] 18.1 Define data models for `TravelDuration` (walking_minutes, driving_minutes) between POI pairs.
    - [x] 18.2 Update `ITravelCostService` interface to accept real duration data instead of calculating from distance.
    - [x] 18.3 Implement user preference logic: apply multiplier to walking/driving costs based on `UserProfile.transport_preference`.
    - [x] 18.4 Refactor routing engine (Task 9) to use preference-adjusted travel costs in EV/Cost ratio calculations.

### Phase 8: User-Editable Itinerary

- [x] **Task 19: Implement Add POI to Itinerary**
  - **Context:** Allow users to manually add specific POIs to their itinerary.
  - **Dependencies:** Task 16
  - **Subtasks:**
    - [x] 19.1 Create `addPoiToItinerary(itinerary, poi, targetDayIndex, insertPosition)` function.
    - [x] 19.2 Validate the POI can fit within the day's time budget (check operating hours and available time).
    - [x] 19.3 Insert POI at the specified position and recalculate travel costs for adjacent POIs.
- [x] **Task 20: Implement Remove POI from Itinerary**
  - **Context:** Allow users to remove specific POIs from their planned itinerary.
  - **Dependencies:** Task 16
  - **Subtasks:**
    - [x] 20.1 Create `removePoiFromItinerary(itinerary, poiId, dayIndex)` function.
    - [x] 20.2 Remove the specified POI and recalculate travel costs for the remaining route.
    - [x] 20.3 Return the freed time budget for that day.
- [x] **Task 21: Implement Itinerary Reoptimization After Edits**
  - **Context:** After manual add/remove operations, rebalance the itinerary to utilize freed time or accommodate new constraints.
  - **Dependencies:** Task 19, Task 20
  - **Subtasks:**
    - [x] 21.1 Create `reoptimizeDay(dayItinerary, availableTime, contextPayload)` function.
    - [x] 21.2 Apply the greedy heuristic routing (Task 9) to the modified day with remaining time budget.
    - [x] 21.3 Preserve user-added POIs as "pinned" items that cannot be removed during reoptimization.
    - [x] 21.4 Expose unified `editItinerary(action, params)` interface combining add/remove/reoptimize operations.

### Phase 9: Weather-Triggered Replanning

- [x] **Task 22: Implement Current Weather Context Parser**
  - **Context:** Process real-time weather data received when user opens the app.
  - **Dependencies:** Task 5
  - **Subtasks:**
    - [x] 22.1 Extend `ContextPayload` to include `current_weather` field (e.g., "rainy", "sunny", "cloudy").
    - [x] 22.2 Create `parseWeatherContext(contextPayload)` function to extract weather status and timestamp.
- [x] **Task 23: Detect Outdoor Activities on Rainy Days**
  - **Context:** Check if today's itinerary contains outdoor POIs when weather is rainy.
  - **Dependencies:** Task 22
  - **Subtasks:**
    - [x] 23.1 Create `hasOutdoorActivities(dayItinerary)` function checking POI `category` or `is_outdoor` flag.
    - [x] 23.2 Trigger replanning flow only if both conditions met: rainy weather AND outdoor activities present.
- [x] **Task 24: Generate Three Rainy Day Options**
  - **Context:** Provide user with three choices when rainy day outdoor conflict detected.
  - **Dependencies:** Task 23, Task 11, Task 12
  - **Subtasks:**
    - [x] 24.1 **Option 1:** Return original itinerary unchanged ("Continue with current plan").
    - [x] 24.2 **Option 2:** Apply "Experience First" strategy (Task 11) with strong outdoor penalty to generate indoor-focused alternative.
    - [x] 24.3 **Option 3:** Apply "Efficiency First" strategy (Task 12) with strong outdoor penalty to generate nearby-indoor alternative.
    - [x] 24.4 Create `RainyDayOptions` data class containing all three options with descriptive labels.
    - [x] 24.5 Expose `handleRainyDayScenario(currentItinerary, contextPayload)` function returning `RainyDayOptions`.

### Phase 10: Fatigue Detection & Response

- [x] **Task 25: Implement Fatigue Detection Algorithm**
  - **Context:** Determine if user is fatigued based on recent step count data.
  - **Dependencies:** Task 2
  - **Subtasks:**
    - [x] 25.1 Extend `ContextPayload` to include `recent_step_count` (steps in the last 1 hour).
    - [x] 25.2 Define fatigue threshold (e.g., < 500 steps/hour = low activity, > 5000 steps/hour = high fatigue).
    - [x] 25.3 Create `detectFatigue(stepCount)` function returning fatigue level enum (LOW, MEDIUM, HIGH).
- [x] **Task 26: Adjust Itinerary Based on Fatigue Level**
  - **Context:** Modify remaining itinerary when high fatigue is detected.
  - **Dependencies:** Task 25
  - **Subtasks:**
    - [x] 26.1 Apply penalty to POIs with high `visit_duration` when fatigue is HIGH.
    - [x] 26.2 Boost EV of nearby, low-intensity POIs (e.g., cafes, parks with short visit times).
    - [x] 26.3 Suggest rest periods or reduce total POI count for remaining day.
    - [x] 26.4 Integrate fatigue adjustment into `adjustItinerary` function (Task 13) as an additional context factor.

### Phase 11: Data Interface Specification

- [x] **Task 27: Define Upstream Input Data Contract**
  - **Context:** Document all data required from upstream services for the trip planning engine.
  - **Dependencies:** All previous tasks
  - **Subtasks:**
    - [x] 27.1 Create `UPSTREAM_DATA_CONTRACT.md` listing all input data fields.
    - [x] 27.2 For each field, specify: name, description, data type, example value, and required/optional status.
    - [x] 27.3 Include: POI master data, travel duration matrices, real-time weather, user step count, user profile, current location/time.
- [x] **Task 28: Define Downstream Output Data Contract**
  - **Context:** Document all data outputs provided by the trip planning engine to downstream consumers (UI/ViewModel).
  - **Dependencies:** All previous tasks
  - **Subtasks:**
    - [x] 28.1 Create `DOWNSTREAM_DATA_CONTRACT.md` listing all output data structures.
    - [x] 28.2 For each output function, specify: function name, return type, data fields, and usage scenario.
    - [x] 28.3 Include outputs from: initial trip generation, adjustment options, rainy day options, fatigue warnings, edit results.