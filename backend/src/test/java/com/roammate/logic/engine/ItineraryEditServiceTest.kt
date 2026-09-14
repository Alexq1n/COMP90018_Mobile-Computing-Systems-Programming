package com.roammate.logic.engine

import com.roammate.logic.models.*
import com.roammate.logic.utils.MockPoiRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for ItineraryEditService
 */
class ItineraryEditServiceTest {

    private lateinit var editService: ItineraryEditService
    private lateinit var userProfile: UserProfile
    private lateinit var testItinerary: Itinerary
    private lateinit var allPOIs: List<POI>

    @Before
    fun setup() {
        userProfile = UserProfile(
            interests = listOf(POICategory.MUSEUM, POICategory.PARK),
            budgetPreference = BudgetLevel.MEDIUM,
            transportMode = TransportMode.WALKING,
            transportPreference = 1.0f
        )

        editService = ItineraryEditService(userProfile)

        // Create test POIs
        allPOIs = listOf(
            POI(
                id = "poi_001",
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
            POI(
                id = "poi_002",
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
                id = "poi_003",
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
            )
        )

        // Create test itinerary with 2 POIs
        val day1POIs = listOf(
            ItineraryPOI(
                poi = allPOIs[0],
                startTime = "09:15",
                endTime = "10:15",
                travelTimeFromPrevious = 15,
                expectedValue = 8.0,
                isFiller = false,
                isUserPinned = false
            ),
            ItineraryPOI(
                poi = allPOIs[1],
                startTime = "10:30",
                endTime = "12:30",
                travelTimeFromPrevious = 15,
                expectedValue = 9.0,
                isFiller = false,
                isUserPinned = false
            )
        )

        val day1 = DayItinerary(
            dayNumber = 1,
            date = "2026-09-20",
            pois = day1POIs,
            totalDuration = 195,
            totalTravelTime = 30,
            totalVisitTime = 180
        )

        testItinerary = Itinerary(
            tripId = "test_trip",
            days = listOf(day1),
            totalEstimatedCost = 100.0,
            totalExpectedValue = 17.0
        )
    }

    @Test
    fun testAddPOI_Success() {
        // Add POI_003 (Botanic Gardens) at position 2 (end of day)
        val result = editService.addPoiToItinerary(
            itinerary = testItinerary,
            poi = allPOIs[2],
            targetDayIndex = 0,
            insertPosition = 2,
            weatherStatus = WeatherStatus.SUNNY
        )

        assertTrue("Add operation should succeed", result.success)
        assertNotNull("Updated day should not be null", result.updatedDay)
        assertEquals("Should have 3 POIs", 3, result.updatedDay!!.pois.size)
        assertTrue("New POI should be user-pinned", result.updatedDay!!.pois[2].isUserPinned)
        assertEquals("New POI should be at position 2", "poi_003", result.updatedDay!!.pois[2].poi.id)

        println("✓ Add POI test passed: ${result.validationMessage}")
    }

    @Test
    fun testAddPOI_InvalidDayIndex() {
        val result = editService.addPoiToItinerary(
            itinerary = testItinerary,
            poi = allPOIs[2],
            targetDayIndex = 5, // Invalid index
            insertPosition = 0,
            weatherStatus = WeatherStatus.SUNNY
        )

        assertFalse("Add operation should fail", result.success)
        assertNull("Updated day should be null", result.updatedDay)
        assertTrue("Should have error message", result.validationMessage.contains("Invalid day index"))

        println("✓ Invalid day index test passed: ${result.validationMessage}")
    }

    @Test
    fun testRemovePOI_Success() {
        // Remove POI_001 (Federation Square)
        val result = editService.removePoiFromItinerary(
            itinerary = testItinerary,
            poiId = "poi_001",
            dayIndex = 0
        )

        assertTrue("Remove operation should succeed", result.success)
        assertNotNull("Updated day should not be null", result.updatedDay)
        assertEquals("Should have 1 POI remaining", 1, result.updatedDay!!.pois.size)
        assertEquals("Remaining POI should be poi_002", "poi_002", result.updatedDay!!.pois[0].poi.id)
        assertTrue("Should have freed time", result.freedTimeMinutes > 0)

        println("✓ Remove POI test passed: Freed ${result.freedTimeMinutes} minutes")
    }

    @Test
    fun testRemovePOI_NotFound() {
        val result = editService.removePoiFromItinerary(
            itinerary = testItinerary,
            poiId = "poi_999", // Non-existent POI
            dayIndex = 0
        )

        assertFalse("Remove operation should fail", result.success)
        assertTrue("Should have error message", result.validationMessage.contains("not found"))

        println("✓ POI not found test passed: ${result.validationMessage}")
    }

    @Test
    fun testEditItinerary_AddAction() {
        val result = editService.editItinerary(
            itinerary = testItinerary,
            action = EditAction.ADD_POI,
            params = EditParams(
                poiId = "poi_003",
                dayIndex = 0,
                insertPosition = 2
            ),
            allPOIs = allPOIs,
            weatherStatus = WeatherStatus.SUNNY
        )

        assertTrue("Unified add action should succeed", result.success)
        assertNotNull("Updated day should not be null", result.updatedDay)

        println("✓ Unified edit interface (ADD) test passed")
    }

    @Test
    fun testEditItinerary_RemoveAction() {
        val result = editService.editItinerary(
            itinerary = testItinerary,
            action = EditAction.REMOVE_POI,
            params = EditParams(
                poiId = "poi_001",
                dayIndex = 0
            ),
            allPOIs = allPOIs
        )

        assertTrue("Unified remove action should succeed", result.success)
        assertNotNull("Updated day should not be null", result.updatedDay)

        println("✓ Unified edit interface (REMOVE) test passed")
    }

    @Test
    fun testReoptimizeDay() {
        // First remove a POI to free up time
        val removeResult = editService.removePoiFromItinerary(
            itinerary = testItinerary,
            poiId = "poi_001",
            dayIndex = 0
        )

        assertTrue(removeResult.success)

        // Now reoptimize with the freed time
        val dayAfterRemoval = removeResult.updatedDay!!
        val optimizedDay = editService.reoptimizeDay(
            dayItinerary = dayAfterRemoval,
            allPOIs = allPOIs,
            availableTimeMinutes = 540, // 9 hours
            weatherStatus = WeatherStatus.SUNNY
        )

        assertNotNull("Optimized day should not be null", optimizedDay)
        // May add more POIs depending on available time and candidates
        assertTrue("Should have at least the original remaining POI", optimizedDay.pois.size >= 1)

        println("✓ Reoptimize day test passed: ${optimizedDay.pois.size} POIs after reoptimization")
    }

    @Test
    fun testUserPinnedPOIsPreserved() {
        // Add a user-pinned POI at the end (position 2) where there's enough time
        val addResult = editService.addPoiToItinerary(
            itinerary = testItinerary,
            poi = allPOIs[2],
            targetDayIndex = 0,
            insertPosition = 2, // Changed from 1 to 2 (at the end)
            weatherStatus = WeatherStatus.SUNNY
        )

        // Print debug info if failed
        if (!addResult.success) {
            println("❌ Add POI failed: ${addResult.validationMessage}")
            println("   Existing POIs in day:")
            testItinerary.days[0].pois.forEachIndexed { idx, poi ->
                println("   [$idx] ${poi.poi.name}: ${poi.startTime} - ${poi.endTime}")
            }
            println("   Trying to add: ${allPOIs[2].name} (duration: ${allPOIs[2].recommendedVisitDuration} min)")
        }

        assertTrue("Add operation should succeed: ${addResult.validationMessage}", addResult.success)
        val dayWithPinnedPOI = addResult.updatedDay!!

        // Reoptimize - should preserve the pinned POI
        val optimizedDay = editService.reoptimizeDay(
            dayItinerary = dayWithPinnedPOI,
            allPOIs = allPOIs,
            availableTimeMinutes = 540,
            weatherStatus = WeatherStatus.SUNNY
        )

        val pinnedPOIs = optimizedDay.pois.filter { it.isUserPinned }
        assertTrue("Should still have pinned POI after reoptimization", pinnedPOIs.isNotEmpty())
        assertEquals("Pinned POI should be poi_003", "poi_003", pinnedPOIs[0].poi.id)

        println("✓ User-pinned POI preservation test passed")
    }
}
