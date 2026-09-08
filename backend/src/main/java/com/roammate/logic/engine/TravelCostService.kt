package com.roammate.logic.engine

import com.roammate.logic.models.TransportMode
import com.roammate.logic.utils.GeoUtils

/**
 * Service for calculating travel costs (time and distance)
 */
object TravelCostService {

    // Average speeds in km/h for different transport modes
    private const val WALKING_SPEED_KMH = 5.0
    private const val CYCLING_SPEED_KMH = 15.0
    private const val PUBLIC_TRANSIT_SPEED_KMH = 25.0
    private const val DRIVING_SPEED_KMH = 40.0

    /**
     * Calculate travel time in minutes between two coordinates
     */
    fun calculateTravelTime(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        transportMode: TransportMode
    ): Int {
        val distanceMeters = GeoUtils.calculateDistance(fromLat, fromLon, toLat, toLon)
        val distanceKm = distanceMeters / 1000.0

        val speedKmh = when (transportMode) {
            TransportMode.WALKING -> WALKING_SPEED_KMH
            TransportMode.CYCLING -> CYCLING_SPEED_KMH
            TransportMode.PUBLIC_TRANSIT -> PUBLIC_TRANSIT_SPEED_KMH
            TransportMode.DRIVING -> DRIVING_SPEED_KMH
        }

        val timeHours = distanceKm / speedKmh
        return (timeHours * 60).toInt() // Convert to minutes
    }

    /**
     * Calculate total travel cost for a route (sum of all segment times)
     */
    fun calculateRouteTravelTime(
        coordinates: List<Pair<Double, Double>>, // List of (lat, lon) pairs
        transportMode: TransportMode
    ): Int {
        if (coordinates.size < 2) return 0

        var totalTime = 0
        for (i in 0 until coordinates.size - 1) {
            val (fromLat, fromLon) = coordinates[i]
            val (toLat, toLon) = coordinates[i + 1]
            totalTime += calculateTravelTime(fromLat, fromLon, toLat, toLon, transportMode)
        }

        return totalTime
    }

    /**
     * Calculate EV/Cost ratio for optimization
     * Higher ratio = better value per time spent traveling
     */
    fun calculateEVCostRatio(expectedValue: Double, travelTimeMinutes: Int): Double {
        // Avoid division by zero
        if (travelTimeMinutes == 0) return expectedValue

        return expectedValue / travelTimeMinutes
    }
}
