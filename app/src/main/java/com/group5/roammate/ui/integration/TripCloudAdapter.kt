package com.group5.roammate.ui.integration

import com.google.gson.Gson
import com.group5.roammate.logic.models.Itinerary
import com.roammate.logic.models.Itinerary as StoredItinerary
import java.time.LocalDate

// Both modules use the same persisted JSON schema, with different Kotlin packages.
private val tripJson = Gson()

internal fun Itinerary.toStoredTrip(): StoredItinerary {
    validateTrip(this)
    return tripJson.fromJson(tripJson.toJson(this), StoredItinerary::class.java)
}

internal fun StoredItinerary.toUiTrip(expectedTripId: String? = null): Itinerary =
    tripJson.fromJson(tripJson.toJson(this), Itinerary::class.java).also { trip ->
        validateTrip(trip)
        require(expectedTripId == null || trip.tripId == expectedTripId) {
            "Saved trip ID does not match the selected trip"
        }
    }

private fun validateTrip(trip: Itinerary) {
    require(trip.tripId.isNotBlank() && !trip.tripId.contains('/')) { "Saved trip has an invalid ID" }
    require(trip.days.isNotEmpty()) { "Saved trip has no days" }
    require(trip.days.map { it.date }.distinct().size == trip.days.size) {
        "Saved trip contains duplicate dates"
    }
    trip.days.forEach { day ->
        LocalDate.parse(day.date)
        require(day.pois.all { it.poi.id.isNotBlank() && it.poi.name.isNotBlank() }) {
            "Saved trip contains an invalid stop"
        }
    }
}
