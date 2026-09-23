package com.example.weathertest.data.geoapify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Minimal DTOs for the fields RoamMate actually uses. Unknown Geoapify fields are ignored. */
@Serializable
data class GeoapifyResponse(
    val features: List<GeoapifyFeature> = emptyList()
)

@Serializable
data class GeoapifyFeature(
    val properties: GeoapifyProperties = GeoapifyProperties()
)

@Serializable
data class GeoapifyProperties(
    val name: String? = null,
    val housenumber: String? = null,
    val street: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val categories: List<String> = emptyList(),
    @SerialName("place_id") val placeId: String? = null,
    val formatted: String? = null,
    @SerialName("opening_hours") val openingHours: String? = null,
    val description: String? = null,
    val website: String? = null,
    val phone: String? = null
)
