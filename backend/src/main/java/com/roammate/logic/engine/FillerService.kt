package com.roammate.logic.engine

import com.roammate.logic.interfaces.IPoiRepository
import com.roammate.logic.models.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Service for injecting filler POIs (Hidden Gems) into existing itineraries
 * Identifies time gaps and inserts micro-POIs along the route
 */
class FillerService(
    private val poiRepository: IPoiRepository,
    private val userProfile: UserProfile
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    companion object {
        private const val MIN_GAP_MINUTES = 5 // Minimum gap to consider for filler (lowered to 5 min)
        private const val FILLER_SEARCH_RADIUS_METERS = 5000.0 // 5km radius for filler search (expanded to 5km for better coverage)
        private const val MAX_FILLER_DURATION = 60 // Only consider fillers that take <= 60 minutes
    }

    /**
     * Inject filler POIs into an existing day itinerary
     * Scans for time gaps > MIN_GAP_MINUTES and inserts suitable fillers
     */
    fun injectFillers(
        dayItinerary: DayItinerary,
        city: String,
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY,
        dailyEndTime: String = "18:00"
    ): DayItinerary {
        if (dayItinerary.pois.isEmpty()) return dayItinerary

        val enrichedPOIs = mutableListOf<ItineraryPOI>()
        val fillerPOIs = poiRepository.getFillerPOIs(city)

        for (i in dayItinerary.pois.indices) {
            val currentPOI = dayItinerary.pois[i]
            enrichedPOIs.add(currentPOI)

            // Check if there's a next POI to calculate gap
            if (i < dayItinerary.pois.size - 1) {
                val nextPOI = dayItinerary.pois[i + 1]

                val currentEndTime = LocalTime.parse(currentPOI.endTime, timeFormatter)
                val nextStartTime = LocalTime.parse(nextPOI.startTime, timeFormatter)

                val gapMinutes = java.time.Duration.between(currentEndTime, nextStartTime).toMinutes().toInt()

                // If gap is significant, try to insert a filler
                if (gapMinutes >= MIN_GAP_MINUTES) {
                    println("  [FillerService] Gap detected: $gapMinutes min between ${currentPOI.poi.name} and ${nextPOI.poi.name}")

                    // Calculate ORIGINAL (non-normalized) travel time to next POI
                    // This represents the actual travel time, not the normalized one
                    val originalTravelTime = TravelCostService.calculateTravelTime(
                        fromLat = currentPOI.poi.coordinates.latitude,
                        fromLon = currentPOI.poi.coordinates.longitude,
                        toLat = nextPOI.poi.coordinates.latitude,
                        toLon = nextPOI.poi.coordinates.longitude,
                        transportMode = userProfile.transportMode
                    )

                    // The actual available time for filler is: gap - originalTravelTime
                    // Because the gap includes the normalized travel time, but we only need original time
                    val actualAvailableMinutes = gapMinutes - originalTravelTime

                    println("  [FillerService] Gap breakdown: total=$gapMinutes min, original travel=$originalTravelTime min, available for filler=$actualAvailableMinutes min")

                    // Check if filler would exceed daily end time
                    val maxEndTime = LocalTime.parse(dailyEndTime, timeFormatter)
                    val effectiveGap = if (nextStartTime > maxEndTime) {
                        // Gap extends beyond daily end time, limit it
                        val timeUntilEnd = java.time.Duration.between(currentEndTime, maxEndTime).toMinutes().toInt()
                        minOf(actualAvailableMinutes, timeUntilEnd)
                    } else {
                        actualAvailableMinutes
                    }

                    if (effectiveGap < MIN_GAP_MINUTES) {
                        println("  [FillerService] Available time too small: $effectiveGap min (need >= $MIN_GAP_MINUTES min)")
                    } else {
                        val filler = findBestFiller(
                            fillerPOIs = fillerPOIs,
                            nearLocation = currentPOI.poi.coordinates,
                            availableMinutes = effectiveGap,
                            currentTime = currentPOI.endTime,
                            weatherStatus = weatherStatus,
                            dailyEndTime = dailyEndTime
                        )

                        if (filler != null) {
                            println("  [FillerService] ✓ Injected filler: ${filler.poi.name}")
                            enrichedPOIs.add(filler)
                        } else {
                            println("  [FillerService] ✗ No suitable filler found")
                        }
                    }
                } else if (gapMinutes > 0) {
                    println("  [FillerService] Gap too small: $gapMinutes min (need >= $MIN_GAP_MINUTES min)")
                }
            }
        }

        // Recalculate totals
        val totalDuration = if (enrichedPOIs.isNotEmpty()) {
            val firstPOI = enrichedPOIs.first()
            val lastPOI = enrichedPOIs.last()
            timeToMinutes(lastPOI.endTime) - timeToMinutes(firstPOI.startTime)
        } else {
            0
        }

        val totalTravelTime = enrichedPOIs.sumOf { it.travelTimeFromPrevious }
        val totalVisitTime = enrichedPOIs.sumOf { it.poi.recommendedVisitDuration }

        return dayItinerary.copy(
            pois = enrichedPOIs,
            totalDuration = totalDuration,
            totalTravelTime = totalTravelTime,
            totalVisitTime = totalVisitTime
        )
    }

    /**
     * Find the best filler POI within radius that fits the time gap
     */
    private fun findBestFiller(
        fillerPOIs: List<POI>,
        nearLocation: Coordinates,
        availableMinutes: Int,
        currentTime: String,
        weatherStatus: WeatherStatus,
        dailyEndTime: String
    ): ItineraryPOI? {
        // Find fillers within search radius (exclude restaurants)
        val nearbyFillers = poiRepository.getPOIsWithinRadius(
            latitude = nearLocation.latitude,
            longitude = nearLocation.longitude,
            radiusMeters = FILLER_SEARCH_RADIUS_METERS,
            city = fillerPOIs.firstOrNull()?.city ?: "Melbourne"
        ).filter { it.isFiller && it.recommendedVisitDuration <= MAX_FILLER_DURATION && it.category != POICategory.RESTAURANT }

        println("    [FillerService] Found ${nearbyFillers.size} fillers within ${FILLER_SEARCH_RADIUS_METERS/1000}km (duration <= ${MAX_FILLER_DURATION}min)")

        if (nearbyFillers.isEmpty()) return null

        // Calculate EV for each filler - use more lenient filtering for fillers
        val fillersWithEV = nearbyFillers.map { poi ->
            val ev = EVCalculator.calculateEV(poi, userProfile, weatherStatus)
            poi to ev
        }.filter { it.second > 0 }  // Only keep positive EV, but don't filter by prune logic

        println("    [FillerService] ${fillersWithEV.size}/${nearbyFillers.size} fillers have positive EV")

        if (fillersWithEV.isEmpty()) {
            println("    [FillerService] No fillers with positive EV")
            return null
        }

        // Select filler with highest EV that fits in the gap
        val candidatesWithTime = fillersWithEV.map { (poi, ev) ->
            // For fillers, use raw travel time WITHOUT normalization
            // Fillers should be flexible to fit small gaps
            val travelTime = TravelCostService.calculateTravelTime(
                fromLat = nearLocation.latitude,
                fromLon = nearLocation.longitude,
                toLat = poi.coordinates.latitude,
                toLon = poi.coordinates.longitude,
                transportMode = userProfile.transportMode
            )
            val totalTime = travelTime + poi.recommendedVisitDuration
            Triple(poi, ev, totalTime)
        }

        val fittingCandidates = candidatesWithTime.filter { it.third <= availableMinutes }
        println("    [FillerService] ${fittingCandidates.size}/${candidatesWithTime.size} fillers fit in ${availableMinutes} min")

        val bestFiller = fittingCandidates
            .maxByOrNull { it.second }
            ?.let { it.first to it.second }

        if (bestFiller == null) {
            println("    [FillerService] No filler fits the time constraint")
            return null
        }

        val (poi, ev) = bestFiller

        // Calculate timing - for fillers, use raw travel time WITHOUT normalization
        val travelTime = TravelCostService.calculateTravelTime(
            fromLat = nearLocation.latitude,
            fromLon = nearLocation.longitude,
            toLat = poi.coordinates.latitude,
            toLon = poi.coordinates.longitude,
            transportMode = userProfile.transportMode
        )

        val currentTimeObj = LocalTime.parse(currentTime, timeFormatter)
        val startTimeObj = currentTimeObj.plusMinutes(travelTime.toLong())
        val endTimeObj = startTimeObj.plusMinutes(poi.recommendedVisitDuration.toLong())

        // Final check: ensure filler doesn't exceed daily end time
        val maxEndTime = LocalTime.parse(dailyEndTime, timeFormatter)
        if (endTimeObj > maxEndTime) {
            println("    [FillerService] Filler ${poi.name} would end at ${endTimeObj.format(timeFormatter)}, exceeds daily end time $dailyEndTime")
            return null
        }

        return ItineraryPOI(
            poi = poi,
            startTime = startTimeObj.format(timeFormatter),
            endTime = endTimeObj.format(timeFormatter),
            travelTimeFromPrevious = travelTime,
            expectedValue = ev,
            isFiller = true
        )
    }

    /**
     * Convert time string to minutes since midnight
     */
    private fun timeToMinutes(timeStr: String): Int {
        val time = LocalTime.parse(timeStr, timeFormatter)
        return time.hour * 60 + time.minute
    }
}
