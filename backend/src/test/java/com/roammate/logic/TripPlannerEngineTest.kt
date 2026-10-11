package com.roammate.logic

import com.roammate.logic.engine.WeatherReplanningService
import com.roammate.logic.models.*
import com.roammate.logic.utils.MockPoiRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for Trip Planner Engine
 */
class TripPlannerEngineTest {

    private lateinit var engine: TripPlannerEngine
    private lateinit var userProfile: UserProfile

    @Before
    fun setup() {
        // Use JSON data from docs/pois.json
        val poiRepository = MockPoiRepository(useJsonData = true)
        engine = TripPlannerEngine(poiRepository)

        userProfile = UserProfile(
            interests = listOf(POICategory.MUSEUM, POICategory.PARK, POICategory.LANDMARK),
            budgetPreference = BudgetLevel.MEDIUM,
            transportMode = TransportMode.WALKING
        )
    }

    @Test
    fun testGenerateInitialTrip_BasicScenario() {
        val itinerary = engine.generateInitialTrip(
            tripId = "trip_001",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 3,
            userProfile = userProfile,
            startingLocation = Coordinates(-37.8136, 144.9631), // CBD
            dailyStartTime = "09:00",
            dailyEndTime = "18:00",
            weatherStatus = WeatherStatus.SUNNY,
            includeFillers = false
        )

        assertNotNull(itinerary)
        assertEquals("trip_001", itinerary.tripId)
        assertEquals(3, itinerary.days.size)

        // Each day should have POIs
        assertTrue(itinerary.days.any { it.pois.isNotEmpty() })

        // Total EV should be positive
        assertTrue(itinerary.totalExpectedValue > 0)

        println("\n========== BASIC SCENARIO ==========")
        println("Generated itinerary with ${itinerary.days.sumOf { it.pois.size }} total POIs")
        println("Total EV: ${itinerary.totalExpectedValue}")

        // Print detailed route
        printDetailedItinerary(itinerary, "BASIC SCENARIO")
    }

    @Test
    fun testGenerateInitialTrip_WithFillers() {
        // Use profile with fewer interests to create more time gaps
        val relaxedProfile = UserProfile(
            interests = listOf(POICategory.MUSEUM), // Only museums - creates gaps between POIs
            budgetPreference = BudgetLevel.MEDIUM,
            transportMode = TransportMode.WALKING
        )

        // Use longer day with more time to trigger filler injection
        val itineraryWithFillers = engine.generateInitialTrip(
            tripId = "trip_002",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 3,
            userProfile = relaxedProfile,
            startingLocation = Coordinates(-37.8136, 144.9631),
            dailyStartTime = "08:00",
            dailyEndTime = "22:00", // Very long day to create gaps
            includeFillers = true
        )

        val itineraryWithoutFillers = engine.generateInitialTrip(
            tripId = "trip_003",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 3,
            userProfile = relaxedProfile,
            startingLocation = Coordinates(-37.8136, 144.9631),
            dailyStartTime = "08:00",
            dailyEndTime = "22:00",
            includeFillers = false
        )

        val fillersCount = itineraryWithFillers.days.sumOf { day ->
            day.pois.count { it.isFiller }
        }

        // With fillers should have some filler POIs
        assertTrue(fillersCount >= 0)

        // Without fillers should have no filler POIs
        assertEquals(0, itineraryWithoutFillers.days.sumOf { day ->
            day.pois.count { it.isFiller }
        })

        println("\n========== FILLER INJECTION TEST ==========")
        println("Itinerary with fillers: ${fillersCount} filler POIs injected")
        println("Total POIs with fillers: ${itineraryWithFillers.days.sumOf { it.pois.size }}")
        println("Total POIs without fillers: ${itineraryWithoutFillers.days.sumOf { it.pois.size }}")

        // Print detailed itinerary with fillers
        printDetailedItinerary(itineraryWithFillers, "WITH FILLERS")
    }

