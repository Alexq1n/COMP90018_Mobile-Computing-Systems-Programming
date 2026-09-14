package com.roammate.logic.engine

import com.roammate.logic.interfaces.ITravelCostService
import com.roammate.logic.models.TransportMode
import com.roammate.logic.models.TravelDurationMatrix
import com.roammate.logic.utils.GeoUtils

/**
 * Service for calculating travel costs (time and distance) with real data and user preferences
 */
object TravelCostService : ITravelCostService {

    // Average speeds in km/h for different transport modes (fallback for Haversine calculation)
    private const val WALKING_SPEED_KMH = 5.0
    private const val CYCLING_SPEED_KMH = 15.0
    private const val PUBLIC_TRANSIT_SPEED_KMH = 25.0
    private const val DRIVING_SPEED_KMH = 40.0

    /**
     * Calculate preference-adjusted travel time between two POIs using real duration data
     *
     * @param fromPoiId Source POI ID
     * @param toPoiId Destination POI ID
     * @param transportMode User's transport mode
     * @param transportPreference User's preference multiplier (<1.0 prefers driving, >1.0 prefers walking)
     * @param durationMatrix Real travel duration data from upstream
     * @return Adjusted travel time in minutes, or null if no data available
     */
    override fun calculateAdjustedTravelTime(
        fromPoiId: String,
        toPoiId: String,
        transportMode: TransportMode,
        transportPreference: Float,
        durationMatrix: TravelDurationMatrix?
    ): Int? {
        if (durationMatrix == null) return null

        val duration = durationMatrix.getDuration(fromPoiId, toPoiId) ?: return null

        // Select base time based on transport mode
        val baseTime = when (transportMode) {
            TransportMode.WALKING, TransportMode.PUBLIC_TRANSIT -> duration.walkingMinutes.toFloat()
            TransportMode.DRIVING, TransportMode.CYCLING -> duration.drivingMinutes.toFloat()
        }

        // Apply preference adjustment
        // transportPreference > 1.0: prefers walking, reduce walking cost
        // transportPreference < 1.0: prefers driving, reduce driving cost
        val adjustedTime = when (transportMode) {
            TransportMode.WALKING -> baseTime / transportPreference
            TransportMode.DRIVING -> baseTime * transportPreference
            else -> baseTime // Neutral for other modes
        }

        return adjustedTime.toInt().coerceAtLeast(1)
    }

    /**
     * Legacy method: Calculate travel time in minutes between two coordinates using Haversine
     * Used as fallback when real duration data is unavailable
     */
    override fun calculateTravelTime(
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
    override fun calculateEVCostRatio(expectedValue: Double, travelTimeMinutes: Int): Double {
        // Avoid division by zero
        if (travelTimeMinutes == 0) return expectedValue

        return expectedValue / travelTimeMinutes
    }
}
