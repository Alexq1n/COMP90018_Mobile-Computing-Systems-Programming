package com.example.localdatebase.planner

/** Small UI-facing projection of a saved itinerary document. */
data class SavedTripSummary(
    val tripId: String,
    val destination: String,
    val durationText: String,
    val dateText: String,
)
