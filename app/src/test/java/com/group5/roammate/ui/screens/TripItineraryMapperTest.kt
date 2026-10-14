package com.group5.roammate.ui.screens

import com.group5.roammate.logic.models.BudgetLevel
import com.group5.roammate.logic.models.Coordinates
import com.group5.roammate.logic.models.DayItinerary
import com.group5.roammate.logic.models.Environment
import com.group5.roammate.logic.models.Itinerary
import com.group5.roammate.logic.models.ItineraryPOI
import com.group5.roammate.logic.models.OperatingHours
import com.group5.roammate.logic.models.POI
import com.group5.roammate.logic.models.POICategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripItineraryMapperTest {
    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun noGeneratedItineraryHasNoDayOne() {
        val noItinerary: Itinerary? = null
        assertTrue(noItinerary.toTripDayPlans(today).isEmpty())
        assertTrue(itinerary().toTripDayPlans(today).isEmpty())
    }

    @Test
    fun futureTripKeepsAllDaysWithoutShowingTodaysStops() {
        val plans = itinerary(
            day(1, "2026-10-16"),
            day(2, "2026-10-17"),
        ).toTripDayPlans(today)

        assertEquals(listOf("Day 1", "Day 2"), plans.map { it.dayTitle })
        assertEquals(listOf("Oct 16", "Oct 17"), plans.map { it.dateText })
        val stops = plans.flatMap { it.stops }
        assertTrue(stops.stopsOnDate(today).isEmpty())
        assertTrue(stops.all { it.status == TripStopStatus.Upcoming })
    }

    @Test
    fun todaysTripUsesTheMatchingDayRatherThanTheFirstDay() {
        val plans = itinerary(
            day(1, "2026-10-13"),
            day(2, "2026-10-14"),
            day(3, "2026-10-15"),
        ).toTripDayPlans(today)

        assertEquals(listOf("Oct 13", "Today", "Tomorrow"), plans.map { it.dateText })
        val stops = plans.flatMap { it.stops }.stopsOnDate(today)
        assertEquals(listOf("Stop 2"), stops.map { it.title })
        assertEquals(TripStopStatus.Current, stops.single().status)
    }

    @Test
    fun finishedTripHasNoTodaysStops() {
        val plans = itinerary(day(1, "2026-10-13")).toTripDayPlans(today)
        assertTrue(plans.flatMap { it.stops }.stopsOnDate(today).isEmpty())
    }

    @Test
    fun undatedStopsDoNotAppearInTodaysTrip() {
        val stops = listOf(TripTimelineStop("09:00", "Sample stop", TripStopStatus.Current))
        assertTrue(stops.stopsOnDate(today).isEmpty())
    }

    @Test
    fun treasureStopRemainsInTheItinerary() {
        val day = day(1, "2026-10-14")
        val treasureDay = day.copy(pois = day.pois.map { it.copy(isFiller = true) })
        val stops = itinerary(treasureDay).toTripDayPlans(today).single().stops
        assertEquals(listOf("Stop 1"), stops.map { it.title })
    }

    @Test
    fun readOnlyTripTodayAndNavigationUseTheSameTimelineWithoutChangingStops() {
        val itinerary = itinerary(day(1, "2026-10-14"), day(2, "2026-10-15"))
        val completed = TripTimelineStop("08:00", "Completed", TripStopStatus.Done, date = today.toString())
        val museum = TripTimelineStop("10:00", "Museum", TripStopStatus.Current, date = today.toString())
        val garden = TripTimelineStop("12:00", "Garden", TripStopStatus.Upcoming, date = today.toString())
        val tomorrowMuseum = museum.copy(date = "2026-10-15", status = TripStopStatus.Upcoming)
        val source = listOf(completed, museum, garden, tomorrowMuseum)
        val tripDays = itinerary.toTripDayPlans(today, timelineStops = source)
        val homeStops = source.stopsOnDate(today)

        assertEquals(listOf("Completed", "Museum", "Garden"), homeStops.map { it.title })
        assertEquals(homeStops, tripDays.first().stops)
        assertEquals(listOf(tomorrowMuseum), tripDays[1].stops)
        assertEquals(listOf(completed, museum, garden), homeStops.take(3))
        assertEquals("Museum", source.nextNavigationStop(today)?.title)
        assertEquals(today.toString(), homeStops.last().date)
        assertEquals(listOf("08:00", "10:00", "12:00"), homeStops.map { it.time })
        assertEquals(listOf(completed, museum, garden, tomorrowMuseum), source)
    }

    @Test
    fun anEmptyGeneratedDayStaysEmptyWhileOtherDaysRemainVisible() {
        val itinerary = itinerary(day(1, "2026-10-14").copy(pois = emptyList()), day(2, "2026-10-15"))
        val generated = itinerary.toTripDayPlans(today).flatMap { it.stops }
        val tripDays = itinerary.toTripDayPlans(today, timelineStops = generated)

        assertEquals(2, tripDays.size)
        assertTrue(tripDays.first().stops.isEmpty())
        assertTrue(generated.stopsOnDate(today).isEmpty())
        assertEquals(listOf("Stop 2"), tripDays[1].stops.map { it.title })
        assertEquals("Stop 2", generated.nextNavigationStop(today)?.title)
    }

    @Test
    fun aFutureDayRetainsItsExistingTimesAndOrderWithoutBecomingCurrentToday() {
        val first = TripTimelineStop("09:00", "First", TripStopStatus.Upcoming, date = "2026-10-15")
        val second = first.copy(time = "11:00", title = "Second")
        val source = listOf(first, second)
        val displayed = itinerary(day(1, "2026-10-15"))
            .toTripDayPlans(today, timelineStops = source).single().stops

        assertEquals(source, displayed)
        assertEquals(listOf("09:00", "11:00"), displayed.map { it.time })
        assertTrue(displayed.all { it.date == "2026-10-15" && it.status == TripStopStatus.Upcoming })
        assertTrue(displayed.stopsOnDate(today).isEmpty())
    }

    @Test
    fun navigationPrefersTodaysCurrentStopOverOtherUpcomingStops() {
        val upcoming = TripTimelineStop("09:00", "Upcoming", TripStopStatus.Upcoming, date = today.toString())
        val current = upcoming.copy(time = "10:00", title = "Current", status = TripStopStatus.Current)
        assertEquals(current, listOf(upcoming, current).nextNavigationStop(today))
    }

    @Test
    fun navigationUsesTodaysFirstUpcomingStopWhenThereIsNoCurrentStop() {
        val upcoming = TripTimelineStop("09:00", "Next stop", TripStopStatus.Upcoming, date = today.toString())
        val laterDay = upcoming.copy(title = "Tomorrow", date = "2026-10-15")
        assertEquals(upcoming, listOf(laterDay, upcoming).nextNavigationStop(today))
    }

    @Test
    fun navigationSkipsPastCompletedAndUndatedStopsAndFindsTheNearestFutureDay() {
        val base = TripTimelineStop("09:00", "Stop", TripStopStatus.Upcoming)
        val tomorrow = base.copy(title = "Tomorrow", date = "2026-10-15")
        val stops = listOf(
            base.copy(title = "Past", date = "2026-10-13", status = TripStopStatus.Current),
            base.copy(title = "Completed", date = today.toString(), status = TripStopStatus.Done),
            base.copy(title = "Later", date = "2026-10-16"),
            base,
            tomorrow,
        )
        assertEquals(tomorrow, stops.nextNavigationStop(today))
    }

    @Test
    fun navigationHasNoDestinationWhenAllDatedStopsAreDoneOrInThePast() {
        val completed = TripTimelineStop("09:00", "Done", TripStopStatus.Done, date = today.toString())
        val past = completed.copy(date = "2026-10-13", status = TripStopStatus.Upcoming)
        assertNull(emptyList<TripTimelineStop>().nextNavigationStop(today))
        assertNull(listOf(completed, past).nextNavigationStop(today))
    }

    @Test
    fun weatherReplacementForOneDayPreservesThePlanForOtherDays() {
        val original = TripTimelineStop("09:00", "Original", TripStopStatus.Current, date = today.toString())
        val tomorrowStop = original.copy(title = "Tomorrow planned", date = "2026-10-15", status = TripStopStatus.Upcoming)
        val replacement = original.copy(title = "Updated")
        val updated = listOf(original, tomorrowStop).replaceTripDayStops(today.toString(), listOf(replacement))

        assertEquals(listOf(replacement, tomorrowStop), updated)
    }

    @Test
    fun changingTheOrderOfSameNamedPlacesIsDetectedByPoiId() {
        val firstDay = day(1, today.toString())
        val firstStop = firstDay.pois.single()
        val secondStop = firstStop.copy(
            poi = firstStop.poi.copy(id = "another-poi-with-the-same-name"),
            startTime = "11:00", endTime = "12:00",
        )
        val generatedDay = firstDay.copy(pois = listOf(firstStop, secondStop))
        val stops = itinerary(generatedDay).toTripDayPlans(today).single().stops
        assertTrue(generatedDay.matchesTimeline(stops))
        assertTrue(!generatedDay.matchesTimeline(stops.reversed()))
    }

    private fun itinerary(vararg days: DayItinerary) = Itinerary(
        tripId = "test-trip",
        days = days.toList(),
        totalEstimatedCost = 0.0,
        totalExpectedValue = 0.0,
    )

    private fun day(number: Int, date: String) = DayItinerary(
        dayNumber = number,
        date = date,
        pois = listOf(
            ItineraryPOI(
                poi = POI(
                    id = "stop-$number",
                    name = "Stop $number",
                    baseScore = 5.0,
                    category = POICategory.MUSEUM,
                    budgetLevel = BudgetLevel.MEDIUM,
                    environment = Environment.INDOOR,
                    coordinates = Coordinates(-37.81, 144.96),
                    recommendedVisitDuration = 60,
                    operatingHours = OperatingHours("09:00", "18:00"),
                    city = "Melbourne",
                ),
                startTime = "09:00",
                endTime = "10:00",
                travelTimeFromPrevious = 0,
                expectedValue = 5.0,
            ),
        ),
        totalDuration = 60,
        totalTravelTime = 0,
        totalVisitTime = 60,
    )
}
