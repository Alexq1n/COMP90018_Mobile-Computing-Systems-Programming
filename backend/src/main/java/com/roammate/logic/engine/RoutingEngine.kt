package com.roammate.logic.engine

import com.roammate.logic.models.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Core routing engine for generating optimal daily itineraries
 * Uses greedy heuristic to maximize EV within time constraints
 */
class RoutingEngine(
    private val userProfile: UserProfile,
    private val travelDurationMatrix: TravelDurationMatrix? = null
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Generate a complete multi-day itinerary
     */
    fun generateItinerary(
        tripId: String,
        startDate: String, // "yyyy-MM-dd"
        numberOfDays: Int,
        allPOIs: List<POI>,
        startingLocation: Coordinates,
        dailyStartTime: String = "09:00",
        dailyEndTime: String = "18:00",
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY
    ): Itinerary {
        // Filter out filler POIs for main itinerary
        val mainPOIs = allPOIs.filter { !it.isFiller }

        // Cluster POIs across days
        val dailyClusters = GeoClusteringService.distributeClustersAcrossDays(
            mainPOIs,
            numberOfDays
        )

        val dayItineraries = mutableListOf<DayItinerary>()
        val rolloverQueue = mutableListOf<POI>()

        for (dayNumber in 1..numberOfDays) {
            val dayPOIs = if (dayNumber <= dailyClusters.size) {
                dailyClusters[dayNumber - 1]
            } else {
                emptyList()
            }

            // Add rollover POIs from previous day
            val candidatePOIs = (rolloverQueue + dayPOIs).distinct()
            rolloverQueue.clear()

            val dayItinerary = generateDayItinerary(
                dayNumber = dayNumber,
                date = calculateDate(startDate, dayNumber - 1),
                candidatePOIs = candidatePOIs,
                startLocation = startingLocation,
                startTime = dailyStartTime,
                endTime = dailyEndTime,
                weatherStatus = weatherStatus,
                rolloverQueue = rolloverQueue
            )

            dayItineraries.add(dayItinerary)
        }

        val totalCost = dayItineraries.sumOf { day ->
            day.pois.sumOf { it.poi.budgetLevel.ordinal * 10.0 } // Approximate cost
        }

        val totalEV = dayItineraries.sumOf { day ->
            day.pois.sumOf { it.expectedValue }
        }

        return Itinerary(
            tripId = tripId,
            days = dayItineraries,
            totalEstimatedCost = totalCost,
            totalExpectedValue = totalEV
        )
    }

    /**
     * Generate itinerary for a single day
     */
    fun generateDayItinerary(
        dayNumber: Int,
        date: String,
        candidatePOIs: List<POI>,
        startLocation: Coordinates,
        startTime: String,
        endTime: String,
        weatherStatus: WeatherStatus,
        rolloverQueue: MutableList<POI>
    ): DayItinerary {
        val startTimeObj = LocalTime.parse(startTime, timeFormatter)
        val endTimeObj = LocalTime.parse(endTime, timeFormatter)
        val availableMinutes = java.time.Duration.between(startTimeObj, endTimeObj).toMinutes().toInt()

        // Calculate EV for all candidate POIs
        val poisWithEV = EVCalculator.calculateEVForPOIs(
            pois = candidatePOIs,
            userProfile = userProfile,
            weatherStatus = weatherStatus,
            currentTime = startTime,
            availableMinutes = availableMinutes
        )

        // Greedy routing: select POIs with best EV/Cost ratio
        val selectedPOIs = selectOptimalRoute(
            poisWithEV = poisWithEV,
            startLocation = startLocation,
            currentTime = startTimeObj,
            availableMinutes = availableMinutes,
            endTime = endTimeObj,
            rolloverQueue = rolloverQueue
        )

        val totalDuration = if (selectedPOIs.isNotEmpty()) {
            val firstPOI = selectedPOIs.first()
            val lastPOI = selectedPOIs.last()
            val startMinutes = timeToMinutes(firstPOI.startTime)
            val endMinutes = timeToMinutes(lastPOI.endTime)
            endMinutes - startMinutes
        } else {
            0
        }

        val totalTravelTime = selectedPOIs.sumOf { it.travelTimeFromPrevious }
        val totalVisitTime = selectedPOIs.sumOf { it.poi.recommendedVisitDuration }

        return DayItinerary(
            dayNumber = dayNumber,
            date = date,
            pois = selectedPOIs,
            totalDuration = totalDuration,
            totalTravelTime = totalTravelTime,
            totalVisitTime = totalVisitTime
        )
    }

    /**
     * Select optimal route using greedy heuristic with preference-adjusted costs
     * Special handling for restaurants: only one per day, scheduled between 11:00-14:00
     */
    private fun selectOptimalRoute(
        poisWithEV: List<Pair<POI, Double>>,
        startLocation: Coordinates,
        currentTime: LocalTime,
        availableMinutes: Int,
        endTime: LocalTime,
        rolloverQueue: MutableList<POI>
    ): List<ItineraryPOI> {
        val selected = mutableListOf<ItineraryPOI>()
        val remaining = poisWithEV.toMutableList()
        var currentLocation = startLocation
        var currentLocationPoiId: String? = null
        var currentTimeObj = currentTime
        var remainingTime = availableMinutes

        val maxPOIsPerDay = 5  // NEW: Limit to 5 POIs per day to create more gaps
        var restaurantScheduled = false  // Track if restaurant already scheduled

        while (remaining.isNotEmpty() && remainingTime > 0 && selected.size < maxPOIsPerDay && currentTimeObj < endTime) {
            // Check if we should prioritize scheduling a restaurant (11:00-14:00 lunch window)
            val lunchStartTime = LocalTime.of(11, 0)
            val lunchEndTime = LocalTime.of(14, 0)
            val shouldScheduleRestaurant = !restaurantScheduled &&
                                          currentTimeObj >= lunchStartTime &&
                                          currentTimeObj < lunchEndTime

            // If in lunch window and no restaurant scheduled yet, try to prioritize restaurant
            val eligiblePOIs = if (shouldScheduleRestaurant) {
                // First try to get restaurants only
                val restaurants = remaining.filter { (poi, _) -> poi.category == POICategory.RESTAURANT }
                if (restaurants.isNotEmpty()) {
                    restaurants  // Prioritize restaurants during lunch time
                } else {
                    // No restaurants available, fall back to normal POIs
                    remaining.filter { (poi, _) -> poi.category != POICategory.RESTAURANT }
                }
            } else {
                // Outside lunch window or restaurant already scheduled: exclude restaurants
                remaining.filter { (poi, _) -> poi.category != POICategory.RESTAURANT }
            }

            if (eligiblePOIs.isEmpty()) break

            // Calculate travel time and EV/Cost ratio for each remaining POI
            val candidates = eligiblePOIs.map { (poi, ev) ->
                // Try to use real duration data with preference adjustment first
                val rawTravelTime = if (currentLocationPoiId != null && travelDurationMatrix != null) {
                    TravelCostService.calculateAdjustedTravelTime(
                        fromPoiId = currentLocationPoiId!!,
                        toPoiId = poi.id,
                        transportMode = userProfile.transportMode,
                        transportPreference = userProfile.transportPreference,
                        durationMatrix = travelDurationMatrix
                    )
                } else {
                    null
                } ?: TravelCostService.calculateTravelTime(
                    fromLat = currentLocation.latitude,
                    fromLon = currentLocation.longitude,
                    toLat = poi.coordinates.latitude,
                    toLon = poi.coordinates.longitude,
                    transportMode = userProfile.transportMode
                )

                // Apply normalization: <30min -> 30min, else round up to 30min multiple
                val travelTime = TravelCostService.normalizeTravelTime(rawTravelTime)

                val totalTime = travelTime + poi.recommendedVisitDuration
                val ratio = if (travelTime > 0) ev / travelTime else ev * 100

                Triple(poi, ev, Pair(travelTime, ratio))
            }.filter { (poi, _, times) ->
                // Only consider POIs we can reach and visit
                val (travelTime, _) = times
                val totalRequired = travelTime + poi.recommendedVisitDuration

                // Relative time check
                val passesRelativeCheck = totalRequired <= remainingTime

                // Absolute time check: ensure POI won't exceed daily end time
                val projectedEndTime = currentTimeObj.plusMinutes(totalRequired.toLong())
                val passesAbsoluteCheck = projectedEndTime <= endTime

                passesRelativeCheck && passesAbsoluteCheck
            }

            if (candidates.isEmpty()) break

            // Select POI with highest EV/Cost ratio
            val (bestPOI, bestEV, times) = candidates.maxByOrNull { it.third.second }!!
            val (travelTime, _) = times

            // Calculate times
            currentTimeObj = currentTimeObj.plusMinutes(travelTime.toLong())
            val startTimeStr = currentTimeObj.format(timeFormatter)
            currentTimeObj = currentTimeObj.plusMinutes(bestPOI.recommendedVisitDuration.toLong())
            val endTimeStr = currentTimeObj.format(timeFormatter)

            // Safety check: verify we haven't exceeded daily end time
            if (currentTimeObj > endTime) {
                println("  [RoutingEngine] POI ${bestPOI.name} would end at $endTimeStr, exceeds daily end time ${endTime.format(timeFormatter)}")
                break
            }

            selected.add(
                ItineraryPOI(
                    poi = bestPOI,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    travelTimeFromPrevious = travelTime,
                    expectedValue = bestEV,
                    isFiller = false
                )
            )

            println("  [RoutingEngine] Selected POI: ${bestPOI.name}, ${startTimeStr}-${endTimeStr}, travel: ${travelTime}min")

            // Update state
            if (bestPOI.category == POICategory.RESTAURANT) {
                restaurantScheduled = true
            }
            currentLocation = bestPOI.coordinates
            currentLocationPoiId = bestPOI.id
            remainingTime -= (travelTime + bestPOI.recommendedVisitDuration)
            remaining.removeIf { it.first.id == bestPOI.id }
        }

        // Add high-value unselected POIs to rollover queue
        val highValueThreshold = 9.0  // Increased from 8.0 to 9.0 - only truly exceptional POIs rollover
        remaining
            .filter { it.first.baseScore >= highValueThreshold }
            .forEach { rolloverQueue.add(it.first) }

        println("  [RoutingEngine] Rollover: ${rolloverQueue.size} POIs (threshold >= $highValueThreshold)")

        return selected
    }

    /**
     * Calculate date by adding days to start date
     */
    private fun calculateDate(startDate: String, daysToAdd: Int): String {
        // Simple date calculation (format: yyyy-MM-dd)
        val parts = startDate.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        // Simplified - doesn't handle month/year boundaries perfectly
        val newDay = day + daysToAdd
        return String.format("%04d-%02d-%02d", year, month, newDay)
    }

    /**
     * Convert time string to minutes since midnight
     */
    private fun timeToMinutes(timeStr: String): Int {
        val time = LocalTime.parse(timeStr, timeFormatter)
        return time.hour * 60 + time.minute
    }
}
