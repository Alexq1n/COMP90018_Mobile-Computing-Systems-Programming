package com.group5.roammate.ui.integration

import com.group5.roammate.logic.models.*
import com.group5.roammate.ui.screens.stopsOnDate
import com.group5.roammate.ui.screens.toTripDayPlans
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCloudAdapterTest {
    @Test
    fun cloudRoundTripPreservesIdsDatesOrderAndPoiMetadata() {
        val trip = trip()
        assertEquals(trip, trip.toStoredTrip().toUiTrip())
    }

    @Test
    fun restoredFutureTripDoesNotAppearAsTodaysTrip() {
        val restored = trip().toStoredTrip().toUiTrip()
        val plans = restored.toTripDayPlans(LocalDate.of(2026, 10, 14))
        assertEquals(1, plans.size)
        assertEquals("Future museum", plans.single().stops.single().title)
        assertTrue(plans.flatMap { it.stops }.stopsOnDate(LocalDate.of(2026, 10, 14)).isEmpty())
    }

    @Test(expected = DateTimeParseException::class)
    fun malformedCloudDateIsRejectedBeforeReplacingTheCurrentTrip() {
        val stored = trip().toStoredTrip()
        stored.copy(days = stored.days.map { it.copy(date = "not-a-date") }).toUiTrip()
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateCloudDatesAreRejected() {
        val trip = trip()
        trip.copy(days = listOf(trip.days.single(), trip.days.single().copy(dayNumber = 2))).toStoredTrip()
    }

    @Test(expected = IllegalArgumentException::class)
    fun payloadFromADifferentTripIsRejected() {
        trip().toStoredTrip().toUiTrip("a-different-trip")
    }

    private fun trip(): Itinerary {
        val poi = POI(
            id = "museum-1", name = "Future museum", baseScore = 7.0,
            category = POICategory.MUSEUM, budgetLevel = BudgetLevel.MEDIUM,
            environment = Environment.INDOOR, coordinates = Coordinates(-37.81, 144.96),
            recommendedVisitDuration = 90, operatingHours = OperatingHours("09:00", "18:00"),
            city = "Melbourne", description = "Actual source description",
        )
        val stop = ItineraryPOI(poi, "10:00", "11:30", 15, 8.0, isUserPinned = true)
        val day = DayItinerary(1, "2026-10-16", listOf(stop), 105, 15, 90)
        return Itinerary("trip-1", listOf(day), 25.0, 8.0)
    }
}
