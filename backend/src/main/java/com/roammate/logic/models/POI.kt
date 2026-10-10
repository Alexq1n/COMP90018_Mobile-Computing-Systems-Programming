package com.roammate.logic.models

import kotlinx.serialization.Serializable

/**
 * Represents a Point of Interest (POI) in the travel database
 */
@Serializable
data class POI(
    val id: String,
    val name: String,
    val baseScore: Double, // 0-10 scale
    val category: POICategory,
    val budgetLevel: BudgetLevel,
    val environment: Environment,
    val coordinates: Coordinates,
    val recommendedVisitDuration: Int, // minutes
    val operatingHours: OperatingHours,
    val isFiller: Boolean = false,
    val city: String,
    val description: String? = null
)

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class OperatingHours(
    val openTime: String, // Format: "HH:mm"
    val closeTime: String // Format: "HH:mm"
)

@Serializable
enum class POICategory {
    MUSEUM,
    PARK,
    RESTAURANT,
    SHOPPING,
    ENTERTAINMENT,
    HISTORICAL,
    NATURE,
    BEACH,
    SPORTS,
    CULTURAL,
    NIGHTLIFE,
    CAFE,
    LANDMARK,
    SCENIC_SPOT
}

@Serializable
enum class BudgetLevel {
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
enum class Environment {
    INDOOR,
    OUTDOOR,
    MIXED
}
