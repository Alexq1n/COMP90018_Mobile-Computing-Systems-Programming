package com.roammate.logic.engine

import com.roammate.logic.models.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for FatigueDetectionService
 */
class FatigueDetectionServiceTest {

    private lateinit var fatigueService: FatigueDetectionService
    private lateinit var userProfile: UserProfile
    private lateinit var allPOIs: List<POI>

    @Before
    fun setup() {
        userProfile = UserProfile(
            interests = listOf(POICategory.MUSEUM, POICategory.PARK, POICategory.CAFE),
            budgetPreference = BudgetLevel.MEDIUM,
            transportMode = TransportMode.WALKING,
            transportPreference = 1.0f
        )

        fatigueService = FatigueDetectionService(userProfile)

        // Create test POIs with varying durations
        allPOIs = listOf(
            // Long duration POI
            POI(
                id = "poi_001",
                name = "Melbourne Museum",
                baseScore = 9.0,
                category = POICategory.MUSEUM,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8033, 144.9717),
                recommendedVisitDuration = 150, // 2.5 hours
                operatingHours = OperatingHours("09:00", "17:00"),
                isFiller = false,
                city = "Melbourne"
            ),
            // Medium duration POI
            POI(
                id = "poi_002",
                name = "Royal Botanic Gardens",
                baseScore = 8.5,
                category = POICategory.PARK,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.OUTDOOR,
                coordinates = Coordinates(-37.8304, 144.9796),
                recommendedVisitDuration = 90, // 1.5 hours
                operatingHours = OperatingHours("07:30", "19:00"),
                isFiller = false,
                city = "Melbourne"
            ),
            // Short duration, low-intensity POI
            POI(
                id = "poi_003",
                name = "Cafe Vue",
                baseScore = 7.5,
                category = POICategory.CAFE,
                budgetLevel = BudgetLevel.MEDIUM,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8136, 144.9631),
                recommendedVisitDuration = 45, // 45 minutes
                operatingHours = OperatingHours("07:00", "17:00"),
                isFiller = false,
                city = "Melbourne"
            ),
            // Short visit POI
            POI(
                id = "poi_004",
                name = "St Paul's Cathedral",
                baseScore = 8.0,
                category = POICategory.HISTORICAL,
                budgetLevel = BudgetLevel.LOW,
                environment = Environment.INDOOR,
                coordinates = Coordinates(-37.8170, 144.9676),
                recommendedVisitDuration = 30,
                operatingHours = OperatingHours("08:00", "18:00"),
                isFiller = false,
                city = "Melbourne"
            )
        )
    }

    @Test
    fun testDetectFatigue_Low() {
        val fatigueLevel = fatigueService.detectFatigue(stepCount = 300)

        assertEquals(FatigueLevel.LOW, fatigueLevel)

        println("✓ Low fatigue detection test passed (300 steps)")
    }

    @Test
    fun testDetectFatigue_Medium() {
        val fatigueLevel = fatigueService.detectFatigue(stepCount = 2000)

        assertEquals(FatigueLevel.MEDIUM, fatigueLevel)

        println("✓ Medium fatigue detection test passed (2000 steps)")
    }

    @Test
    fun testDetectFatigue_High() {
        val fatigueLevel = fatigueService.detectFatigue(stepCount = 6000)

        assertEquals(FatigueLevel.HIGH, fatigueLevel)

        println("✓ High fatigue detection test passed (6000 steps)")
    }

    @Test
    fun testAnalyzeFatigueStatus_NoStepData() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T14:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = null,
            itineraryProgress = ItineraryProgress(emptyList(), 0),
            recentStepCount = null // No step data
        )

        val fatigueStatus = fatigueService.analyzeFatigueStatus(contextPayload)

        assertEquals(FatigueLevel.LOW, fatigueStatus.level)
        assertTrue(fatigueStatus.recommendedAction.contains("No step data"))
        assertNull(fatigueStatus.adjustedItinerary)

        println("✓ No step data test passed")
    }

    @Test
    fun testAnalyzeFatigueStatus_LowFatigue() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T14:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = null,
            itineraryProgress = ItineraryProgress(emptyList(), 0),
            recentStepCount = 400
        )

        val fatigueStatus = fatigueService.analyzeFatigueStatus(contextPayload)

        assertEquals(FatigueLevel.LOW, fatigueStatus.level)
        assertTrue(fatigueStatus.recommendedAction.contains("Normal activity"))

        println("✓ Low fatigue analysis test passed")
    }

    @Test
    fun testAnalyzeFatigueStatus_MediumFatigue() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T14:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = null,
            itineraryProgress = ItineraryProgress(emptyList(), 0),
            recentStepCount = 3500
        )

        val fatigueStatus = fatigueService.analyzeFatigueStatus(contextPayload)

        assertEquals(FatigueLevel.MEDIUM, fatigueStatus.level)
        assertTrue(fatigueStatus.recommendedAction.contains("Moderate activity"))

        println("✓ Medium fatigue analysis test passed")
    }

    @Test
    fun testAnalyzeFatigueStatus_HighFatigue() {
        val contextPayload = ContextPayload(
            currentLocation = Coordinates(-37.8136, 144.9631),
            currentTime = "2026-09-20T14:00:00",
            weatherStatus = WeatherStatus.SUNNY,
            currentWeather = null,
            itineraryProgress = ItineraryProgress(emptyList(), 0),
            recentStepCount = 5500
        )

        val fatigueStatus = fatigueService.analyzeFatigueStatus(contextPayload)

        assertEquals(FatigueLevel.HIGH, fatigueStatus.level)
        assertTrue(fatigueStatus.recommendedAction.contains("High fatigue"))

        println("✓ High fatigue analysis test passed")
    }

    @Test
    fun testAdjustItineraryForFatigue_LowFatigue() {
        val remainingItinerary = emptyList<ItineraryPOI>()

        val adjustedDay = fatigueService.adjustItineraryForFatigue(
            remainingItinerary = remainingItinerary,
            allPOIs = allPOIs,
            fatigueLevel = FatigueLevel.LOW,
            currentLocation = Coordinates(-37.8136, 144.9631),
            availableMinutes = 300,
            weatherStatus = WeatherStatus.SUNNY,
            dayNumber = 1,
            date = "2026-09-20"
        )

        assertNotNull(adjustedDay)
        // Low fatigue should allow normal POIs
        println("✓ Low fatigue adjustment test passed: ${adjustedDay.pois.size} POIs")
    }

    @Test
    fun testAdjustItineraryForFatigue_MediumFatigue() {
        val remainingItinerary = emptyList<ItineraryPOI>()

        val adjustedDay = fatigueService.adjustItineraryForFatigue(
            remainingItinerary = remainingItinerary,
            allPOIs = allPOIs,
            fatigueLevel = FatigueLevel.MEDIUM,
            currentLocation = Coordinates(-37.8136, 144.9631),
            availableMinutes = 300,
            weatherStatus = WeatherStatus.SUNNY,
            dayNumber = 1,
            date = "2026-09-20"
        )

        assertNotNull(adjustedDay)
        // Medium fatigue should limit POI count to max 4
        assertTrue("POI count should be limited", adjustedDay.pois.size <= 4)

        // Should prefer shorter duration POIs
        val longDurationPOIs = adjustedDay.pois.filter { it.poi.recommendedVisitDuration > 90 }
        println("✓ Medium fatigue adjustment test passed: ${adjustedDay.pois.size} POIs, ${longDurationPOIs.size} long-duration")
    }

    @Test
    fun testAdjustItineraryForFatigue_HighFatigue() {
        val remainingItinerary = emptyList<ItineraryPOI>()

        val adjustedDay = fatigueService.adjustItineraryForFatigue(
            remainingItinerary = remainingItinerary,
            allPOIs = allPOIs,
            fatigueLevel = FatigueLevel.HIGH,
            currentLocation = Coordinates(-37.8136, 144.9631),
            availableMinutes = 300,
            weatherStatus = WeatherStatus.SUNNY,
            dayNumber = 1,
            date = "2026-09-20"
        )

        assertNotNull(adjustedDay)
        // High fatigue should limit POI count to max 2
        assertTrue("POI count should be severely limited", adjustedDay.pois.size <= 2)

        // Should only include short-duration POIs (≤60 min)
        val allShortDuration = adjustedDay.pois.all { it.poi.recommendedVisitDuration <= 60 }
        assertTrue("All POIs should be short duration", allShortDuration)

        // Should prefer indoor/cafe/restaurant POIs
        val lowIntensityPOIs = adjustedDay.pois.filter {
            it.poi.environment == Environment.INDOOR ||
            it.poi.category == POICategory.CAFE ||
            it.poi.category == POICategory.RESTAURANT
        }
        assertTrue("Should prioritize low-intensity POIs", lowIntensityPOIs.isNotEmpty())

        println("✓ High fatigue adjustment test passed: ${adjustedDay.pois.size} POIs, all short-duration and low-intensity")
    }

    @Test
    fun testFatigueThresholds() {
        // Test boundary values
        assertEquals(FatigueLevel.LOW, fatigueService.detectFatigue(499))
        assertEquals(FatigueLevel.MEDIUM, fatigueService.detectFatigue(500))
        assertEquals(FatigueLevel.MEDIUM, fatigueService.detectFatigue(4999))
        assertEquals(FatigueLevel.HIGH, fatigueService.detectFatigue(5000))

        println("✓ Fatigue threshold boundary test passed")
    }

    @Test
    fun testHighFatigueFiltersPOIs() {
        // High fatigue should filter out long-duration POIs
        val candidatePOIs = allPOIs // Includes 150-min museum

        val adjustedDay = fatigueService.adjustItineraryForFatigue(
            remainingItinerary = emptyList(),
            allPOIs = candidatePOIs,
            fatigueLevel = FatigueLevel.HIGH,
            currentLocation = Coordinates(-37.8136, 144.9631),
            availableMinutes = 300,
            weatherStatus = WeatherStatus.SUNNY,
            dayNumber = 1,
            date = "2026-09-20"
        )

        // Melbourne Museum (150 min) should not be included
        val hasLongDurationPOI = adjustedDay.pois.any { it.poi.id == "poi_001" }
        assertFalse("Long-duration POI should be filtered out", hasLongDurationPOI)

        println("✓ High fatigue POI filtering test passed")
    }

    @Test
    fun testMediumFatigueAllowsModeratePOIs() {
        val adjustedDay = fatigueService.adjustItineraryForFatigue(
            remainingItinerary = emptyList(),
            allPOIs = allPOIs,
            fatigueLevel = FatigueLevel.MEDIUM,
            currentLocation = Coordinates(-37.8136, 144.9631),
            availableMinutes = 300,
            weatherStatus = WeatherStatus.SUNNY,
            dayNumber = 1,
            date = "2026-09-20"
        )

        // Should allow POIs up to 90 minutes
        val has90MinPOI = adjustedDay.pois.any {
            it.poi.recommendedVisitDuration <= 90
        }
        assertTrue("Should allow moderate-duration POIs", has90MinPOI)

        println("✓ Medium fatigue moderate POI allowance test passed")
    }
}