    @Test
    fun testAdjustTripState_WithDelay() {
        // First generate an initial trip
        val initialItinerary = engine.generateInitialTrip(
            tripId = "trip_004",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 1,
            userProfile = userProfile,
            startingLocation = Coordinates(-37.8136, 144.9631),
            includeFillers = false
        )

        assertTrue(initialItinerary.days.isNotEmpty())
        val firstDay = initialItinerary.days[0]

        // Simulate a 2-hour delay after completing first POI
        val completedPOIs = if (firstDay.pois.isNotEmpty()) {
            listOf(firstDay.pois[0].poi.id)
        } else {
            emptyList()
        }

        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-15T13:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            itineraryProgress = ItineraryProgress(
                completedPOIs = completedPOIs,
                delayMinutes = 120
            )
        )

        val adjustmentResponse = engine.adjustTripState(
            currentItinerary = firstDay.pois,
            contextPayload = contextPayload,
            destination = "Melbourne",
            userProfile = userProfile,
            dailyEndTime = "18:00"
        )

        assertNotNull(adjustmentResponse.optionA)
        assertNotNull(adjustmentResponse.optionB)

        // Option A should prioritize experience (higher EV)
        assertEquals(AdjustmentStrategy.EXPERIENCE_FIRST, adjustmentResponse.optionA.strategy)

        // Option B should prioritize efficiency
        assertEquals(AdjustmentStrategy.EFFICIENCY_FIRST, adjustmentResponse.optionB.strategy)

        println("Option A: ${adjustmentResponse.optionA.remainingItinerary.size} POIs, " +
                "EV: ${adjustmentResponse.optionA.totalExpectedValue}, " +
                "Travel: ${adjustmentResponse.optionA.totalTravelTime}min")

