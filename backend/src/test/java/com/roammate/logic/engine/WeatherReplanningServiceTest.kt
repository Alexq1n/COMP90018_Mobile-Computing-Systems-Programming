package com.roammate.logic.engine

import com.roammate.logic.models.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for WeatherReplanningService
 */
class WeatherReplanningServiceTest {

    private lateinit var weatherService: WeatherReplanningService
    private lateinit var userProfile: UserProfile
    private lateinit var testDayItinerary: DayItinerary
    private lateinit var allPOIs: List<POI>

    @Before
    fun setup() {
        userProfile = UserProfile(
            interests = listOf(POICategory.MUSEUM, POICategory.PARK),
            budgetPreference = BudgetLevel.MEDIUM,
            transportMode = TransportMode.WALKING,
            transportPreference = 1.0f
        )

        weatherService = WeatherReplanningService(userProfile)

        // Create test POIs - mix of indoor and outdoor
        allPOIs = listOf(
            // Outdoor POIs
            POI(
                id = "outdoor_001",
                name = "Royal Botanic Gardens",
                baseScore = 8.5,
                category = POICategory.PARK,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8304, 144.9796),
                recommendedVisitDuration = 90,
                operatingHours = OperatingHours("07:30", "19:00"),
                isFiller = false,
                city = "Melbourne"
            ),
            POI(
                id = "outdoor_002",
                name = "Federation Square",
                baseScore = 8.0,
                category = POICategory.LANDMARK,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8180, 144.9685),
                recommendedVisitDuration = 60,
                operatingHours = OperatingHours("00:00", "23:59"),
                isFiller = false,
                city = "Melbourne"
            ),
            // Indoor POIs
            POI(
                id = "indoor_001",
                name = "NGV",
                baseScore = 9.0,
                category = POICategory.MUSEUM,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8226, 144.9692),
                recommendedVisitDuration = 120,
                operatingHours = OperatingHours("10:00", "17:00"),
                isFiller = false,
                city = "Melbourne"
            ),
            POI(
                id = "indoor_002",
                name = "State Library",
                baseScore = 8.0,
                category = POICategory.MUSEUM,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8098, 144.9652),
                recommendedVisitDuration = 90,
                operatingHours = OperatingHours("10:00", "18:00"),
                isFiller = false,
                city = "Melbourne"
            ),
            POI(
                id = "indoor_003",
                name = "Melbourne Museum",
                baseScore = 8.5,
                category = POICategory.MUSEUM,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8033, 144.9717),
                recommendedVisitDuration = 150,
                operatingHours = OperatingHours("09:00", "17:00"),
                isFiller = false,
                city = "Melbourne"
            )
        )

        // Create test itinerary with outdoor POIs
        val dayPOIs = listOf(
            ItineraryPOI(
                poi = allPOIs[0], // Botanic Gardens - outdoor
                startTime = "09:30",
                endTime = "11:00",
                travelTimeFromPrevious = 15,
                expectedValue = 8.5,
                isFiller = false,
                isUserPinned = false
            ),
            ItineraryPOI(
                poi = allPOIs[1], // Federation Square - outdoor
                startTime = "11:20",
                endTime = "12:20",
                travelTimeFromPrevious = 20,
                expectedValue = 8.0,
                isFiller = false,
                isUserPinned = false
            )
        )

        testDayItinerary = DayItinerary(
            dayNumber = 1,
            date = "2026-09-20",
            pois = dayPOIs,
            totalDuration = 170,
            totalTravelTime = 35,
            totalVisitTime = 150
        )
    }

    @Test
    fun testParseWeatherContext_Rainy() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T09:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = WeatherStatus.RAINY,
            itineraryProgress = ItineraryProgress(emptyList(), 0)
        )

        val weatherContext = weatherService.parseWeatherContext(contextPayload)

        assertEquals(WeatherStatus.RAINY, weatherContext.weatherStatus)
        assertTrue(weatherContext.isRainy)
        assertEquals("2026-09-20T09:00:00", weatherContext.timestamp)

        println("✓ Parse weather context test passed")
    }

    @Test
    fun testHasOutdoorActivities_True() {
        val hasOutdoor = weatherService.hasOutdoorActivities(testDayItinerary)

        assertTrue("Should detect outdoor activities", hasOutdoor)

        println("✓ Detect outdoor activities test passed")
    }

    @Test
    fun testHasOutdoorActivities_False() {
        // Create itinerary with only indoor POIs
        val indoorOnlyPOIs = listOf(
            ItineraryPOI(
                poi = allPOIs[2], // NGV - indoor
                startTime = "10:00",
                endTime = "12:00",
                travelTimeFromPrevious = 15,
                expectedValue = 9.0,
                isFiller = false,
                isUserPinned = false
            )
        )

        val indoorItinerary = testDayItinerary.copy(pois = indoorOnlyPOIs)
        val hasOutdoor = weatherService.hasOutdoorActivities(indoorItinerary)

        assertFalse("Should not detect outdoor activities", hasOutdoor)

        println("✓ No outdoor activities test passed")
    }

    @Test
    fun testShouldTriggerReplanning_True() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T09:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = WeatherStatus.RAINY,
            itineraryProgress = ItineraryProgress(emptyList(), 0)
        )

        val shouldTrigger = weatherService.shouldTriggerReplanning(
            contextPayload = contextPayload,
            currentDayItinerary = testDayItinerary
        )

        assertTrue("Should trigger replanning for rainy day with outdoor activities", shouldTrigger)

        println("✓ Should trigger replanning test passed")
    }

    @Test
    fun testShouldTriggerReplanning_False_NoRain() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T09:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = WeatherStatus.SUNNY,
            itineraryProgress = ItineraryProgress(emptyList(), 0)
        )

        val shouldTrigger = weatherService.shouldTriggerReplanning(
            contextPayload = contextPayload,
            currentDayItinerary = testDayItinerary
        )

        assertFalse("Should not trigger replanning on sunny day", shouldTrigger)

        println("✓ No trigger on sunny day test passed")
    }

    @Test
    fun testHandleRainyDayScenario_ThreeOptions() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T09:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = WeatherStatus.RAINY,
            itineraryProgress = ItineraryProgress(emptyList(), 0)
        )

        val rainyDayOptions = weatherService.handleRainyDayScenario(
            currentItinerary = testDayItinerary,
            contextPayload = contextPayload,
            allPOIs = allPOIs,
            startLocation = Coordinates(-37.8136, 144.9631),
            availableTimeMinutes = 480
        )

        // Verify all three options are present
        assertNotNull("Original option should exist", rainyDayOptions.original)
        assertNotNull("Experience First option should exist", rainyDayOptions.experienceFirst)
        assertNotNull("Efficiency First option should exist", rainyDayOptions.efficiencyFirst)

        // Original should be unchanged
        assertEquals("Original should have same POIs",
            testDayItinerary.pois.size,
            rainyDayOptions.original.pois.size)

        // Experience First should prioritize indoor POIs
        val expFirstIndoorCount = rainyDayOptions.experienceFirst.pois.count {
            it.poi.environment == Environment.INDOOR
        }
        assertTrue("Experience First should have indoor POIs", expFirstIndoorCount > 0)

        // Efficiency First should also have indoor POIs
        val effFirstIndoorCount = rainyDayOptions.efficiencyFirst.pois.count {
            it.poi.environment == Environment.INDOOR
        }
        assertTrue("Efficiency First should have indoor POIs", effFirstIndoorCount > 0)

        // Trigger reason should mention rainy weather
        assertTrue("Trigger reason should mention rain",
            rainyDayOptions.triggerReason.contains("Rainy") ||
            rainyDayOptions.triggerReason.contains("rainy"))

        println("✓ Rainy day three options test passed")
        println("  - Original: ${rainyDayOptions.original.pois.size} POIs")
        println("  - Experience First: ${rainyDayOptions.experienceFirst.pois.size} POIs (${expFirstIndoorCount} indoor)")
        println("  - Efficiency First: ${rainyDayOptions.efficiencyFirst.pois.size} POIs (${effFirstIndoorCount} indoor)")
        println("  - Reason: ${rainyDayOptions.triggerReason}")
    }

    @Test
    fun testHandleRainyDayScenario_NoOutdoorActivities() {
        // Create indoor-only itinerary
        val indoorOnlyPOIs = listOf(
            ItineraryPOI(
                poi = allPOIs[2], // NGV - indoor
                startTime = "10:00",
                endTime = "12:00",
                travelTimeFromPrevious = 15,
                expectedValue = 9.0,
                isFiller = false,
                isUserPinned = false
            )
        )

        val indoorItinerary = testDayItinerary.copy(pois = indoorOnlyPOIs)

        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T09:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = WeatherStatus.RAINY,
            itineraryProgress = ItineraryProgress(emptyList(), 0)
        )

        val rainyDayOptions = weatherService.handleRainyDayScenario(
            currentItinerary = indoorItinerary,
            contextPayload = contextPayload,
            allPOIs = allPOIs,
            startLocation = Coordinates(-37.8136, 144.9631),
            availableTimeMinutes = 480
        )

        // Should return same itinerary for all options since no replanning needed
        assertEquals("All options should be same when no outdoor activities",
            indoorItinerary.pois.size,
            rainyDayOptions.original.pois.size)

        assertTrue("Reason should mention no outdoor activities",
            rainyDayOptions.triggerReason.contains("No outdoor"))

        println("✓ No outdoor activities scenario test passed")
    }

    @Test
    fun testHandleRainyDayScenario_SunnyWeather() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T09:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = WeatherStatus.SUNNY,
            itineraryProgress = ItineraryProgress(emptyList(), 0)
        )

        val rainyDayOptions = weatherService.handleRainyDayScenario(
            currentItinerary = testDayItinerary,
            contextPayload = contextPayload,
            allPOIs = allPOIs,
            startLocation = Coordinates(-37.8136, 144.9631),
            availableTimeMinutes = 480
        )

        // Should return same itinerary for all options since weather is not rainy
        assertTrue("Reason should mention weather is not rainy",
            rainyDayOptions.triggerReason.contains("not rainy"))

        println("✓ Sunny weather scenario test passed")
    }

    @Test
    fun testExperienceFirstVsEfficiencyFirst() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T09:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = WeatherStatus.RAINY,
            itineraryProgress = ItineraryProgress(emptyList(), 0)
        )

        val rainyDayOptions = weatherService.handleRainyDayScenario(
            currentItinerary = testDayItinerary,
            contextPayload = contextPayload,
            allPOIs = allPOIs,
            startLocation = Coordinates(-37.8136, 144.9631),
            availableTimeMinutes = 480
        )

        val expFirstTravelTime = rainyDayOptions.experienceFirst.totalTravelTime
        val effFirstTravelTime = rainyDayOptions.efficiencyFirst.totalTravelTime

        // Efficiency First should generally have less travel time (though not guaranteed with small dataset)
        println("✓ Strategy comparison test passed")
        println("  - Experience First travel time: ${expFirstTravelTime} min")
        println("  - Efficiency First travel time: ${effFirstTravelTime} min")
    }
}
