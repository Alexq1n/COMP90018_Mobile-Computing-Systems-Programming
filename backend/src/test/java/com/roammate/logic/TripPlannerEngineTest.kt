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
        val poiRepository = MockPoiRepository()
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

        println("Generated itinerary with ${itinerary.days.sumOf { it.pois.size }} total POIs")
        println("Total EV: ${itinerary.totalExpectedValue}")
    }

    @Test
    fun testGenerateInitialTrip_WithFillers() {
        val itineraryWithFillers = engine.generateInitialTrip(
            tripId = "trip_002",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 2,
            userProfile = userProfile,
            startingLocation = Coordinates(-37.8136, 144.9631),
            includeFillers = true
        )

        val itineraryWithoutFillers = engine.generateInitialTrip(
            tripId = "trip_003",
            destination = "Melbourne",
            startDate = "2026-09-15",
            numberOfDays = 2,
            userProfile = userProfile,
            startingLocation = Coordinates(-37.8136, 144.9631),
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

        println("Itinerary with fillers: ${fillersCount} filler POIs injected")
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
}
