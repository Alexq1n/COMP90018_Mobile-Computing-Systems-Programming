package com.roammate.logic.engine

import com.roammate.logic.models.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Dynamic itinerary adjustment engine
 * Responds to delays and weather changes with two strategic options
 */
class DynamicAdjustmentEngine(
    private val userProfile: UserProfile
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Adjust itinerary based on current context
     * Returns two options: Experience First (Max EV) and Efficiency First (Max EV/Cost)
     */
    fun adjustItinerary(
        currentItinerary: List<ItineraryPOI>,
        contextPayload: ContextPayload,
        allPOIs: List<POI>,
        dailyEndTime: String = "18:00"
    ): AdjustmentResponse {
        // Parse current state
        val completedPOIIds = contextPayload.itineraryProgress.completedPOIs.toSet()
        val currentTime = parseTimeFromDateTime(contextPayload.currentTime)
        val currentTimeObj = LocalTime.parse(currentTime, timeFormatter)
        val endTimeObj = LocalTime.parse(dailyEndTime, timeFormatter)

        // Calculate remaining time budget
        val remainingMinutes = if (endTimeObj.isAfter(currentTimeObj)) {
            java.time.Duration.between(currentTimeObj, endTimeObj).toMinutes().toInt()
        } else {
            0
        }

        // Get remaining POIs from original itinerary
        val remainingOriginalPOIs = currentItinerary
            .filter { it.poi.id !in completedPOIIds }
            .map { it.poi }

        // Calculate EV for remaining POIs with updated weather
        val poisWithEV = EVCalculator.calculateEVForPOIs(
            pois = remainingOriginalPOIs,
            userProfile = userProfile,
            weatherStatus = contextPayload.weatherStatus,
            currentTime = currentTime,
            availableMinutes = remainingMinutes
        )

        // Generate Option A: Experience First (Max EV)
        val optionA = generateExperienceFirstOption(
            poisWithEV = poisWithEV,
            currentLocation = contextPayload.currentLocation,
            currentTime = currentTimeObj,
            remainingMinutes = remainingMinutes
        )

        // Generate Option B: Efficiency First (Max EV/Cost)
        val optionB = generateEfficiencyFirstOption(
            poisWithEV = poisWithEV,
            currentLocation = contextPayload.currentLocation,
            currentTime = currentTimeObj,
            remainingMinutes = remainingMinutes
        )

        return AdjustmentResponse(
            optionA = optionA,
            optionB = optionB
        )
    }

    /**
     * Option A: Maximize total EV (Experience First)
     * Prioritizes high-value POIs regardless of travel cost
     */
    private fun generateExperienceFirstOption(
        poisWithEV: List<Pair<POI, Double>>,
        currentLocation: Coordinates,
        currentTime: LocalTime,
        remainingMinutes: Int
    ): AdjustmentOption {
        // Sort by EV descending
        val sortedByEV = poisWithEV.sortedByDescending { it.second }

        val (selected, dropped) = selectPOIsGreedy(
            candidates = sortedByEV,
            currentLocation = currentLocation,
            currentTime = currentTime,
            remainingMinutes = remainingMinutes,
            prioritizeEV = true
        )

        val totalEV = selected.sumOf { it.expectedValue }
        val totalTravelTime = selected.sumOf { it.travelTimeFromPrevious }

        return AdjustmentOption(
            strategy = AdjustmentStrategy.EXPERIENCE_FIRST,
            remainingItinerary = selected,
            droppedPOIs = dropped,
            totalExpectedValue = totalEV,
            totalTravelTime = totalTravelTime,
            description = "Maximizes experience value by retaining high-score attractions. " +
                    "May involve longer travel distances. Total EV: ${"%.2f".format(totalEV)}, " +
                    "Travel time: ${totalTravelTime}min"
        )
    }

    /**
     * Option B: Maximize EV/Cost ratio (Efficiency First)
     * Prioritizes nearby POIs to reduce travel fatigue
     */
    private fun generateEfficiencyFirstOption(
        poisWithEV: List<Pair<POI, Double>>,
        currentLocation: Coordinates,
        currentTime: LocalTime,
        remainingMinutes: Int
    ): AdjustmentOption {
        // Calculate EV/Cost ratio for each POI based on distance from current location
        val poisWithRatio = poisWithEV.map { (poi, ev) ->
            val travelTime = TravelCostService.calculateTravelTime(
                fromLat = currentLocation.latitude,
                fromLon = currentLocation.longitude,
                toLat = poi.coordinates.latitude,
                toLon = poi.coordinates.longitude,
                transportMode = userProfile.transportMode
            )

            val ratio = if (travelTime > 0) {
                ev / travelTime
            } else {
                ev * 100 // Very close, high priority
            }

            Triple(poi, ev, ratio)
        }.sortedByDescending { it.third } // Sort by ratio descending

        val poisWithEVSorted = poisWithRatio.map { it.first to it.second }

        val (selected, dropped) = selectPOIsGreedy(
            candidates = poisWithEVSorted,
            currentLocation = currentLocation,
            currentTime = currentTime,
            remainingMinutes = remainingMinutes,
            prioritizeEV = false
        )

        val totalEV = selected.sumOf { it.expectedValue }
        val totalTravelTime = selected.sumOf { it.travelTimeFromPrevious }

        return AdjustmentOption(
            strategy = AdjustmentStrategy.EFFICIENCY_FIRST,
            remainingItinerary = selected,
            droppedPOIs = dropped,
            totalExpectedValue = totalEV,
            totalTravelTime = totalTravelTime,
            description = "Maximizes efficiency by prioritizing nearby attractions. " +
                    "Reduces travel fatigue with shorter distances. Total EV: ${"%.2f".format(totalEV)}, " +
                    "Travel time: ${totalTravelTime}min"
        )
    }

    /**
     * Greedy selection of POIs within time constraints
     */
    private fun selectPOIsGreedy(
        candidates: List<Pair<POI, Double>>,
        currentLocation: Coordinates,
        currentTime: LocalTime,
        remainingMinutes: Int,
        prioritizeEV: Boolean
    ): Pair<List<ItineraryPOI>, List<POI>> {
        val selected = mutableListOf<ItineraryPOI>()
        val dropped = mutableListOf<POI>()

        var currentLoc = currentLocation
        var currentTimeObj = currentTime
        var timeLeft = remainingMinutes

        for ((poi, ev) in candidates) {
            val travelTime = TravelCostService.calculateTravelTime(
                fromLat = currentLoc.latitude,
                fromLon = currentLoc.longitude,
                toLat = poi.coordinates.latitude,
                toLon = poi.coordinates.longitude,
                transportMode = userProfile.transportMode
            )

            val totalRequired = travelTime + poi.recommendedVisitDuration

            if (totalRequired <= timeLeft) {
                // Can fit this POI
                currentTimeObj = currentTimeObj.plusMinutes(travelTime.toLong())
                val startTime = currentTimeObj.format(timeFormatter)
                currentTimeObj = currentTimeObj.plusMinutes(poi.recommendedVisitDuration.toLong())
                val endTime = currentTimeObj.format(timeFormatter)

                selected.add(
                    ItineraryPOI(
                        poi = poi,
                        startTime = startTime,
                        endTime = endTime,
                        travelTimeFromPrevious = travelTime,
                        expectedValue = ev,
                        isFiller = false
                    )
                )

                currentLoc = poi.coordinates
                timeLeft -= totalRequired
            } else {
                // Cannot fit, add to dropped
                dropped.add(poi)
            }
        }

        return selected to dropped
    }

    /**
     * Extract time (HH:mm) from ISO datetime string (yyyy-MM-dd'T'HH:mm:ss)
     */
    private fun parseTimeFromDateTime(dateTime: String): String {
        val parts = dateTime.split("T")
        return if (parts.size == 2) {
            parts[1].substring(0, 5) // Extract HH:mm
        } else {
            "12:00" // Default fallback
        }
    }
}
