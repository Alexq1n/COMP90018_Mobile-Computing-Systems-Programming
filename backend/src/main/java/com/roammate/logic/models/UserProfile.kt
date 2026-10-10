package com.roammate.logic.models

import kotlinx.serialization.Serializable

/**
 * Represents user travel preferences and profile
 */
@Serializable
data class UserProfile(
    val interests: List<POICategory>,
    val budgetPreference: BudgetLevel,
    val transportMode: TransportMode,
    val transportPreference: Float = 1.0f // Multiplier: <1.0 prefers driving, >1.0 prefers walking, 1.0 neutral
)

@Serializable
enum class TransportMode {
    WALKING,
    PUBLIC_TRANSIT,
    DRIVING,
    CYCLING
}
