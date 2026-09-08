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
        private const val MIN_GAP_MINUTES = 20 // Minimum gap to consider for filler
        private const val FILLER_SEARCH_RADIUS_METERS = 500.0 // 500m radius for filler search
    }

    /**
     * Inject filler POIs into an existing day itinerary
     * Scans for time gaps > MIN_GAP_MINUTES and inserts suitable fillers
     */
    fun injectFillers(
        dayItinerary: DayItinerary,
        city: String,
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY
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
                    val filler = findBestFiller(
                        fillerPOIs = fillerPOIs,
                        nearLocation = currentPOI.poi.coordinates,
                        availableMinutes = gapMinutes,
                        currentTime = currentPOI.endTime,
                        weatherStatus = weatherStatus
                    )

                    if (filler != null) {
                        enrichedPOIs.add(filler)
                    }
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
        weatherStatus: WeatherStatus
    ): ItineraryPOI? {
        // Find fillers within search radius
        val nearbyFillers = poiRepository.getPOIsWithinRadius(
            latitude = nearLocation.latitude,
            longitude = nearLocation.longitude,
            radiusMeters = FILLER_SEARCH_RADIUS_METERS,
            city = fillerPOIs.firstOrNull()?.city ?: "Melbourne"
        ).filter { it.isFiller }

        if (nearbyFillers.isEmpty()) return null

        // Calculate EV for each filler
        val fillersWithEV = EVCalculator.calculateEVForPOIs(
            pois = nearbyFillers,
            userProfile = userProfile,
            weatherStatus = weatherStatus,
            currentTime = currentTime,
            availableMinutes = availableMinutes
        )

        if (fillersWithEV.isEmpty()) return null

        // Select filler with highest EV that fits in the gap
        val bestFiller = fillersWithEV
            .filter { (poi, _) ->
                val travelTime = TravelCostService.calculateTravelTime(
                    fromLat = nearLocation.latitude,
                    fromLon = nearLocation.longitude,
                    toLat = poi.coordinates.latitude,
                    toLon = poi.coordinates.longitude,
                    transportMode = userProfile.transportMode
                )
                val totalTime = travelTime + poi.recommendedVisitDuration
                totalTime <= availableMinutes
            }
            .maxByOrNull { it.second }

        if (bestFiller == null) return null

        val (poi, ev) = bestFiller

        // Calculate timing
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
