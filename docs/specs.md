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
  - **Strategic Dynamic Adjustment:** Triggered by severe delays or weather changes. Recalculates the remaining itinerary and outputs exactly two distinct choices:
    - **Option A (Experience First):** Maximizes remaining total EV, retaining high-score POIs even if travel cost is higher.
    - **Option B (Efficiency First):** Maximizes EV/Cost ratio, prioritizing nearby POIs to reduce travel fatigue.
  - **Dynamic Fillers (Hidden Gems):** Injects micro-POIs (e.g., ice cream shops, boutiques) seamlessly into the existing route via spatial queries along the path buffer zone, strictly when time surpluses exist.
  - **High-EV Rollover Mechanism:** Captures high-EV POIs dropped due to daily constraints and places them in a priority queue for potential insertion into subsequent travel days.

  ## Core components

  ### 1. Data Models

  - **POI Schema:** `id`, `name`, `base_score` (0-10), `category`, `budget_level`, `environment` (indoor/outdoor), `coordinates` (lat, lng), `recommended_visit_duration` (minutes), `operating_hours` (open/close time), `is_filler` (boolean).
  - **User Profile Schema:** `interests` (list of categories), `budget_preference`, `transport_mode`.
  - **Context Payload:** `current_location`, `current_time`, `weather_status`, `itinerary_progress` (delay_minutes).

  ### 2. Expected Value (EV) Engine

  - **Formula:** `Final_EV = base_score * interest_weight * budget_weight * weather_modifier`
  - **Logic:** Modifiers apply positive multipliers for matches (e.g., category matches interest) and penalties for mismatches. Weather strictly penalizes outdoor POIs during rain and boosts indoor ones.

  ### 3. Routing Engine

  - **Cost Evaluator:** Interfaces with routing estimates (distance/time) based on `transport_mode`.
  - **Heuristic Re-ranker:** Processes the EV and Cost to sequence POIs efficiently within the `available_time_window`.

  ## App/user flow

  1. **Initialization:** UI module passes user destination (e.g., Melbourne), dates, interests, budget, and transport mode to the Logic engine.
  2. **Pre-processing:** Engine clusters POIs geographically and groups them by day.
  3. **Initial Generation:** Routing Engine calculates `Final_EV` and returns the baseline itinerary to the UI.
  4. **In-Trip Trigger:** UI detects a delay or sudden weather change, prompts the user, and sends a `Context Payload` to the logic engine.
  5. **Dynamic Adjustment:**
     - Engine prunes POIs that violate operating hours.
     - Calculates Option A (Max EV) and Option B (Max EV/Cost).
     - Pushes dropped high-EV POIs to the Rollover Queue.
  6. **Output:** Engine returns Option A and B payloads for the UI to render.
  7. **Micro-Exploration:** During normal transit, if the engine detects a time surplus > filler duration, it triggers a Hidden Gem recommendation.

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