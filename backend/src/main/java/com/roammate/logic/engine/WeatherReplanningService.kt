package com.roammate.logic.engine

import com.roammate.logic.models.*
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Service for handling weather-triggered replanning scenarios
 */
class WeatherReplanningService(
    private val userProfile: UserProfile,
    private val travelDurationMatrix: TravelDurationMatrix? = null
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Parse current weather from context payload
     */
    fun parseWeatherContext(contextPayload: ContextPayload): WeatherContext {
        val currentWeather = contextPayload.currentWeather ?: contextPayload.weatherStatus
        val timestamp = contextPayload.currentTime

        return WeatherContext(
            weatherStatus = currentWeather,
            timestamp = timestamp,
            isRainy = currentWeather == WeatherStatus.RAINY || currentWeather == WeatherStatus.STORMY
        )
    }

    /**
     * Check if today's itinerary contains outdoor activities
     */
    fun hasOutdoorActivities(dayItinerary: DayItinerary): Boolean {
        return dayItinerary.pois.any { itineraryPOI ->
            itineraryPOI.poi.environment == Environment.OUTDOOR ||
            itineraryPOI.poi.environment == Environment.MIXED
        }
    }

    /**
     * Detect if replanning should be triggered
     */
    fun shouldTriggerReplanning(
        contextPayload: ContextPayload,
        currentDayItinerary: DayItinerary
    ): Boolean {
        val weatherContext = parseWeatherContext(contextPayload)
        return weatherContext.isRainy && hasOutdoorActivities(currentDayItinerary)
    }

    /**
     * Generate three rainy day options for user selection
     */
    fun handleRainyDayScenario(
        currentItinerary: DayItinerary,
        contextPayload: ContextPayload,
        allPOIs: List<POI>,
        startLocation: Coordinates,
        availableTimeMinutes: Int
    ): RainyDayOptions {
        val weatherContext = parseWeatherContext(contextPayload)

        if (!weatherContext.isRainy) {
            return RainyDayOptions(
                original = currentItinerary,
                experienceFirst = currentItinerary,
                efficiencyFirst = currentItinerary,
                triggerReason = "Weather is not rainy, no replanning needed"
            )
        }

        if (!hasOutdoorActivities(currentItinerary)) {
            return RainyDayOptions(
                original = currentItinerary,
                experienceFirst = currentItinerary,
                efficiencyFirst = currentItinerary,
                triggerReason = "No outdoor activities scheduled, no replanning needed"
            )
        }

        // Option 1: Original itinerary unchanged
        val originalOption = currentItinerary

        // Get indoor POIs only (apply strong outdoor penalty)
        val indoorPOIs = allPOIs.filter {
            it.environment == Environment.INDOOR && !it.isFiller
        }

        // Calculate EV for indoor POIs with rainy weather context
        val poisWithEV = EVCalculator.calculateEVForPOIs(
            pois = indoorPOIs,
            userProfile = userProfile,
            weatherStatus = WeatherStatus.RAINY,
            currentTime = contextPayload.currentTime.substring(11, 16), // Extract HH:mm
            availableMinutes = availableTimeMinutes
        )

        // Option 2: Experience First strategy (maximize EV)
        val experienceFirstOption = generateExperienceFirstOption(
            poisWithEV = poisWithEV,
            startLocation = startLocation,
            currentTime = LocalTime.parse(currentItinerary.pois.firstOrNull()?.startTime ?: "09:00", timeFormatter),
            availableMinutes = availableTimeMinutes,
            dayNumber = currentItinerary.dayNumber,
            date = currentItinerary.date
        )

        // Option 3: Efficiency First strategy (maximize EV/Cost ratio)
        val efficiencyFirstOption = generateEfficiencyFirstOption(
            poisWithEV = poisWithEV,
            startLocation = startLocation,
            currentTime = LocalTime.parse(currentItinerary.pois.firstOrNull()?.startTime ?: "09:00", timeFormatter),
            availableMinutes = availableTimeMinutes,
            dayNumber = currentItinerary.dayNumber,
            date = currentItinerary.date
        )

        return RainyDayOptions(
            original = originalOption,
            experienceFirst = experienceFirstOption,
            efficiencyFirst = efficiencyFirstOption,
            triggerReason = "Rainy weather detected with ${currentItinerary.pois.count { it.poi.environment == Environment.OUTDOOR }} outdoor activities scheduled"
        )
    }

    /**
     * Generate Experience First alternative (maximize EV)
     */
    private fun generateExperienceFirstOption(
        poisWithEV: List<Pair<POI, Double>>,
        startLocation: Coordinates,
        currentTime: LocalTime,
        availableMinutes: Int,
        dayNumber: Int,
        date: String
    ): DayItinerary {
        // Sort by EV descending
        val sortedByEV = poisWithEV.sortedByDescending { it.second }

        val selectedPOIs = selectPOIsExperienceFirst(
            poisWithEV = sortedByEV,
            startLocation = startLocation,
            currentTime = currentTime,
            availableMinutes = availableMinutes
        )

        return createDayItinerary(selectedPOIs, dayNumber, date)
    }

    /**
     * Generate Efficiency First alternative (maximize EV/Cost ratio)
     */
    private fun generateEfficiencyFirstOption(
        poisWithEV: List<Pair<POI, Double>>,
        startLocation: Coordinates,
        currentTime: LocalTime,
        availableMinutes: Int,
        dayNumber: Int,
        date: String
    ): DayItinerary {
        val selectedPOIs = selectPOIsEfficiencyFirst(
            poisWithEV = poisWithEV,
            startLocation = startLocation,
            currentTime = currentTime,
            availableMinutes = availableMinutes
        )

        return createDayItinerary(selectedPOIs, dayNumber, date)
    }

    /**
     * Select POIs using Experience First strategy
     */
    private fun selectPOIsExperienceFirst(
        poisWithEV: List<Pair<POI, Double>>,
        startLocation: Coordinates,
        currentTime: LocalTime,
        availableMinutes: Int
    ): List<ItineraryPOI> {
        val selected = mutableListOf<ItineraryPOI>()
        val remaining = poisWithEV.toMutableList()
        var currentLocation = startLocation
        var currentLocationPoiId: String? = null
        var currentTimeObj = currentTime
        var remainingTime = availableMinutes

        while (remaining.isNotEmpty() && remainingTime > 0) {
            // Find the highest EV POI that we can still visit
            val candidate = remaining.firstOrNull { (poi, ev) ->
                val travelTime = calculateTravelTime(
                    currentLocation,
                    poi.coordinates,
                    currentLocationPoiId,
                    poi.id
                )
                val totalRequired = travelTime + poi.recommendedVisitDuration
                totalRequired <= remainingTime
            }

            if (candidate == null) break

            val (poi, ev) = candidate
            val travelTime = calculateTravelTime(
                currentLocation,
                poi.coordinates,
                currentLocationPoiId,
                poi.id
            )

            currentTimeObj = currentTimeObj.plusMinutes(travelTime.toLong())
            val startTimeStr = currentTimeObj.format(timeFormatter)
            currentTimeObj = currentTimeObj.plusMinutes(poi.recommendedVisitDuration.toLong())
            val endTimeStr = currentTimeObj.format(timeFormatter)

            selected.add(
                ItineraryPOI(
                    poi = poi,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    travelTimeFromPrevious = travelTime,
                    expectedValue = ev,
                    isFiller = false,
                    isUserPinned = false
                )
            )

            currentLocation = poi.coordinates
            currentLocationPoiId = poi.id
            remainingTime -= (travelTime + poi.recommendedVisitDuration)
            remaining.remove(candidate)
        }

        return selected
    }

    /**
     * Select POIs using Efficiency First strategy
     */
    private fun selectPOIsEfficiencyFirst(
        poisWithEV: List<Pair<POI, Double>>,
        startLocation: Coordinates,
        currentTime: LocalTime,
        availableMinutes: Int
    ): List<ItineraryPOI> {
        val selected = mutableListOf<ItineraryPOI>()
        val remaining = poisWithEV.toMutableList()
        var currentLocation = startLocation
        var currentLocationPoiId: String? = null
        var currentTimeObj = currentTime
        var remainingTime = availableMinutes

        while (remaining.isNotEmpty() && remainingTime > 0) {
            // Calculate EV/Cost ratio for each remaining POI
            val candidates = remaining.mapNotNull { (poi, ev) ->
                val travelTime = calculateTravelTime(
                    currentLocation,
                    poi.coordinates,
                    currentLocationPoiId,
                    poi.id
                )
                val totalRequired = travelTime + poi.recommendedVisitDuration

                if (totalRequired <= remainingTime) {
                    val ratio = if (travelTime > 0) ev / travelTime else ev * 100
                    Triple(poi, ev, Pair(travelTime, ratio))
                } else {
                    null
                }
            }

            if (candidates.isEmpty()) break

            // Select POI with highest EV/Cost ratio
            val (bestPOI, bestEV, times) = candidates.maxByOrNull { it.third.second }!!
            val (travelTime, _) = times

            currentTimeObj = currentTimeObj.plusMinutes(travelTime.toLong())
            val startTimeStr = currentTimeObj.format(timeFormatter)
            currentTimeObj = currentTimeObj.plusMinutes(bestPOI.recommendedVisitDuration.toLong())
            val endTimeStr = currentTimeObj.format(timeFormatter)

            selected.add(
                ItineraryPOI(
                    poi = bestPOI,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    travelTimeFromPrevious = travelTime,
                    expectedValue = bestEV,
                    isFiller = false,
                    isUserPinned = false
                )
            )

            currentLocation = bestPOI.coordinates
            currentLocationPoiId = bestPOI.id
            remainingTime -= (travelTime + bestPOI.recommendedVisitDuration)
            remaining.removeIf { it.first.id == bestPOI.id }
        }

        return selected
    }

    private fun calculateTravelTime(
        from: Coordinates,
        to: Coordinates,
        fromPoiId: String?,
        toPoiId: String
    ): Int {
        return if (fromPoiId != null && travelDurationMatrix != null) {
            TravelCostService.calculateAdjustedTravelTime(
                fromPoiId = fromPoiId,
                toPoiId = toPoiId,
                transportMode = userProfile.transportMode,
                transportPreference = userProfile.transportPreference,
                durationMatrix = travelDurationMatrix
            )
        } else {
            null
        } ?: TravelCostService.calculateTravelTime(
            fromLat = from.latitude,
            fromLon = from.longitude,
            toLat = to.latitude,
            toLon = to.longitude,
            transportMode = userProfile.transportMode
        )
    }

    private fun createDayItinerary(
        pois: List<ItineraryPOI>,
        dayNumber: Int,
        date: String
    ): DayItinerary {
        val totalDuration = if (pois.isNotEmpty()) {
            val firstPOI = pois.first()
            val lastPOI = pois.last()
            val startMinutes = timeToMinutes(firstPOI.startTime)
            val endMinutes = timeToMinutes(lastPOI.endTime)
            endMinutes - startMinutes
        } else {
            0
        }

        return DayItinerary(
            dayNumber = dayNumber,
            date = date,
            pois = pois,
            totalDuration = totalDuration,
            totalTravelTime = pois.sumOf { it.travelTimeFromPrevious },
            totalVisitTime = pois.sumOf { it.poi.recommendedVisitDuration }
        )
    }

    private fun timeToMinutes(timeStr: String): Int {
        val time = LocalTime.parse(timeStr, timeFormatter)
        return time.hour * 60 + time.minute
    }
}

/**
 * Weather context extracted from payload
 */
data class WeatherContext(
    val weatherStatus: WeatherStatus,
    val timestamp: String,
    val isRainy: Boolean
)

/**
 * Three options provided for rainy day scenario
 */
@Serializable
data class RainyDayOptions(
    val original: DayItinerary,
    val experienceFirst: DayItinerary,
    val efficiencyFirst: DayItinerary,
    val triggerReason: String
)