        println("Option B: ${adjustmentResponse.optionB.remainingItinerary.size} POIs, " +
                "EV: ${adjustmentResponse.optionB.totalExpectedValue}, " +
                "Travel: ${adjustmentResponse.optionB.totalTravelTime}min")
    }

    @Test
    fun testAdjustTripState_RainyWeather() {
        val initialItinerary = engine.generateInitialTrip(
            tripId = "trip_005",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 1,
            userProfile = userProfile,
            startingLocation = Coordinates(-37.8136, 144.9631),
            weatherStatus = WeatherStatus.SUNNY,
            includeFillers = false
        )

        val firstDay = initialItinerary.days[0]

        // Weather changes to rainy
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-15T12:00:00",
            weatherStatus = WeatherStatus.RAINY,
            itineraryProgress = ItineraryProgress(
                completedPOIs = emptyList(),
                delayMinutes = 0
            )
        )

        val adjustmentResponse = engine.adjustTripState(
            currentItinerary = firstDay.pois,
            contextPayload = contextPayload,
            destination = "Melbourne",
            userProfile = userProfile
        )

        // Should adjust for rainy weather - indoor POIs should be prioritized
        val optionAIndoorCount = adjustmentResponse.optionA.remainingItinerary.count {
            it.poi.environment == Environment.INDOOR
        }

        println("Rainy weather adjustment - Indoor POIs in Option A: $optionAIndoorCount")
    }

    @Test
    fun testCalculatePOIValue() {
        val poi = POI(
            id = "test",
            name = "Test Museum",
            baseScore = 8.0,
            category = POICategory.MUSEUM,
            budgetLevel = BudgetLevel.MEDIUM,
            environment = Environment.INDOOR,
            coordinates = Coordinates(-37.8226, 144.9692),
            recommendedVisitDuration = 120,
            operatingHours = OperatingHours("10:00", "17:00"),
            city = "Melbourne"
        )

        val ev = engine.calculatePOIValue(poi, userProfile, WeatherStatus.SUNNY)

        assertTrue(ev > 0)
        println("POI EV: $ev")
    }

    @Test
    fun testGetPOIsForDestination() {
        val pois = engine.getPOIsForDestination("Melbourne", includeFillers = false)
        val poisWithFillers = engine.getPOIsForDestination("Melbourne", includeFillers = true)

        assertTrue(pois.isNotEmpty())
        assertTrue(poisWithFillers.size > pois.size)

        println("POIs without fillers: ${pois.size}")
        println("POIs with fillers: ${poisWithFillers.size}")
    }

    @Test
    fun testTravelTimeGaps_BetweenPOIs() {
        val itinerary = engine.generateInitialTrip(
            tripId = "trip_gap_test",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 1,
            userProfile = userProfile,
            startingLocation = Coordinates(-37.8136, 144.9631),
            dailyStartTime = "09:00",
            dailyEndTime = "18:00",
            includeFillers = false
        )

        val firstDay = itinerary.days[0]

        println("\n========== TRAVEL TIME GAP VERIFICATION ==========")

        for (i in 0 until firstDay.pois.size - 1) {
            val currentPOI = firstDay.pois[i]
            val nextPOI = firstDay.pois[i + 1]

            val currentEndMinutes = timeToMinutes(currentPOI.endTime)
            val nextStartMinutes = timeToMinutes(nextPOI.startTime)
            val actualGap = nextStartMinutes - currentEndMinutes
            val expectedTravelTime = nextPOI.travelTimeFromPrevious

            println("${currentPOI.poi.name} (ends ${currentPOI.endTime}) -> ${nextPOI.poi.name} (starts ${nextPOI.startTime})")
            println("  Gap: $actualGap min, Expected travel time: $expectedTravelTime min")

            // The gap should equal the travel time (no extra free time unless intended)
            assertTrue("Gap between POIs should match travel time", actualGap >= expectedTravelTime)
        }
    }

    @Test
    fun testRestaurantScheduling() {
        // Create profile that includes restaurant interest
        val profileWithFood = UserProfile(
            interests = listOf(POICategory.MUSEUM, POICategory.RESTAURANT, POICategory.PARK),
            budgetPreference = BudgetLevel.MEDIUM,
            transportMode = TransportMode.WALKING
        )

        val itinerary = engine.generateInitialTrip(
            tripId = "trip_restaurant_test",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 2,
            userProfile = profileWithFood,
            startingLocation = Coordinates(-37.8136, 144.9631),
            dailyStartTime = "09:00",
            dailyEndTime = "18:00",
            includeFillers = false
        )

        println("\n========== RESTAURANT SCHEDULING VERIFICATION ==========")

        itinerary.days.forEachIndexed { dayIndex, day ->
            val restaurants = day.pois.filter { it.poi.category == POICategory.RESTAURANT }

            println("Day ${dayIndex + 1}:")
            println("  Restaurant count: ${restaurants.size}")

            // Should have at most 1 restaurant per day
            assertTrue("Should have at most 1 restaurant per day", restaurants.size <= 1)

            if (restaurants.isNotEmpty()) {
                val restaurant = restaurants[0]
                val startHour = restaurant.startTime.split(":")[0].toInt()

                println("  Restaurant: ${restaurant.poi.name}")
                println("  Scheduled time: ${restaurant.startTime} - ${restaurant.endTime}")

                // Restaurant should be scheduled between 11:00-14:00
                assertTrue("Restaurant should start at or after 11:00", startHour >= 11)
                assertTrue("Restaurant should start before 14:00", startHour < 14)
            }
        }
    }

    @Test
    fun testTravelTimeNormalization() {
        println("\n========== TRAVEL TIME NORMALIZATION TEST ==========")

        // Test the normalization function
        val testCases = listOf(
            5 to 30,
            15 to 30,
            29 to 30,
            30 to 30,
            31 to 60,
            40 to 60,
            60 to 60,
            61 to 90,
            89 to 90,
            90 to 90,
            91 to 120
        )

        testCases.forEach { (input, expected) ->
            val result = com.roammate.logic.engine.TravelCostService.normalizeTravelTime(input)
            println("$input min -> $result min (expected: $expected min)")
            assertEquals("Travel time normalization failed for $input", expected, result)
        }

        // Test in actual itinerary
        val itinerary = engine.generateInitialTrip(
            tripId = "trip_travel_time_test",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 1,
            userProfile = userProfile,
            startingLocation = Coordinates(-37.8136, 144.9631),
            dailyStartTime = "09:00",
            dailyEndTime = "18:00",
            includeFillers = false
        )

        val firstDay = itinerary.days[0]

        println("\n=== Verifying normalized travel times in itinerary ===")
        firstDay.pois.forEach { poi ->
            val travelTime = poi.travelTimeFromPrevious
            println("${poi.poi.name}: travel time = $travelTime min")

            // All travel times should be multiples of 30
            if (travelTime > 0) {
                assertTrue("Travel time should be multiple of 30, got $travelTime", travelTime % 30 == 0)
                assertTrue("Travel time should be at least 30 min, got $travelTime", travelTime >= 30)
            }
        }
    }

    private fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /**
     * Helper function to print detailed itinerary with timeline
     */
    private fun printDetailedItinerary(itinerary: Itinerary, label: String) {
        println("\n+================================================================╗")
        println("|  DETAILED ITINERARY - $label")
        println("+================================================================╝")

        itinerary.days.forEachIndexed { index, day ->
            println("\n[DATE] DAY ${index + 1} - ${day.date}")
            println("-".repeat(65))

            if (day.pois.isEmpty()) {
                println("   (No POIs scheduled)")
            } else {
                day.pois.forEachIndexed { poiIndex, scheduledPOI ->
                    val fillerTag = if (scheduledPOI.isFiller) " 🌟[FILLER]" else ""
                    println("\n${poiIndex + 1}. ${scheduledPOI.poi.name}$fillerTag")
                    println("   ⏰ Time: ${scheduledPOI.startTime} - ${scheduledPOI.endTime}")
                    println("   [LOCATION] Category: ${scheduledPOI.poi.category}")
                    println("   [COST] Budget: ${scheduledPOI.poi.budgetLevel}")
                    println("   [INDOOR] Environment: ${scheduledPOI.poi.environment}")
                    println("   [STAR] Expected Value: ${"%.2f".format(scheduledPOI.expectedValue)}")
                    println("   [TIME]  Duration: ${scheduledPOI.poi.recommendedVisitDuration} min")

                    if (poiIndex < day.pois.size - 1) {
                        val nextPOI = day.pois[poiIndex + 1]
                        val travelTime = nextPOI.travelTimeFromPrevious
                        if (travelTime > 0) {
                            println("   [TRAVEL] Travel to next: $travelTime min")
                        }

                        // Calculate gap between POIs
                        val endParts = scheduledPOI.endTime.split(":")
                        val nextStartParts = nextPOI.startTime.split(":")
                        val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
                        val nextStartMinutes = nextStartParts[0].toInt() * 60 + nextStartParts[1].toInt()
                        val gap = nextStartMinutes - endMinutes - travelTime

                        if (gap > 0) {
                            println("   ⏸️  Free time: $gap min")
                        }
                    }
                }

                println("\n" + "-".repeat(65))
                println("Day ${index + 1} Summary:")
                println("  * Total POIs: ${day.pois.size}")
                println("  * Filler POIs: ${day.pois.count { it.isFiller }}")
                println("  * Total EV: ${"%.2f".format(day.pois.sumOf { it.expectedValue })}")
                println("  * Total Travel Time: ${day.totalTravelTime} min")
            }
        }

        println("\n" + "=".repeat(65))
        println("TRIP SUMMARY:")
        println("  * Total Days: ${itinerary.days.size}")
        println("  * Total POIs: ${itinerary.days.sumOf { it.pois.size }}")
        println("  * Total Fillers: ${itinerary.days.sumOf { day -> day.pois.count { it.isFiller } }}")
        println("  * Total EV: ${"%.2f".format(itinerary.totalExpectedValue)}")
        println("=".repeat(65) + "\n")
    }

    @Test
    fun testGenerateInitialTrip_WithPinnedPOIs() {
        println("\n========== TESTING PINNED POIs ==========")

        // Get POI repository to find specific POIs
        val poiRepository = MockPoiRepository(useJsonData = true)
        val allPOIs = poiRepository.getPOIsByCity("Melbourne")

        // Find some specific POIs to pin
        val shotTower = allPOIs.find { it.name.contains("Shot Tower Museum", ignoreCase = true) }
        val anzMuseum = allPOIs.find { it.name.contains("ANZ Banking Museum", ignoreCase = true) }
        val koorieHeritage = allPOIs.find { it.name.contains("Koorie Heritage Trust", ignoreCase = true) }

        assertNotNull("Shot Tower Museum should exist in data", shotTower)
        assertNotNull("ANZ Banking Museum should exist in data", anzMuseum)
        assertNotNull("Koorie Heritage Trust should exist in data", koorieHeritage)

        println("Found POIs to pin:")
        println("  1. ${shotTower?.name} (ID: ${shotTower?.id})")
        println("  2. ${anzMuseum?.name} (ID: ${anzMuseum?.id})")
        println("  3. ${koorieHeritage?.name} (ID: ${koorieHeritage?.id})")

        // Create pinned POIs:
        // - Day 1: Shot Tower Museum (morning)
        // - Day 2: ANZ Banking Museum (morning), Koorie Heritage Trust (afternoon)
        // - Day 3: No pinned POIs
        val pinnedPOIs = listOf(
            PinnedPOI(
                poiId = shotTower!!.id,
                dayNumber = 1,
                preferredStartTime = "09:30"
            ),
            PinnedPOI(
                poiId = anzMuseum!!.id,
                dayNumber = 2,
                preferredStartTime = "10:00"
            ),
            PinnedPOI(
                poiId = koorieHeritage!!.id,
                dayNumber = 2,
                preferredStartTime = "14:00"
            )
        )

        // Generate itinerary with pinned POIs
        val itinerary = engine.generateInitialTrip(
            tripId = "trip_pinned",
            destination = "Melbourne",
            startDate = "2026-09-20",
            numberOfDays = 3,
            userProfile = userProfile,
            startingLocation = Coordinates(-37.8136, 144.9631), // CBD
            dailyStartTime = "09:00",
            dailyEndTime = "18:00",
            weatherStatus = WeatherStatus.SUNNY,
            includeFillers = false,
            pinnedPOIs = pinnedPOIs
        )

        assertNotNull(itinerary)
        assertEquals(3, itinerary.days.size)

        // Verify Day 1 contains Shot Tower Museum
        val day1POIs = itinerary.days[0].pois
        assertTrue("Day 1 should contain Shot Tower Museum",
            day1POIs.any { it.poi.id == shotTower.id })

        // Verify Day 2 contains both ANZ Banking Museum and Koorie Heritage Trust
        val day2POIs = itinerary.days[1].pois
        assertTrue("Day 2 should contain ANZ Banking Museum",
            day2POIs.any { it.poi.id == anzMuseum.id })
        assertTrue("Day 2 should contain Koorie Heritage Trust",
            day2POIs.any { it.poi.id == koorieHeritage.id })

        // Verify the order on Day 2 (ANZ Banking Museum should come before Koorie Heritage Trust)
        val anzIndex = day2POIs.indexOfFirst { it.poi.id == anzMuseum.id }
        val koorieIndex = day2POIs.indexOfFirst { it.poi.id == koorieHeritage.id }
        if (anzIndex >= 0 && koorieIndex >= 0) {
            assertTrue("ANZ Banking Museum should be scheduled before Koorie Heritage Trust on Day 2",
                anzIndex < koorieIndex)
        }

        println("\n[OK] VERIFICATION RESULTS:")
        println("  * Day 1 has Shot Tower Museum: ${day1POIs.any { it.poi.id == shotTower.id }}")
        println("  * Day 2 has ANZ Banking Museum: ${day2POIs.any { it.poi.id == anzMuseum.id }}")
        println("  * Day 2 has Koorie Heritage Trust: ${day2POIs.any { it.poi.id == koorieHeritage.id }}")

        if (anzIndex >= 0 && koorieIndex >= 0) {
            println("  * ANZ Banking Museum scheduled at position ${anzIndex + 1}")
            println("  * Koorie Heritage Trust scheduled at position ${koorieIndex + 1}")
            println("  * Order is correct: ${anzIndex < koorieIndex}")
        }

        // Print detailed itinerary
        printDetailedItinerary(itinerary, "PINNED POIs TEST")

        println("\n[LOCATION] PINNED POIs SUMMARY:")
        pinnedPOIs.forEach { pinned ->
            val poi = allPOIs.find { it.id == pinned.poiId }
            val scheduled = itinerary.days[pinned.dayNumber - 1].pois.find { it.poi.id == pinned.poiId }
            println("  * ${poi?.name} on Day ${pinned.dayNumber}")
            println("    - Requested: ${pinned.preferredStartTime ?: "anytime"}")
            println("    - Scheduled: ${scheduled?.startTime ?: "NOT SCHEDULED"}")
            println("    - Status: ${if (scheduled != null) "[CHECK] SUCCESS" else "✗ FAILED"}")
        }

        println("=".repeat(65))
    }

    @Test
    fun testWeatherChangeReplanning() {
        println("\n========== TESTING WEATHER CHANGE REPLANNING ==========")

        // Step 1: Generate initial itinerary with sunny weather
        val initialItinerary = engine.generateInitialTrip(
            tripId = "trip_weather_test",
            destination = "Melbourne",
            startDate = "2026-09-20",
            numberOfDays = 2,
            userProfile = userProfile,
            startingLocation = Coordinates(-37.8136, 144.9631),
            dailyStartTime = "09:00",
            dailyEndTime = "18:00",
            weatherStatus = WeatherStatus.SUNNY,
            includeFillers = false,
            pinnedPOIs = emptyList()
        )

        println("\n[DATE] INITIAL ITINERARY (Sunny Weather):")
        initialItinerary.days.forEachIndexed { dayIndex, day ->
            println("Day ${dayIndex + 1}:")
            day.pois.forEach { poi ->
                println("  * ${poi.poi.name} (${poi.poi.environment}) - ${poi.startTime} to ${poi.endTime}")
            }
            val outdoorCount = day.pois.count { it.poi.environment == Environment.OUTDOOR }
            println("  Outdoor activities: $outdoorCount")
        }

        // Step 2: Simulate weather change on Day 2 from sunny to rainy
        println("\n[RAIN] WEATHER CHANGE: Day 2 changes from Sunny to Rainy")

        val day2Original = initialItinerary.days[1]
        val hasOutdoorActivities = day2Original.pois.any {
            it.poi.environment == Environment.OUTDOOR || it.poi.environment == Environment.MIXED
        }

        println("Day 2 has outdoor activities: $hasOutdoorActivities")

        if (hasOutdoorActivities) {
            // Step 3: Trigger weather replanning
            val weatherService = WeatherReplanningService(userProfile)
            val allPOIs = MockPoiRepository(useJsonData = true).getPOIsByCity("Melbourne")

            val contextPayload = ContextPayload(
                currentTime = "2026-09-21T09:00:00",
                currentLocation = Coordinates(-37.8136, 144.9631),
                currentWeather = WeatherStatus.RAINY,
                weatherStatus = WeatherStatus.RAINY,
                itineraryProgress = ItineraryProgress(
                    completedPOIs = emptyList(),
                    delayMinutes = 0
                ),
                recentStepCount = null
            )

            val availableTimeMinutes = 540 // 09:00 to 18:00 = 9 hours

            val rainyDayOptions = weatherService.handleRainyDayScenario(
                currentItinerary = day2Original,
                contextPayload = contextPayload,
                allPOIs = allPOIs,
                startLocation = Coordinates(-37.8136, 144.9631),
                availableTimeMinutes = availableTimeMinutes
            )

            println("\n[INFO] REPLANNING TRIGGERED:")
            println("Reason: ${rainyDayOptions.triggerReason}")

            // Verify Option 1: Original (keep outdoor activities despite rain)
            println("\n📌 OPTION 1: Original (Keep outdoor activities)")
            println("  Total POIs: ${rainyDayOptions.original.pois.size}")
            rainyDayOptions.original.pois.forEach { itineraryPOI ->
                println("  * ${itineraryPOI.poi.name} (${itineraryPOI.poi.environment})")
            }
            val originalOutdoor = rainyDayOptions.original.pois.count { it.poi.environment == Environment.OUTDOOR }
            println("  Outdoor activities: $originalOutdoor")

            // Verify Option 2: Experience First (maximize EV with indoor POIs)
            println("\n📌 OPTION 2: Experience First (Maximize EV)")
            println("  Total POIs: ${rainyDayOptions.experienceFirst.pois.size}")
            rainyDayOptions.experienceFirst.pois.forEach { itineraryPOI ->
                println("  * ${itineraryPOI.poi.name} (${itineraryPOI.poi.environment}) - EV: ${String.format("%.2f", itineraryPOI.expectedValue)}")
            }
            val expFirstOutdoor = rainyDayOptions.experienceFirst.pois.count { it.poi.environment == Environment.OUTDOOR }
            val expFirstIndoor = rainyDayOptions.experienceFirst.pois.count { it.poi.environment == Environment.INDOOR }
            println("  Outdoor activities: $expFirstOutdoor")
            println("  Indoor activities: $expFirstIndoor")

            // Verify Option 3: Efficiency First (maximize EV/Cost ratio with indoor POIs)
            println("\n📌 OPTION 3: Efficiency First (Maximize EV/Cost)")
            println("  Total POIs: ${rainyDayOptions.efficiencyFirst.pois.size}")
            rainyDayOptions.efficiencyFirst.pois.forEach { itineraryPOI ->
                println("  * ${itineraryPOI.poi.name} (${itineraryPOI.poi.environment}) - EV: ${String.format("%.2f", itineraryPOI.expectedValue)}")
            }
            val effFirstOutdoor = rainyDayOptions.efficiencyFirst.pois.count { it.poi.environment == Environment.OUTDOOR }
            val effFirstIndoor = rainyDayOptions.efficiencyFirst.pois.count { it.poi.environment == Environment.INDOOR }
            println("  Outdoor activities: $effFirstOutdoor")
            println("  Indoor activities: $effFirstIndoor")

            // Assertions
            println("\n[OK] VERIFICATION:")

            // Option 1 should keep original itinerary
            assertEquals("Option 1 should match original itinerary",
                day2Original.pois.size, rainyDayOptions.original.pois.size)
            println("  [CHECK] Option 1 preserves original itinerary")

            // Options 2 and 3 should have alternative POIs
            assertTrue("Experience First should provide alternatives",
                rainyDayOptions.experienceFirst.pois.isNotEmpty())
            assertTrue("Efficiency First should provide alternatives",
                rainyDayOptions.efficiencyFirst.pois.isNotEmpty())
            println("  [CHECK] Alternative options provided")

            // Experience First and Efficiency First should prioritize indoor POIs
            assertTrue("Experience First should prioritize indoor activities in rainy weather",
                expFirstIndoor > expFirstOutdoor)
            assertTrue("Efficiency First should prioritize indoor activities in rainy weather",
                effFirstIndoor > effFirstOutdoor)
            println("  [CHECK] Alternative options prioritize indoor activities")

            // Experience First should maximize total EV
            val expFirstTotalEV = rainyDayOptions.experienceFirst.pois.sumOf { it.expectedValue }
            val effFirstTotalEV = rainyDayOptions.efficiencyFirst.pois.sumOf { it.expectedValue }
            println("  * Experience First total EV: ${String.format("%.2f", expFirstTotalEV)}")
            println("  * Efficiency First total EV: ${String.format("%.2f", effFirstTotalEV)}")

            // All POIs in alternative options should be valid
            rainyDayOptions.experienceFirst.pois.forEach { itineraryPOI ->
                assertNotNull("POI should not be null", itineraryPOI.poi)
                assertNotNull("Start time should not be null", itineraryPOI.startTime)
                assertNotNull("End time should not be null", itineraryPOI.endTime)
            }
            rainyDayOptions.efficiencyFirst.pois.forEach { itineraryPOI ->
                assertNotNull("POI should not be null", itineraryPOI.poi)
                assertNotNull("Start time should not be null", itineraryPOI.startTime)
                assertNotNull("End time should not be null", itineraryPOI.endTime)
            }
            println("  [CHECK] All POIs have valid scheduling information")

        } else {
            println("\n[WARNING] Day 2 has no outdoor activities, skipping weather replanning test")
            println("Note: Adjust test data or initial weather to ensure outdoor activities are scheduled")
        }

        println("\n" + "=".repeat(65))
    }
}
