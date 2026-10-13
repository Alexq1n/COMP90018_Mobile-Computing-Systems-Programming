package com.group5.roammate.logic.models

import kotlinx.serialization.Serializable

/**
 * Represents a user-specified POI that must be visited on a specific day
 */
@Serializable
data class PinnedPOI(
    val poiId: String,
    val dayNumber: Int, // 1-based day number
    val preferredStartTime: String? = null // Optional: "HH:mm" format
)
