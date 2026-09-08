package com.roammate.logic.models

import kotlinx.serialization.Serializable

/**
 * Represents user travel preferences and profile
 */
@Serializable
data class UserProfile(
    val interests: List<POICategory>,
    val budgetPreference: BudgetLevel,
    val transportMode: TransportMode
)

@Serializable
enum class TransportMode {
    WALKING,
    PUBLIC_TRANSIT,
    DRIVING,
    CYCLING
}
