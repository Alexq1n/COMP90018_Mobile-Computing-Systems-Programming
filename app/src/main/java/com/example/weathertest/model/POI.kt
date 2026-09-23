package com.example.weathertest.model

import kotlinx.serialization.Serializable

@Serializable
data class POI(
    val id: String,
    val name: String,
    val baseScore: Double,
    val category: POICategory,
    val environment: Environment,
    val coordinates: Coordinates,
    val recommendedVisitDuration: Int,
    val operatingHours: OperatingHours,
    val isFiller: Boolean,
    val city: String,
    val address: String?,
    val description: String?
)

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class OperatingHours(
    val openTime: String,
    val closeTime: String
)

@Serializable
enum class POICategory {
    MUSEUM, PARK, RESTAURANT, SHOPPING, ENTERTAINMENT,
    HISTORICAL, NATURE, BEACH, SPORTS, CULTURAL,
    NIGHTLIFE, CAFE, LANDMARK, SCENIC_SPOT
}

@Serializable
enum class Environment {
    INDOOR,
    OUTDOOR,
    MIXED
}
