package com.group5.roammate.ui.screens

import com.group5.roammate.logic.models.Itinerary
import com.group5.roammate.logic.models.DayItinerary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun Itinerary?.toTripDayPlans(
    today: LocalDate = LocalDate.now(),
    timelineStops: List<TripTimelineStop>? = null,
): List<TripDayPlan> = this?.days.orEmpty().map { day ->
    val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
    val dateText = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        null -> day.date
        else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
    }

    TripDayPlan(
        dayTitle = "Day ${day.dayNumber}",
        dateText = dateText,
        date = day.date,
        stops = timelineStops?.filter { it.date == day.date } ?: day.pois.mapIndexed { index, stop ->
            TripTimelineStop(
                time = stop.startTime,
                title = stop.poi.name,
                status = if (date == today && index == 0) {
                    TripStopStatus.Current
                } else {
                    TripStopStatus.Upcoming
                },
                date = day.date,
                poiId = stop.poi.id,
            )
        },
    )
}

internal fun List<TripTimelineStop>.stopsOnDate(
    date: LocalDate = LocalDate.now(),
): List<TripTimelineStop> = filter { it.date == date.toString() }

internal fun DayItinerary.matchesTimeline(stops: List<TripTimelineStop>): Boolean {
    val displayedIds = stops.filter { it.date == date }.map { stop ->
        stop.poiId ?: pois.singleOrNull { it.poi.name == stop.title }?.poi?.id
    }
    return displayedIds == pois.map { it.poi.id }
}

internal fun List<TripTimelineStop>.nextNavigationStop(
    today: LocalDate = LocalDate.now(),
): TripTimelineStop? {
    val remaining = filter { stop ->
        val date = stop.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        stop.status != TripStopStatus.Done && date != null && !date.isBefore(today)
    }
    val todayStops = remaining.stopsOnDate(today)
    return todayStops.firstOrNull { it.status == TripStopStatus.Current }
        ?: todayStops.firstOrNull()
        ?: remaining.sortedBy { it.date }.firstOrNull()
}

internal fun List<TripTimelineStop>.replaceTripDayStops(
    date: String,
    replacements: List<TripTimelineStop>,
): List<TripTimelineStop> {
    val otherDays = filterNot { it.date == date }
    return (otherDays + replacements.map { it.copy(date = date) }).sortedBy { it.date }
}
