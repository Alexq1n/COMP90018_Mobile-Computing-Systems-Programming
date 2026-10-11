package com.roammate.logic.engine

import com.roammate.logic.models.*
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Service for user-editable itinerary operations
 */
class ItineraryEditService(
    private val userProfile: UserProfile,
    private val travelDurationMatrix: TravelDurationMatrix? = null
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Add a POI to a specific day at a specific position
     */
    fun addPoiToItinerary(
        itinerary: Itinerary,
        poi: POI,
        targetDayIndex: Int,
        insertPosition: Int,
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY
    ): EditResult {
        if (targetDayIndex < 0 || targetDayIndex >= itinerary.days.size) {
            return EditResult(
                success = false,
                validationMessage = "Invalid day index: $targetDayIndex",
                updatedDay = null,
                freedTimeMinutes = 0
            )
        }

        val targetDay = itinerary.days[targetDayIndex]
        val existingPOIs = targetDay.pois.toMutableList()

        if (insertPosition < 0 || insertPosition > existingPOIs.size) {
            return EditResult(
                success = false,
                validationMessage = "Invalid insert position: $insertPosition",
                updatedDay = null,
                freedTimeMinutes = 0
            )
        }

        // Calculate EV for the new POI
        val ev = EVCalculator.calculateEV(
            poi = poi,
            userProfile = userProfile,
            weatherStatus = weatherStatus
        )

        // Validate operating hours and calculate start time
        val startTime = if (existingPOIs.isEmpty()) {
            LocalTime.parse("09:00", timeFormatter)
        } else if (insertPosition == 0) {
            // Inserting at the beginning - need to shift everything
            LocalTime.parse("09:00", timeFormatter)
        } else {
            // Inserting after previous POI
            LocalTime.parse(existingPOIs[insertPosition - 1].endTime, timeFormatter)
        }

        // Check if POI can fit
        val nextPOIStartTime = if (insertPosition < existingPOIs.size) {
            LocalTime.parse(existingPOIs[insertPosition].startTime, timeFormatter)
        } else {
            LocalTime.parse("18:00", timeFormatter)
        }

        val previousLocation = if (insertPosition == 0) {
            null // No previous location for first position
        } else {
            existingPOIs[insertPosition - 1].poi.coordinates
        }

        val travelTime = if (previousLocation != null) {
            calculateTravelTime(previousLocation, poi.coordinates, existingPOIs.getOrNull(insertPosition - 1)?.poi?.id)
        } else {
            0
        }

        val requiredTime = travelTime + poi.recommendedVisitDuration
        val availableTime = if (nextPOIStartTime.isAfter(startTime)) {
            java.time.Duration.between(startTime, nextPOIStartTime).toMinutes().toInt()
        } else {
            0
        }

        if (requiredTime > availableTime) {
            return EditResult(
                success = false,
                validationMessage = "Not enough time to add POI. Required: $requiredTime min, Available: $availableTime min",
                updatedDay = null,
                freedTimeMinutes = 0
            )
        }

        // Create new POI entry
        val newStartTime = startTime.plusMinutes(travelTime.toLong())
        val newEndTime = newStartTime.plusMinutes(poi.recommendedVisitDuration.toLong())

        val newItineraryPOI = ItineraryPOI(
            poi = poi,
            startTime = newStartTime.format(timeFormatter),
            endTime = newEndTime.format(timeFormatter),
            travelTimeFromPrevious = travelTime,
            expectedValue = ev,
            isFiller = false,
            isUserPinned = true
        )

        // Insert the POI
        existingPOIs.add(insertPosition, newItineraryPOI)

        // Recalculate times for subsequent POIs
        recalculateTimes(existingPOIs, insertPosition + 1)

        // Create updated day
        val updatedDay = targetDay.copy(
            pois = existingPOIs,
            totalTravelTime = existingPOIs.sumOf { it.travelTimeFromPrevious },
            totalVisitTime = existingPOIs.sumOf { it.poi.recommendedVisitDuration }
        )

        return EditResult(
            success = true,
            validationMessage = "POI added successfully",
            updatedDay = updatedDay,
            freedTimeMinutes = 0
        )
    }

    /**
     * Remove a POI from the itinerary
     */
    fun removePoiFromItinerary(
        itinerary: Itinerary,
        poiId: String,
        dayIndex: Int
    ): EditResult {
        if (dayIndex < 0 || dayIndex >= itinerary.days.size) {
            return EditResult(
                success = false,
                validationMessage = "Invalid day index: $dayIndex",
                updatedDay = null,
                freedTimeMinutes = 0
            )
        }

        val targetDay = itinerary.days[dayIndex]
        val existingPOIs = targetDay.pois.toMutableList()
        val poiIndex = existingPOIs.indexOfFirst { it.poi.id == poiId }

        if (poiIndex == -1) {
            return EditResult(
                success = false,
                validationMessage = "POI not found in day $dayIndex",
                updatedDay = null,
                freedTimeMinutes = 0
            )
        }

        val removedPOI = existingPOIs[poiIndex]
        val freedTime = removedPOI.travelTimeFromPrevious + removedPOI.poi.recommendedVisitDuration

        // Remove the POI
        existingPOIs.removeAt(poiIndex)

        // Recalculate travel time for next POI if exists
        if (poiIndex < existingPOIs.size && existingPOIs.isNotEmpty()) {
            val previousLocation = if (poiIndex > 0) {
                existingPOIs[poiIndex - 1].poi.coordinates
            } else {
                null
            }

            val newTravelTime = if (previousLocation != null) {
                calculateTravelTime(
                    previousLocation,
                    existingPOIs[poiIndex].poi.coordinates,
                    existingPOIs.getOrNull(poiIndex - 1)?.poi?.id
                )
            } else {
                0
            }

            existingPOIs[poiIndex] = existingPOIs[poiIndex].copy(
                travelTimeFromPrevious = newTravelTime
            )
        }

        // Recalculate times for subsequent POIs
        recalculateTimes(existingPOIs, poiIndex)

        val updatedDay = targetDay.copy(
            pois = existingPOIs,
            totalTravelTime = existingPOIs.sumOf { it.travelTimeFromPrevious },
            totalVisitTime = existingPOIs.sumOf { it.poi.recommendedVisitDuration }
        )

        return EditResult(
            success = true,
            validationMessage = "POI removed successfully",
            updatedDay = updatedDay,
            freedTimeMinutes = freedTime
        )
    }

    /**
     * Reoptimize a day's itinerary after manual edits
     */
    fun reoptimizeDay(
        dayItinerary: DayItinerary,
        allPOIs: List<POI>,
        availableTimeMinutes: Int,
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY
    ): DayItinerary {
        // Separate user-pinned and regular POIs
        val pinnedPOIs = dayItinerary.pois.filter { it.isUserPinned }
        val regularPOIs = dayItinerary.pois.filter { !it.isUserPinned }

        // If no pinned POIs, keep existing regular POIs
        if (pinnedPOIs.isEmpty()) {
            // Just return the existing day with regularPOIs
            if (regularPOIs.isEmpty()) {
                // Try to add new POIs
                val candidatePOIs = allPOIs.filter { !it.isFiller }
                val poisWithEV = EVCalculator.calculateEVForPOIs(
                    pois = candidatePOIs,
                    userProfile = userProfile,
                    weatherStatus = weatherStatus,
                    currentTime = "09:00",
                    availableMinutes = availableTimeMinutes
                )

                if (poisWithEV.isEmpty()) {
                    return dayItinerary
                }

                val optimizedPOIs = selectOptimalPOIsForReoptimization(
                    poisWithEV = poisWithEV,
                    startLocation = Coordinates(0.0, 0.0),
                    startTime = LocalTime.parse("09:00", timeFormatter),
                    availableMinutes = availableTimeMinutes
                )

                return dayItinerary.copy(
                    pois = optimizedPOIs,
                    totalTravelTime = optimizedPOIs.sumOf { it.travelTimeFromPrevious },
                    totalVisitTime = optimizedPOIs.sumOf { it.poi.recommendedVisitDuration }
                )
            }

            // Keep existing regular POIs
            return dayItinerary.copy(
                pois = regularPOIs,
                totalTravelTime = regularPOIs.sumOf { it.travelTimeFromPrevious },
                totalVisitTime = regularPOIs.sumOf { it.poi.recommendedVisitDuration }
            )
        }

        // Calculate time used by pinned POIs
        val pinnedTime = pinnedPOIs.sumOf {
            it.travelTimeFromPrevious + it.poi.recommendedVisitDuration
        }

        val remainingTime = availableTimeMinutes - pinnedTime

        if (remainingTime <= 0) {
            // No time left for optimization, return only pinned POIs
            return dayItinerary.copy(
                pois = pinnedPOIs,
                totalTravelTime = pinnedPOIs.sumOf { it.travelTimeFromPrevious },
                totalVisitTime = pinnedPOIs.sumOf { it.poi.recommendedVisitDuration }
            )
        }

        // Get candidate POIs (excluding already scheduled ones)
        val scheduledIds = dayItinerary.pois.map { it.poi.id }.toSet()
        val candidatePOIs = allPOIs.filter { it.id !in scheduledIds && !it.isFiller }

        // Calculate EV for candidates
        val poisWithEV = EVCalculator.calculateEVForPOIs(
            pois = candidatePOIs,
            userProfile = userProfile,
            weatherStatus = weatherStatus,
            currentTime = "09:00",
            availableMinutes = remainingTime
        )

        // Use routing engine to select additional POIs
        val lastPinnedLocation = pinnedPOIs.lastOrNull()?.poi?.coordinates
            ?: Coordinates(0.0, 0.0)

        val optimizedPOIs = selectOptimalPOIsForReoptimization(
            poisWithEV = poisWithEV,
            startLocation = lastPinnedLocation,
            startTime = LocalTime.parse(pinnedPOIs.lastOrNull()?.endTime ?: "09:00", timeFormatter),
            availableMinutes = remainingTime
        )

        // Merge pinned and optimized POIs, then sort by start time
        val finalPOIs = (pinnedPOIs + optimizedPOIs).sortedBy {
            LocalTime.parse(it.startTime, timeFormatter)
        }

        return dayItinerary.copy(
            pois = finalPOIs,
            totalTravelTime = finalPOIs.sumOf { it.travelTimeFromPrevious },
            totalVisitTime = finalPOIs.sumOf { it.poi.recommendedVisitDuration }
        )
    }

    /**
     * Unified interface for editing itinerary
     */
    fun editItinerary(
        itinerary: Itinerary,
        action: EditAction,
        params: EditParams,
        allPOIs: List<POI> = emptyList(),
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY
    ): EditResult {
        return when (action) {
            EditAction.ADD_POI -> {
                val poi = allPOIs.find { it.id == params.poiId }
                    ?: return EditResult(
                        success = false,
                        validationMessage = "POI not found: ${params.poiId}",
                        updatedDay = null,
                        freedTimeMinutes = 0
                    )
                addPoiToItinerary(
                    itinerary = itinerary,
                    poi = poi,
                    targetDayIndex = params.dayIndex,
                    insertPosition = params.insertPosition ?: 0,
                    weatherStatus = weatherStatus
                )
            }
            EditAction.REMOVE_POI -> {
                removePoiFromItinerary(
                    itinerary = itinerary,
                    poiId = params.poiId ?: "",
                    dayIndex = params.dayIndex
                )
            }
            EditAction.REOPTIMIZE -> {
                val dayToOptimize = itinerary.days.getOrNull(params.dayIndex)
                    ?: return EditResult(
                        success = false,
                        validationMessage = "Invalid day index",
                        updatedDay = null,
                        freedTimeMinutes = 0
                    )
                val optimizedDay = reoptimizeDay(
                    dayItinerary = dayToOptimize,
                    allPOIs = allPOIs,
                    availableTimeMinutes = params.availableTimeMinutes ?: 540,
                    weatherStatus = weatherStatus
                )
                EditResult(
                    success = true,
                    validationMessage = "Day reoptimized successfully",
                    updatedDay = optimizedDay,
                    freedTimeMinutes = 0
                )
            }
        }
    }

    private fun calculateTravelTime(from: Coordinates, to: Coordinates, fromPoiId: String?): Int {
        return if (fromPoiId != null && travelDurationMatrix != null) {
            // Try to find POI ID for destination
            // This is simplified - in real implementation, we'd need a POI lookup
            0 // Fallback for now
        } else {
            TravelCostService.calculateTravelTime(
                fromLat = from.latitude,
                fromLon = from.longitude,
                toLat = to.latitude,
                toLon = to.longitude,
                transportMode = userProfile.transportMode
            )
        }
    }

    private fun recalculateTimes(pois: MutableList<ItineraryPOI>, startIndex: Int) {
        if (startIndex >= pois.size) return

        for (i in startIndex until pois.size) {
            val previousEndTime = if (i > 0) {
                LocalTime.parse(pois[i - 1].endTime, timeFormatter)
            } else {
                LocalTime.parse("09:00", timeFormatter)
            }

            val travelTime = pois[i].travelTimeFromPrevious
            val newStartTime = previousEndTime.plusMinutes(travelTime.toLong())
            val newEndTime = newStartTime.plusMinutes(pois[i].poi.recommendedVisitDuration.toLong())

            pois[i] = pois[i].copy(
                startTime = newStartTime.format(timeFormatter),
                endTime = newEndTime.format(timeFormatter)
            )
        }
    }

    private fun selectOptimalPOIsForReoptimization(
        poisWithEV: List<Pair<POI, Double>>,
        startLocation: Coordinates,
        startTime: LocalTime,
        availableMinutes: Int
    ): List<ItineraryPOI> {
        val selected = mutableListOf<ItineraryPOI>()
        val remaining = poisWithEV.toMutableList()
        var currentLocation = startLocation
        var currentTimeObj = startTime
        var remainingTime = availableMinutes

        while (remaining.isNotEmpty() && remainingTime > 0) {
            val candidates = remaining.map { (poi, ev) ->
                val travelTime = TravelCostService.calculateTravelTime(
                    fromLat = currentLocation.latitude,
                    fromLon = currentLocation.longitude,
                    toLat = poi.coordinates.latitude,
                    toLon = poi.coordinates.longitude,
                    transportMode = userProfile.transportMode
                )

                val totalTime = travelTime + poi.recommendedVisitDuration
                val ratio = if (travelTime > 0) ev / travelTime else ev * 100

                Triple(poi, ev, Pair(travelTime, ratio))
            }.filter { (poi, _, times) ->
                val (travelTime, _) = times
                val totalRequired = travelTime + poi.recommendedVisitDuration
                totalRequired <= remainingTime
            }

            if (candidates.isEmpty()) break

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
            remainingTime -= (travelTime + bestPOI.recommendedVisitDuration)
            remaining.removeIf { it.first.id == bestPOI.id }
        }

        return selected
    }
}

/**
 * Result of an itinerary edit operation
 */
@Serializable
data class EditResult(
    val success: Boolean,
    val validationMessage: String,
    val updatedDay: DayItinerary?,
    val freedTimeMinutes: Int
)

/**
 * Available edit actions
 */
enum class EditAction {
    ADD_POI,
    REMOVE_POI,
    REOPTIMIZE
}

/**
 * Parameters for edit operations
 */
data class EditParams(
    val poiId: String? = null,
    val dayIndex: Int,
    val insertPosition: Int? = null,
    val availableTimeMinutes: Int? = null
)
