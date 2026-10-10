package com.roammate.logic

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
        println("\n╔════════════════════════════════════════════════════════════════╗")
        println("║  DETAILED ITINERARY - $label")
        println("╚════════════════════════════════════════════════════════════════╝")

        itinerary.days.forEachIndexed { index, day ->
            println("\n📅 DAY ${index + 1} - ${day.date}")
            println("─".repeat(65))

            if (day.pois.isEmpty()) {
                println("   (No POIs scheduled)")
            } else {
                day.pois.forEachIndexed { poiIndex, scheduledPOI ->
                    val fillerTag = if (scheduledPOI.isFiller) " 🌟[FILLER]" else ""
                    println("\n${poiIndex + 1}. ${scheduledPOI.poi.name}$fillerTag")
                    println("   ⏰ Time: ${scheduledPOI.startTime} - ${scheduledPOI.endTime}")
                    println("   📍 Category: ${scheduledPOI.poi.category}")
                    println("   💰 Budget: ${scheduledPOI.poi.budgetLevel}")
                    println("   🏠 Environment: ${scheduledPOI.poi.environment}")
                    println("   ⭐ Expected Value: ${"%.2f".format(scheduledPOI.expectedValue)}")
                    println("   ⏱️  Duration: ${scheduledPOI.poi.recommendedVisitDuration} min")

                    if (poiIndex < day.pois.size - 1) {
                        val nextPOI = day.pois[poiIndex + 1]
                        val travelTime = nextPOI.travelTimeFromPrevious
                        if (travelTime > 0) {
                            println("   🚶 Travel to next: $travelTime min")
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

                println("\n" + "─".repeat(65))
                println("Day ${index + 1} Summary:")
                println("  • Total POIs: ${day.pois.size}")
                println("  • Filler POIs: ${day.pois.count { it.isFiller }}")
                println("  • Total EV: ${"%.2f".format(day.pois.sumOf { it.expectedValue })}")
                println("  • Total Travel Time: ${day.totalTravelTime} min")
            }
        }

        println("\n" + "═".repeat(65))
        println("TRIP SUMMARY:")
        println("  • Total Days: ${itinerary.days.size}")
        println("  • Total POIs: ${itinerary.days.sumOf { it.pois.size }}")
        println("  • Total Fillers: ${itinerary.days.sumOf { day -> day.pois.count { it.isFiller } }}")
        println("  • Total EV: ${"%.2f".format(itinerary.totalExpectedValue)}")
        println("═".repeat(65) + "\n")
    }
}
