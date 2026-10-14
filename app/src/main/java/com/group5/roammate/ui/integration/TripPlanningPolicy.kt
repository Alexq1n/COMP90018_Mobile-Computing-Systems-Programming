package com.group5.roammate.ui.integration

import com.group5.roammate.logic.models.Coordinates
import com.group5.roammate.logic.models.Itinerary
import com.group5.roammate.logic.models.POI

// Initial planning excludes the journey from the user's current city to the destination.
internal fun destinationPlanningOrigin(destinationPois: List<POI>): Coordinates =
    requireNotNull(destinationPois.firstOrNull { !it.isFiller }) {
        "No attractions are available for this destination yet. Choose another destination."
    }.coordinates

internal fun Itinerary.requireScheduledStops(): Itinerary = apply {
    require(days.any { it.pois.isNotEmpty() }) {
        "No stops fit this plan. Try an earlier start time or another destination."
    }
}
