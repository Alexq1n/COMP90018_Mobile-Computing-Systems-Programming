package com.roammate.logic.models

import kotlinx.serialization.Serializable

/**
 * Represents a complete trip itinerary
 */
@Serializable
data class Itinerary(
    val tripId: String,
    val days: List<DayItinerary>,
    val totalEstimatedCost: Double,
    val totalExpectedValue: Double
)

@Serializable
data class DayItinerary(
    val dayNumber: Int,
    val date: String, // Format: "yyyy-MM-dd"
    val pois: List<ItineraryPOI>,
    val totalDuration: Int, // minutes
    val totalTravelTime: Int, // minutes
    val totalVisitTime: Int // minutes
)

@Serializable
data class ItineraryPOI(
    val poi: POI,
    val startTime: String, // Format: "HH:mm"
    val endTime: String, // Format: "HH:mm"
    val travelTimeFromPrevious: Int, // minutes
    val expectedValue: Double,
    val isFiller: Boolean = false
)

/**
 * Response containing two adjustment options
 */
@Serializable
data class AdjustmentResponse(
    val optionA: AdjustmentOption,
    val optionB: AdjustmentOption
)

@Serializable
data class AdjustmentOption(
    val strategy: AdjustmentStrategy,
    val remainingItinerary: List<ItineraryPOI>,
    val droppedPOIs: List<POI>,
    val totalExpectedValue: Double,
    val totalTravelTime: Int,
    val description: String
)

@Serializable
enum class AdjustmentStrategy {
    EXPERIENCE_FIRST, // Option A: Maximize EV
    EFFICIENCY_FIRST  // Option B: Maximize EV/Cost ratio
}
