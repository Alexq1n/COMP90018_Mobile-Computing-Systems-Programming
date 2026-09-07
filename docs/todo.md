### Phase 1: Project Foundation & Data Layer

The goal of this phase is to set up the backend environment, establish the data structures, and seed the database with spatial data to allow local testing.

- [ ] **Task 1: Initialize Backend Project environment**
  - **Context:** Set up the repository, framework (e.g., Node.js/TypeScript or Python/FastAPI), and linting/formatting tools. Establish the folder structure (routes, controllers, services, models).
  - **Dependencies:** None
  - **Subtasks:**
    - [ ] 1.1 Initialize git repository and package manager.
    - [ ] 1.2 Configure environment variables (e.g., `.env` for API keys, DB connection).
    - [ ] 1.3 Setup spatial database connection (e.g., PostgreSQL + PostGIS, or mock in-memory store for MVP).
- [ ] **Task 2: Implement Data Schemas/Models**
  - **Context:** Translate the PRD's data models into ORM models or type interfaces.
  - **Dependencies:** Task 1
  - **Subtasks:**
    - [ ] 2.1 Define `POI` model (`id`, `name`, `base_score`, `category`, `budget_level`, `environment`, `lat`, `lng`, `visit_duration`, `operating_hours`, `is_filler`).
    - [ ] 2.2 Define `UserProfile` schema (`interests`, `budget_preference`, `transport_mode`).
    - [ ] 2.3 Define `Context` payload interface (`current_location`, `current_time`, `weather_status`, `delay_minutes`).
- [ ] **Task 3: Generate and Seed Mock Data**
  - **Context:** Create a realistic dataset for Melbourne (the default city) to validate algorithms before hooking up production databases.
  - **Dependencies:** Task 2
  - **Subtasks:**
    - [ ] 3.1 Create 30+ standard POIs across different categories (Museums, Parks, Food) distributed across Melbourne CBD and suburbs.
    - [ ] 3.2 Create 15+ `is_filler=true` POIs (Hidden Gems like cafes, street art) clustered near main POIs.
    - [ ] 3.3 Write a seed script to populate the local database/store.

### Phase 2: Expected Value (EV) Engine

The goal of this phase is to build the core mathematical scoring logic that dictates how attractive a POI is under current conditions.

- [ ] **Task 4: Build Base EV Calculator**
  - **Context:** Create a service function that calculates the initial `Final_EV` utilizing interests and budget.
  - **Dependencies:** Task 2
  - **Subtasks:**
    - [ ] 4.1 Implement `interest_weight` logic (apply >1.0 multiplier if POI category is in user `interests`).
    - [ ] 4.2 Implement `budget_weight` logic (apply penalty/bonus based on user `budget_preference`).
- [ ] **Task 5: Integrate Weather Modifiers**
  - **Context:** Modify the EV dynamically based on real-time weather inputs.
  - **Dependencies:** Task 4
  - **Subtasks:**
    - [ ] 5.1 Implement logic to heavily penalize `outdoor` POIs if `weather_status` is 'rainy'.
    - [ ] 5.2 Implement logic to slightly boost `indoor` POIs during bad weather.
- [ ] **Task 6: Implement Hard Time Window Pruning**
  - **Context:** Ensure the algorithm never recommends a place that is closed or about to close.
  - **Dependencies:** Task 5
  - **Subtasks:**
    - [ ] 6.1 Write a helper to calculate remaining operating hours `(closing_time - current_time)`.
    - [ ] 6.2 Add strict filter: If `remaining_time < recommended_visit_duration`, force `Final_EV = 0`.

### Phase 3: Routing Engine & Geo-Clustering

The goal of this phase is to group POIs logically and string them together into a baseline itinerary.

- [ ] **Task 7: Implement Travel Cost Service**
  - **Context:** Create a service that calculates travel time between two coordinates based on `transport_mode`. For the MVP, this can use a mock calculation (straight-line distance * speed constant) before swapping to an external API (like Google Maps).
  - **Dependencies:** Task 2
  - **Subtasks:**
    - [ ] 7.1 Implement Haversine formula for basic distance calculation.
    - [ ] 7.2 Create a wrapper function `getTravelCost(pointA, pointB, transport_mode)` returning estimated minutes.
- [ ] **Task 8: Build Geographic Clustering Service**
  - **Context:** Pre-filter POIs to prevent cross-city travel on the same day.
  - **Dependencies:** Task 3, Task 7
  - **Subtasks:**
    - [ ] 8.1 Implement an algorithm (e.g., K-Means or simple radius bounding) to group POIs into "zones" (e.g., CBD zone, Beach zone).
    - [ ] 8.2 Create a function that assigns a daily zone to a multi-day trip (Day 1: Zone A, Day 2: Zone B).
