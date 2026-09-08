package com.roammate.logic.models

import kotlinx.serialization.Serializable

/**
 * Real-time context information for dynamic trip adjustments
 */
@Serializable
data class ContextPayload(
    val currentLocation: Coordinates,
    val currentTime: String, // Format: "yyyy-MM-dd'T'HH:mm:ss"
    val weatherStatus: WeatherStatus,
    val itineraryProgress: ItineraryProgress
)

@Serializable
data class ItineraryProgress(
    val completedPOIs: List<String>, // List of completed POI IDs
    val delayMinutes: Int // Delay in minutes from original schedule
)

@Serializable
enum class WeatherStatus {
    SUNNY,
    CLOUDY,
    RAINY,
    STORMY,
    SNOWY
}
