package com.roammate.logic.engine

import com.roammate.logic.models.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Core routing engine for generating optimal daily itineraries
 * Uses greedy heuristic to maximize EV within time constraints
 */
class RoutingEngine(
    private val userProfile: UserProfile
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
     * Select optimal route using greedy heuristic
     */
    private fun selectOptimalRoute(
        poisWithEV: List<Pair<POI, Double>>,
        startLocation: Coordinates,
        currentTime: LocalTime,
        availableMinutes: Int,
        rolloverQueue: MutableList<POI>
    ): List<ItineraryPOI> {
        val selected = mutableListOf<ItineraryPOI>()
        val remaining = poisWithEV.toMutableList()
        var currentLocation = startLocation
        var currentTimeObj = currentTime
        var remainingTime = availableMinutes

        while (remaining.isNotEmpty() && remainingTime > 0) {
            // Calculate travel time and EV/Cost ratio for each remaining POI
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
                // Only consider POIs we can reach and visit
                val (travelTime, _) = times
                val totalRequired = travelTime + poi.recommendedVisitDuration
                totalRequired <= remainingTime
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

            // Update state
            currentLocation = bestPOI.coordinates
            remainingTime -= (travelTime + bestPOI.recommendedVisitDuration)
            remaining.removeIf { it.first.id == bestPOI.id }
        }

        // Add high-value unselected POIs to rollover queue
        val highValueThreshold = 8.0
        remaining
            .filter { it.first.baseScore >= highValueThreshold }
            .forEach { rolloverQueue.add(it.first) }

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
