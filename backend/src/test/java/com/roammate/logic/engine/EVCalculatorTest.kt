package com.roammate.logic.engine

import com.roammate.logic.models.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for EV Calculator
 */
class EVCalculatorTest {

    private val testUserProfile = UserProfile(
        interests = listOf(POICategory.MUSEUM, POICategory.PARK),
        budgetPreference = BudgetLevel.MEDIUM,
        transportMode = TransportMode.WALKING
    )

    private val testPOI = POI(
        id = "test_poi",
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

    @Test
    fun testCalculateEV_InterestMatch() {
        val ev = EVCalculator.calculateEV(testPOI, testUserProfile, WeatherStatus.SUNNY)
        // base_score (8.0) * interest_match (1.5) * budget_match (1.2) * weather (1.0)
        // = 8.0 * 1.5 * 1.2 * 1.0 = 14.4
        assertEquals(14.4, ev, 0.01)
    }

    @Test
    fun testCalculateEV_NoInterestMatch() {
        val restaurantPOI = testPOI.copy(category = POICategory.RESTAURANT)
        val ev = EVCalculator.calculateEV(restaurantPOI, testUserProfile, WeatherStatus.SUNNY)
        // base_score (8.0) * no_interest_match (1.0) * budget_match (1.2) * weather (1.0)
        // = 8.0 * 1.0 * 1.2 * 1.0 = 9.6
        assertEquals(9.6, ev, 0.01)
    }

    @Test
    fun testCalculateEV_RainyWeather_OutdoorPOI() {
        val outdoorPOI = testPOI.copy(environment = Environment.OUTDOOR)
        val ev = EVCalculator.calculateEV(outdoorPOI, testUserProfile, WeatherStatus.RAINY)
        // base_score (8.0) * interest (1.5) * budget (1.2) * rainy_outdoor_penalty (0.2)
        // = 8.0 * 1.5 * 1.2 * 0.2 = 2.88
        assertEquals(2.88, ev, 0.01)
    }

    @Test
    fun testCalculateEV_RainyWeather_IndoorPOI() {
        val ev = EVCalculator.calculateEV(testPOI, testUserProfile, WeatherStatus.RAINY)
        // base_score (8.0) * interest (1.5) * budget (1.2) * rainy_indoor_bonus (1.15)
        // = 8.0 * 1.5 * 1.2 * 1.15 = 16.56
        assertEquals(16.56, ev, 0.01)
    }

    @Test
    fun testShouldPrunePOI_InsufficientTime() {
        val shouldPrune = EVCalculator.shouldPrunePOI(
            poi = testPOI,
            currentTime = "16:00",
            availableMinutes = 30
        )
        // POI requires 120 minutes, only 30 available
        assertTrue(shouldPrune)
    }

    @Test
    fun testShouldPrunePOI_ClosingSoon() {
        val shouldPrune = EVCalculator.shouldPrunePOI(
            poi = testPOI,
            currentTime = "16:30",
            availableMinutes = 180
        )
        // Closes at 17:00, only 30 minutes remaining, but POI needs 120 minutes
        assertTrue(shouldPrune)
    }

    @Test
    fun testShouldPrunePOI_EnoughTime() {
        val shouldPrune = EVCalculator.shouldPrunePOI(
            poi = testPOI,
            currentTime = "10:00",
            availableMinutes = 300
        )
        // Opens at 10:00, closes at 17:00 (420 minutes), POI needs 120 minutes
        assertFalse(shouldPrune)
    }

    @Test
    fun testCalculateEVForPOIs_FiltersPruned() {
        val pois = listOf(
            testPOI, // Needs 120 min, closes at 17:00
            testPOI.copy(id = "poi2", recommendedVisitDuration = 30,
                operatingHours = OperatingHours("10:00", "18:00")) // Shorter visit, closes later
        )

        val results = EVCalculator.calculateEVForPOIs(
            pois = pois,
            userProfile = testUserProfile,
            weatherStatus = WeatherStatus.SUNNY,
            currentTime = "16:45",
            availableMinutes = 60
        )

        // Only poi2 should remain (needs 30 min, closes at 18:00, so has 75 min remaining)
        // testPOI is pruned (needs 120 min, closes at 17:00, only 15 min remaining)
        assertEquals(1, results.size)
        assertEquals("poi2", results[0].first.id)
    }

    @Test
    fun testBudgetWeight_OneLevelOff() {
        val lowBudgetPOI = testPOI.copy(budgetLevel = BudgetLevel.LOW)
        val ev = EVCalculator.calculateEV(lowBudgetPOI, testUserProfile, WeatherStatus.SUNNY)
        // base_score (8.0) * interest (1.5) * budget_one_off (1.0) * weather (1.0)
        // = 8.0 * 1.5 * 1.0 * 1.0 = 12.0
        assertEquals(12.0, ev, 0.01)
    }

    @Test
    fun testBudgetWeight_TwoLevelsOff() {
        val userHighBudget = testUserProfile.copy(budgetPreference = BudgetLevel.HIGH)
        val lowBudgetPOI = testPOI.copy(budgetLevel = BudgetLevel.LOW)
        val ev = EVCalculator.calculateEV(lowBudgetPOI, userHighBudget, WeatherStatus.SUNNY)
        // base_score (8.0) * interest (1.5) * budget_penalty (0.7) * weather (1.0)
        // = 8.0 * 1.5 * 0.7 * 1.0 = 8.4
        assertEquals(8.4, ev, 0.01)
    }
}
