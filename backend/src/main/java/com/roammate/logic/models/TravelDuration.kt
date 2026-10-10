package com.roammate.logic.models

import kotlinx.serialization.Serializable

/**
 * Represents real travel duration data between two POIs from upstream services
 */
@Serializable
data class TravelDuration(
    val fromPoiId: String,
    val toPoiId: String,
    val walkingMinutes: Int,
    val drivingMinutes: Int
)

/**
 * Matrix holder for efficient lookup of travel durations between POI pairs
 */
class TravelDurationMatrix(private val durations: List<TravelDuration>) {

    private val durationMap: Map<Pair<String, String>, TravelDuration> = durations.associateBy {
        Pair(it.fromPoiId, it.toPoiId)
    }

    /**
     * Get travel duration between two POIs
     * Returns null if no data exists for this pair
     */
    fun getDuration(fromPoiId: String, toPoiId: String): TravelDuration? {
        return durationMap[Pair(fromPoiId, toPoiId)]
    }

    /**
     * Get walking time in minutes between two POIs
     */
    fun getWalkingTime(fromPoiId: String, toPoiId: String): Int? {
        return getDuration(fromPoiId, toPoiId)?.walkingMinutes
    }

    /**
     * Get driving time in minutes between two POIs
     */
    fun getDrivingTime(fromPoiId: String, toPoiId: String): Int? {
        return getDuration(fromPoiId, toPoiId)?.drivingMinutes
    }
}
