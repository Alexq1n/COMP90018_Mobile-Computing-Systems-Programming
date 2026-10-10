package com.roammate.logic.engine

import com.roammate.logic.models.*
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Service for detecting user fatigue and adjusting itinerary accordingly
 */
class FatigueDetectionService(
    private val userProfile: UserProfile,
    private val travelDurationMatrix: TravelDurationMatrix? = null
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Fatigue thresholds based on steps per hour
    private companion object {
        const val LOW_ACTIVITY_THRESHOLD = 500
        const val HIGH_FATIGUE_THRESHOLD = 5000
        const val MEDIUM_FATIGUE_THRESHOLD = 3000
    }

    /**
     * Detect fatigue level based on recent step count
     */
    fun detectFatigue(stepCount: Int): FatigueLevel {
        return when {
            stepCount < LOW_ACTIVITY_THRESHOLD -> FatigueLevel.LOW
            stepCount in LOW_ACTIVITY_THRESHOLD until MEDIUM_FATIGUE_THRESHOLD -> FatigueLevel.MEDIUM
            stepCount >= HIGH_FATIGUE_THRESHOLD -> FatigueLevel.HIGH
            else -> FatigueLevel.MEDIUM
        }
    }

    /**
     * Analyze context payload and determine fatigue status
     */
    fun analyzeFatigueStatus(contextPayload: ContextPayload): FatigueStatus {
        val stepCount = contextPayload.recentStepCount ?: return FatigueStatus(
            level = FatigueLevel.LOW,
            recommendedAction = "No step data available",
            adjustedItinerary = null
        )

        val fatigueLevel = detectFatigue(stepCount)

        val recommendedAction = when (fatigueLevel) {
            FatigueLevel.LOW -> "Normal activity level detected. Continue with current plan."
            FatigueLevel.MEDIUM -> "Moderate activity detected. Consider shorter visits or rest breaks."
            FatigueLevel.HIGH -> "High fatigue detected. Recommend reducing intensity and prioritizing nearby, low-effort activities."
        }

        return FatigueStatus(
            level = fatigueLevel,
            recommendedAction = recommendedAction,
            adjustedItinerary = null
        )
    }

    /**
     * Adjust remaining itinerary based on fatigue level
     */
    fun adjustItineraryForFatigue(
        remainingItinerary: List<ItineraryPOI>,
        allPOIs: List<POI>,
        fatigueLevel: FatigueLevel,
        currentLocation: Coordinates,
        availableMinutes: Int,
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY,
        dayNumber: Int,
        date: String
    ): DayItinerary {
        if (fatigueLevel == FatigueLevel.LOW) {
            // No adjustment needed
            return createDayItinerary(remainingItinerary, dayNumber, date)
        }

        // Get unscheduled candidate POIs
        val scheduledIds = remainingItinerary.map { it.poi.id }.toSet()
        val candidatePOIs = allPOIs.filter { it.id !in scheduledIds && !it.isFiller }

        // Apply fatigue-based filtering and EV adjustment
        val adjustedCandidates = filterAndAdjustForFatigue(
            pois = candidatePOIs,
            fatigueLevel = fatigueLevel,
            weatherStatus = weatherStatus
        )

        // Calculate EV for adjusted candidates
        val poisWithEV = EVCalculator.calculateEVForPOIs(
            pois = adjustedCandidates,
            userProfile = userProfile,
            weatherStatus = weatherStatus,
            currentTime = "09:00",
            availableMinutes = availableMinutes
        )

        // Apply additional fatigue penalty to EV
        val fatigueAdjustedEV = applyFatiguePenalty(poisWithEV, fatigueLevel)

        // Select POIs with fatigue consideration
        val selectedPOIs = selectPOIsWithFatigueConsideration(
            poisWithEV = fatigueAdjustedEV,
            startLocation = currentLocation,
            currentTime = LocalTime.parse(remainingItinerary.firstOrNull()?.startTime ?: "09:00", timeFormatter),
            availableMinutes = availableMinutes,
            fatigueLevel = fatigueLevel
        )

        return createDayItinerary(selectedPOIs, dayNumber, date)
    }

    /**
     * Filter and adjust POI list based on fatigue level
     */
    private fun filterAndAdjustForFatigue(
        pois: List<POI>,
        fatigueLevel: FatigueLevel,
        weatherStatus: WeatherStatus
    ): List<POI> {
        return when (fatigueLevel) {
            FatigueLevel.LOW -> pois
            FatigueLevel.MEDIUM -> {
                // Prefer shorter duration POIs and indoor activities
                pois.filter { poi ->
                    poi.recommendedVisitDuration <= 90 // Max 1.5 hours
                }
            }
            FatigueLevel.HIGH -> {
                // Strong preference for short, nearby, low-intensity POIs
                pois.filter { poi ->
                    poi.recommendedVisitDuration <= 60 && // Max 1 hour
                    (poi.environment == Environment.INDOOR ||
                     poi.category == POICategory.CAFE ||
                     poi.category == POICategory.RESTAURANT)
                }
            }
        }
    }

    /**
     * Apply fatigue penalty to EV scores
     */
    private fun applyFatiguePenalty(
        poisWithEV: List<Pair<POI, Double>>,
        fatigueLevel: FatigueLevel
    ): List<Pair<POI, Double>> {
        val penaltyMultiplier = when (fatigueLevel) {
            FatigueLevel.LOW -> 1.0
            FatigueLevel.MEDIUM -> 0.8
            FatigueLevel.HIGH -> 0.5
        }

        return poisWithEV.map { (poi, ev) ->
            val adjustedEV = if (poi.recommendedVisitDuration > 90) {
                // Apply stronger penalty to long-duration POIs
                ev * penaltyMultiplier * 0.7
            } else if (poi.environment == Environment.INDOOR ||
                       poi.category == POICategory.CAFE ||
                       poi.category == POICategory.RESTAURANT) {
                // Boost low-intensity POIs
                ev * penaltyMultiplier * 1.3
            } else {
                ev * penaltyMultiplier
            }

            Pair(poi, adjustedEV)
        }
    }

    /**
     * Select POIs considering fatigue level
     */
    private fun selectPOIsWithFatigueConsideration(
        poisWithEV: List<Pair<POI, Double>>,
        startLocation: Coordinates,
        currentTime: LocalTime,
        availableMinutes: Int,
        fatigueLevel: FatigueLevel
    ): List<ItineraryPOI> {
        val selected = mutableListOf<ItineraryPOI>()
        val remaining = poisWithEV.toMutableList()
        var currentLocation = startLocation
        var currentLocationPoiId: String? = null
        var currentTimeObj = currentTime
        var remainingTime = availableMinutes

        // Limit total POI count based on fatigue
        val maxPOIs = when (fatigueLevel) {
            FatigueLevel.LOW -> Int.MAX_VALUE
            FatigueLevel.MEDIUM -> 4
            FatigueLevel.HIGH -> 2
        }

        while (remaining.isNotEmpty() && remainingTime > 0 && selected.size < maxPOIs) {
            // Prioritize nearby POIs when fatigued
            val candidates = remaining.mapNotNull { (poi, ev) ->
                val travelTime = calculateTravelTime(
                    currentLocation,
                    poi.coordinates,
                    currentLocationPoiId,
                    poi.id
                )
                val totalRequired = travelTime + poi.recommendedVisitDuration

                if (totalRequired <= remainingTime) {
                    // Apply extra penalty for long travel when fatigued
                    val travelPenalty = if (fatigueLevel == FatigueLevel.HIGH && travelTime > 15) {
                        0.5
                    } else if (fatigueLevel == FatigueLevel.MEDIUM && travelTime > 20) {
                        0.7
                    } else {
                        1.0
                    }

                    val adjustedEV = ev * travelPenalty
                    val ratio = if (travelTime > 0) adjustedEV / travelTime else adjustedEV * 100

                    Triple(poi, adjustedEV, Pair(travelTime, ratio))
                } else {
                    null
                }
            }

            if (candidates.isEmpty()) break

            // Select POI with highest adjusted EV/Cost ratio
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

            // Add rest break suggestion for high fatigue
            if (fatigueLevel == FatigueLevel.HIGH && selected.size > 0) {
                remainingTime -= 15 // Implicit 15-minute rest breaks
            }
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
 * Fatigue level enumeration
 */
@Serializable
enum class FatigueLevel {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Fatigue status result
 */
@Serializable
data class FatigueStatus(
    val level: FatigueLevel,
    val recommendedAction: String,
    val adjustedItinerary: DayItinerary?
)
