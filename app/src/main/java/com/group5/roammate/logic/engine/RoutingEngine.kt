package com.group5.roammate.logic.engine

import com.group5.roammate.logic.models.*
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
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY,
        pinnedPOIs: List<PinnedPOI> = emptyList()
    ): Itinerary {
        // Filter out filler POIs for main itinerary
        val mainPOIs = allPOIs.filter { !it.isFiller }

        // Separate restaurants from other POIs
        val restaurants = mainPOIs.filter { it.category == POICategory.RESTAURANT }
        val nonRestaurants = mainPOIs.filter { it.category != POICategory.RESTAURANT }

        // Create a map of pinned POIs by day number
        val pinnedPOIsByDay = pinnedPOIs.groupBy { it.dayNumber }
        val pinnedPOIIds = pinnedPOIs.map { it.poiId }.toSet()

        // Filter out pinned POIs from clustering (they'll be added separately per day)
        val poisToCluster = nonRestaurants.filter { it.id !in pinnedPOIIds }

        // Cluster non-restaurant POIs across days
        val dailyClusters = GeoClusteringService.distributeClustersAcrossDays(
            poisToCluster,
            numberOfDays
        )

        val dayItineraries = mutableListOf<DayItinerary>()
        val rolloverQueue = mutableListOf<POI>()
        val visitedPOIIds = mutableSetOf<String>()  // Track visited POIs across all days

        for (dayNumber in 1..numberOfDays) {
            val dayPOIs = if (dayNumber <= dailyClusters.size) {
                dailyClusters[dayNumber - 1]
            } else {
                emptyList()
            }

            // Add rollover POIs from previous day and ALL restaurants to each day
            // This ensures restaurants are available every day for lunch scheduling
            // BUT exclude POIs that have already been visited in previous days
            val candidatePOIs = (rolloverQueue + dayPOIs + restaurants)
                .filter { it.id !in visitedPOIIds }  // Exclude visited POIs
                .distinct()
            rolloverQueue.clear()

            // Get pinned POIs for this day
            val dayPinnedPOIs = pinnedPOIsByDay[dayNumber] ?: emptyList()

            val dayItinerary = generateDayItinerary(
                dayNumber = dayNumber,
                date = calculateDate(startDate, dayNumber - 1),
                candidatePOIs = candidatePOIs,
                startLocation = startingLocation,
                startTime = dailyStartTime,
                endTime = dailyEndTime,
                weatherStatus = weatherStatus,
                rolloverQueue = rolloverQueue,
                pinnedPOIs = dayPinnedPOIs,
                allPOIs = allPOIs
            )

            dayItineraries.add(dayItinerary)

            // Mark all POIs from this day as visited (including fillers)
            dayItinerary.pois.forEach { itineraryPOI ->
                visitedPOIIds.add(itineraryPOI.poi.id)
            }
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
        rolloverQueue: MutableList<POI>,
        pinnedPOIs: List<PinnedPOI> = emptyList(),
        allPOIs: List<POI> = emptyList()
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

        // Resolve pinned POIs to actual POI objects
        val pinnedPOIObjects = pinnedPOIs.mapNotNull { pinned ->
            allPOIs.find { it.id == pinned.poiId }?.let { poi ->
                Pair(poi, pinned)
            }
        }

        // Greedy routing: select POIs with best EV/Cost ratio
        val selectedPOIs = selectOptimalRoute(
            poisWithEV = poisWithEV,
            startLocation = startLocation,
            currentTime = startTimeObj,
            availableMinutes = availableMinutes,
            endTime = endTimeObj,
            rolloverQueue = rolloverQueue,
            pinnedPOIs = pinnedPOIObjects
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
     * Pinned POIs are scheduled first according to user preferences
     */
    private fun selectOptimalRoute(
        poisWithEV: List<Pair<POI, Double>>,
        startLocation: Coordinates,
        currentTime: LocalTime,
        availableMinutes: Int,
        endTime: LocalTime,
        rolloverQueue: MutableList<POI>,
        pinnedPOIs: List<Pair<POI, PinnedPOI>> = emptyList()
    ): List<ItineraryPOI> {
        val selected = mutableListOf<ItineraryPOI>()
        val remaining = poisWithEV.toMutableList()
        var currentLocation = startLocation
        var currentLocationPoiId: String? = null
        var currentTimeObj = currentTime
        var remainingTime = availableMinutes

        val maxPOIsPerDay = 5  // NEW: Limit to 5 POIs per day to create more gaps
        var restaurantScheduled = false  // Track if restaurant already scheduled

        // Step 1: Schedule pinned POIs first
        println("[RoutingEngine] Scheduling ${pinnedPOIs.size} pinned POIs")
        for ((poi, pinnedPOI) in pinnedPOIs.sortedBy { it.second.preferredStartTime ?: "00:00" }) {
            val ev = EVCalculator.calculateEV(poi, userProfile, WeatherStatus.SUNNY)

            // Calculate travel time to this POI
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

            val travelTime = TravelCostService.normalizeTravelTime(rawTravelTime)
            val totalTime = travelTime + poi.recommendedVisitDuration

            // Check if we have enough time
            if (totalTime > remainingTime) {
                println("  [RoutingEngine] [WARNING] Not enough time for pinned POI: ${poi.name}")
                continue
            }

            // Schedule this pinned POI
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
                    expectedValue = ev
                )
            )

            // Update state
            currentLocation = poi.coordinates
            currentLocationPoiId = poi.id
            remainingTime -= totalTime

            // Remove this POI from remaining candidates
            remaining.removeAll { it.first.id == poi.id }

            // Track restaurant if this is one
            if (poi.category == POICategory.RESTAURANT) {
                restaurantScheduled = true
            }

            println("  [RoutingEngine] [CHECK] Scheduled pinned POI: ${poi.name} at $startTimeStr")
        }

        // Step 2: Fill remaining time with optimal POIs using greedy algorithm

        while (remaining.isNotEmpty() && remainingTime > 0 && selected.size < maxPOIsPerDay && currentTimeObj < endTime) {
            // Check if we should prioritize scheduling a restaurant (11:00-14:00 lunch window)
            val lunchStartTime = LocalTime.of(11, 0)
            val lunchEndTime = LocalTime.of(14, 0)

            // Determine if we're in or approaching lunch window
            val inLunchWindow = currentTimeObj >= lunchStartTime && currentTimeObj < lunchEndTime
            val beforeLunchEnd = currentTimeObj < lunchEndTime

            // Force restaurant scheduling if:
            // 1. No restaurant scheduled yet
            // 2. We're before lunch end time (still have a chance to schedule)
            // 3. There are restaurants available
            val shouldPrioritizeRestaurant = !restaurantScheduled && beforeLunchEnd

            // If should prioritize restaurant, try restaurants first; otherwise exclude them from lunch window
            val eligiblePOIs = if (shouldPrioritizeRestaurant && inLunchWindow) {
                // In lunch window: ONLY consider restaurants if available
                val restaurants = remaining.filter { (poi, _) -> poi.category == POICategory.RESTAURANT }
                if (restaurants.isNotEmpty()) {
                    println("  [RoutingEngine] [RESTAURANT]  Forcing restaurant selection in lunch window (${currentTimeObj.format(timeFormatter)})")
                    restaurants
                } else {
                    // No restaurants available, skip to normal POIs
                    remaining.filter { (poi, _) -> poi.category != POICategory.RESTAURANT }
                }
            } else if (shouldPrioritizeRestaurant) {
                // Before lunch window: include all POIs but prioritize getting to lunch time
                remaining
            } else {
                // Restaurant already scheduled or past lunch window: exclude restaurants
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

                // Restaurant time window check: restaurants must start between 11:00-14:00
                val projectedStartTime = currentTimeObj.plusMinutes(travelTime.toLong())
                val passesRestaurantTimeCheck = if (poi.category == POICategory.RESTAURANT) {
                    projectedStartTime >= lunchStartTime && projectedStartTime < lunchEndTime
                } else {
                    true  // Non-restaurants always pass this check
                }

                passesRelativeCheck && passesAbsoluteCheck && passesRestaurantTimeCheck
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
