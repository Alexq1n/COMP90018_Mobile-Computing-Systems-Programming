package com.roammate.logic.interfaces

import com.roammate.logic.models.TransportMode
import com.roammate.logic.models.TravelDurationMatrix

/**
 * Interface for travel cost calculation services
 */
interface ITravelCostService {

    /**
     * Calculate preference-adjusted travel time between two POIs
     *
     * @param fromPoiId Source POI ID
     * @param toPoiId Destination POI ID
     * @param transportMode User's transport mode
     * @param transportPreference User's preference multiplier (walking vs driving)
     * @param durationMatrix Real travel duration data from upstream
     * @return Adjusted travel time in minutes, or null if no data available
     */
    fun calculateAdjustedTravelTime(
        fromPoiId: String,
        toPoiId: String,
        transportMode: TransportMode,
        transportPreference: Float,
        durationMatrix: TravelDurationMatrix?
    ): Int?

    /**
     * Calculate EV/Cost ratio for optimization
     */
    fun calculateEVCostRatio(expectedValue: Double, travelTimeMinutes: Int): Double

    /**
     * Legacy method: Calculate travel time using Haversine formula
     * Used as fallback when real duration data is unavailable
     */
    fun calculateTravelTime(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        transportMode: TransportMode
    ): Int
}