- [ ] **Task 9: Develop Baseline Heuristic Router**
  - **Context:** The core trip generation loop for a single day. Creates an Open Path that maximizes EV within the daily time budget.
  - **Dependencies:** Task 6, Task 8
  - **Subtasks:**
    - [ ] 9.1 Filter daily POI pool to remove `is_filler=true` (save them for later).
    - [ ] 9.2 Implement a greedy heuristic algorithm: From `current_location`, iteratively select the next reachable POI with the highest `Final_EV / Cost` ratio.
    - [ ] 9.3 Loop until the sum of `(visit_durations + travel_times)` hits the daily time limit.

### Phase 4: Dynamic Adjustment Module

The goal of this phase is to handle in-trip disruptions (delays, weather) and output strategic alternatives.

- [ ] **Task 10: Implement State Parser**
  - **Context:** When a user is delayed, the system must establish the "new normal" (where they are, how much time is actually left).
  - **Dependencies:** Task 9
  - **Subtasks:**
    - [ ] 10.1 Create a function `parseCurrentState(itinerary, delay_minutes, current_location)` to calculate the updated `available_time_window`.
    - [ ] 10.2 Filter out already visited POIs from the candidate pool.
- [ ] **Task 11: Implement Option A (Experience First) Strategy**
  - **Context:** Generates an adjusted route focusing on absolute value.
  - **Dependencies:** Task 10
  - **Subtasks:**
    - [ ] 11.1 Modify the routing loop to sort purely by `Final_EV` (descending).
    - [ ] 11.2 Accept higher travel costs as long as it fits in the remaining time window.
- [ ] **Task 12: Implement Option B (Efficiency First) Strategy**
  - **Context:** Generates an adjusted route focusing on minimizing travel fatigue.
  - **Dependencies:** Task 10
  - **Subtasks:**
    - [ ] 12.1 Modify the routing loop to sort by `Final_EV / Travel_Cost` ratio.
    - [ ] 12.2 Apply a heavy penalty weight to distance, forcing the algorithm to select nearby POIs.
- [ ] **Task 13: Orchestrate `adjust_itinerary` Method**
  - **Context:** The main entry point for recalculation that returns the two distinct options.
  - **Dependencies:** Task 11, Task 12
  - **Subtasks:**
    - [ ] 13.1 Build the `adjust_itinerary()` wrapper that receives the Context Payload.
    - [ ] 13.2 Execute Option A and Option B sequentially and format the JSON response to contain both arrays of POIs.

### Phase 5: Advanced Features & Edge Cases

The goal of this phase is to add the "Hidden Gems" and multi-day Rollover functionality to make the app feel like a smart companion.

- [ ] **Task 14: Implement Hidden Gems (Dynamic Fillers) Injection**
  - **Context:** Inject micro-POIs into empty time gaps on an already calculated route.
  - **Dependencies:** Task 9, Task 13
  - **Subtasks:**
    - [ ] 14.1 Iterate through a generated itinerary to find time gaps (e.g., user has 30 mins spare before a restaurant reservation).
    - [ ] 14.2 Run a spatial query: find `is_filler=true` POIs within a short radius (e.g., 500m) of the path between POI `N` and POI `N+1`.
    - [ ] 14.3 Insert the highest EV filler into the itinerary array.
- [ ] **Task 15: Implement High-EV Rollover Queue**
  - **Context:** Save highly desired POIs that had to be dropped today.
  - **Dependencies:** Task 13
  - **Subtasks:**
    - [ ] 15.1 During Task 13's recalculation, identify POIs with high `base_score` that were pruned due to time limits.
    - [ ] 15.2 Store these POIs in a temporary `RolloverQueue` attached to the User's trip state.
    - [ ] 15.3 Update Task 9 (Baseline Router): Before generating Day 2's route, force-inject items from the `RolloverQueue` into Day 2's candidate pool, bypassing the strict geographic cluster filter.

### Phase 6: API Layer & Finalization

The goal of this phase is to expose the logic via HTTP endpoints and ensure system reliability.

- [ ] **Task 16: Expose API Endpoints**
  - **Context:** Create the controllers and routes to allow the frontend to communicate with this logic.
  - **Dependencies:** Task 14, Task 15
  - **Subtasks:**
    - [ ] 16.1 Create `POST /api/trip/generate` (takes User Profile, returns baseline multi-day itinerary).
    - [ ] 16.2 Create `POST /api/trip/adjust` (takes Context payload and delay/weather triggers, returns Option A & B).
- [ ] **Task 17: End-to-End Testing**
  - **Context:** Validate the logic mathematically using the mock data.
  - **Dependencies:** Task 16
  - **Subtasks:**
    - [ ] 17.1 Write a unit test for the EV Engine (ensure rainy weather zeros out outdoor parks).
    - [ ] 17.2 Write an integration test for `adjust_itinerary` (simulate a 2-hour delay in Melbourne CBD and assert Option A and B are mathematically distinct).