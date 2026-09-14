package com.roammate.logic.models

import kotlinx.serialization.Serializable

/**
 * Real-time context information for dynamic trip adjustments
 */
@Serializable
data class ContextPayload(
    val currentLocation: Coordinates,
    val currentTime: String, // Format: "yyyy-MM-dd'T'HH:mm:ss"
    val weatherStatus: WeatherStatus, // Historical/planned weather
    val currentWeather: WeatherStatus? = null, // Real-time weather when app opens
    val itineraryProgress: ItineraryProgress,
    val recentStepCount: Int? = null // Steps in last 1 hour
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
