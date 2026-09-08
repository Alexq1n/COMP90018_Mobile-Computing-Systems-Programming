package com.roammate.logic.engine

import com.roammate.logic.models.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Core Expected Value (EV) calculation engine
 * Calculates Final_EV = base_score * interest_weight * budget_weight * weather_modifier
 */
object EVCalculator {

    private const val INTEREST_MATCH_MULTIPLIER = 1.5
    private const val INTEREST_NO_MATCH_MULTIPLIER = 1.0

    private const val BUDGET_MATCH_MULTIPLIER = 1.2
    private const val BUDGET_ONE_LEVEL_OFF_MULTIPLIER = 1.0
    private const val BUDGET_TWO_LEVELS_OFF_PENALTY = 0.7

    private const val WEATHER_OUTDOOR_RAINY_PENALTY = 0.2
    private const val WEATHER_INDOOR_RAINY_BONUS = 1.15
    private const val WEATHER_OUTDOOR_SUNNY_BONUS = 1.1

    /**
     * Calculate the final Expected Value for a POI
     */
    fun calculateEV(
        poi: POI,
        userProfile: UserProfile,
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY
    ): Double {
        val interestWeight = calculateInterestWeight(poi.category, userProfile.interests)
        val budgetWeight = calculateBudgetWeight(poi.budgetLevel, userProfile.budgetPreference)
        val weatherModifier = calculateWeatherModifier(poi.environment, weatherStatus)

        return poi.baseScore * interestWeight * budgetWeight * weatherModifier
    }

    /**
     * Calculate interest weight based on user preferences
     */
    private fun calculateInterestWeight(
        poiCategory: POICategory,
        userInterests: List<POICategory>
    ): Double {
        return if (userInterests.contains(poiCategory)) {
            INTEREST_MATCH_MULTIPLIER
        } else {
            INTEREST_NO_MATCH_MULTIPLIER
        }
    }

    /**
     * Calculate budget weight based on user preference
     */
    private fun calculateBudgetWeight(
        poiBudget: BudgetLevel,
        userBudget: BudgetLevel
    ): Double {
        val budgetDifference = kotlin.math.abs(poiBudget.ordinal - userBudget.ordinal)

        return when (budgetDifference) {
            0 -> BUDGET_MATCH_MULTIPLIER // Exact match
            1 -> BUDGET_ONE_LEVEL_OFF_MULTIPLIER // One level off
            else -> BUDGET_TWO_LEVELS_OFF_PENALTY // Two levels off
        }
    }

    /**
     * Calculate weather modifier based on environment and weather
     */
    private fun calculateWeatherModifier(
        environment: Environment,
        weatherStatus: WeatherStatus
    ): Double {
        return when {
            // Rainy weather
            weatherStatus == WeatherStatus.RAINY || weatherStatus == WeatherStatus.STORMY -> {
                when (environment) {
                    Environment.OUTDOOR -> WEATHER_OUTDOOR_RAINY_PENALTY
                    Environment.INDOOR -> WEATHER_INDOOR_RAINY_BONUS
                    Environment.MIXED -> (WEATHER_OUTDOOR_RAINY_PENALTY + WEATHER_INDOOR_RAINY_BONUS) / 2
                }
            }
            // Sunny weather
            weatherStatus == WeatherStatus.SUNNY -> {
                when (environment) {
                    Environment.OUTDOOR -> WEATHER_OUTDOOR_SUNNY_BONUS
                    Environment.INDOOR -> 1.0
                    Environment.MIXED -> (WEATHER_OUTDOOR_SUNNY_BONUS + 1.0) / 2
                }
            }
            // Default (cloudy, snowy)
            else -> 1.0
        }
    }

    /**
     * Check if POI is currently open and has enough time for visit
     * Returns true if POI should be pruned (EV forced to 0)
     */
    fun shouldPrunePOI(
        poi: POI,
        currentTime: String, // Format: "HH:mm"
        availableMinutes: Int
    ): Boolean {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val current = LocalTime.parse(currentTime, formatter)
        val closeTime = LocalTime.parse(poi.operatingHours.closeTime, formatter)

        // Calculate remaining operating hours in minutes
        val remainingMinutes = if (closeTime.isAfter(current)) {
            java.time.Duration.between(current, closeTime).toMinutes().toInt()
        } else {
            0 // Already closed
        }

        // Prune if:
        // 1. POI is closed
        // 2. Remaining operating time < recommended visit duration
        // 3. Available time < recommended visit duration
        return remainingMinutes < poi.recommendedVisitDuration ||
               availableMinutes < poi.recommendedVisitDuration
    }

    /**
     * Calculate EV for a list of POIs, filtering out pruned ones
     */
    fun calculateEVForPOIs(
        pois: List<POI>,
        userProfile: UserProfile,
        weatherStatus: WeatherStatus,
        currentTime: String,
        availableMinutes: Int
    ): List<Pair<POI, Double>> {
        return pois
            .filter { !shouldPrunePOI(it, currentTime, availableMinutes) }
            .map { poi -> poi to calculateEV(poi, userProfile, weatherStatus) }
            .filter { it.second > 0 } // Only keep POIs with positive EV
    }
}
