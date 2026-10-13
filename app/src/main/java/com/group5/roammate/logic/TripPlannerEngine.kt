package com.group5.roammate.logic

import com.group5.roammate.logic.engine.*
import com.group5.roammate.logic.interfaces.IPoiRepository
import com.group5.roammate.logic.interfaces.ITripPlannerEngine
import com.group5.roammate.logic.models.*

/**
 * Facade implementation of the Trip Planner Engine
 * Orchestrates all subsystems to provide a unified API
 */
class TripPlannerEngine(
    private val poiRepository: IPoiRepository
) : ITripPlannerEngine {

    override fun generateInitialTrip(
        tripId: String,
        destination: String,
        startDate: String,
        numberOfDays: Int,
        userProfile: UserProfile,
        startingLocation: Coordinates,
        dailyStartTime: String,
        dailyEndTime: String,
        weatherStatus: WeatherStatus,
        includeFillers: Boolean,
        pinnedPOIs: List<PinnedPOI>
    ): Itinerary {
        println("[TripPlannerEngine] generateInitialTrip called with ${pinnedPOIs.size} pinned POIs")
        pinnedPOIs.forEach { pinned ->
            println("  - PinnedPOI: poiId=${pinned.poiId}")
        }

        // Get all POIs for destination
        var allPOIs = poiRepository.getPOIsByCity(destination)
        println("[TripPlannerEngine] Retrieved ${allPOIs.size} POIs for destination='$destination'")

        // CRITICAL FIX: Ensure all pinned POIs are included in allPOIs
        // If a pinned POI is not in allPOIs, try to find it by ID across all cities
        val missingPinnedPOIIds = pinnedPOIs.map { it.poiId }.filter { pinnedId ->
            allPOIs.none { poi -> poi.id == pinnedId }
        }

        if (missingPinnedPOIIds.isNotEmpty()) {
            println("[TripPlannerEngine] WARNING: ${missingPinnedPOIIds.size} pinned POIs not found in destination='$destination'")
            println("[TripPlannerEngine] Trying to fetch missing POIs by ID...")

            // Try to get missing POIs from all available POIs
            val additionalPOIs = missingPinnedPOIIds.mapNotNull { poiId ->
                poiRepository.getPOIById(poiId)?.also {
                    println("[TripPlannerEngine] Found missing POI: ${it.name} (city=${it.city})")
                }
            }

            // Add missing POIs to allPOIs
            allPOIs = allPOIs + additionalPOIs
            println("[TripPlannerEngine] Total POIs after adding missing: ${allPOIs.size}")
        }

        // Generate baseline itinerary using routing engine
        val routingEngine = RoutingEngine(userProfile)
        val baseItinerary = routingEngine.generateItinerary(
            tripId = tripId,
            startDate = startDate,
            numberOfDays = numberOfDays,
            allPOIs = allPOIs,
            startingLocation = startingLocation,
            dailyStartTime = dailyStartTime,
            dailyEndTime = dailyEndTime,
            weatherStatus = weatherStatus,
            pinnedPOIs = pinnedPOIs
        )

        // Inject fillers if requested
        return if (includeFillers) {
            val fillerService = FillerService(poiRepository, userProfile)
            val visitedPOIIds = mutableSetOf<String>()

            val enrichedDays = baseItinerary.days.map { dayItinerary ->
                // Inject fillers for this day, excluding already visited POIs
                val enrichedDay = fillerService.injectFillers(
                    dayItinerary,
                    destination,
                    weatherStatus,
                    dailyEndTime,
                    visitedPOIIds
                )

                // Mark all POIs from this day as visited (including newly added fillers)
                enrichedDay.pois.forEach { itineraryPOI ->
                    visitedPOIIds.add(itineraryPOI.poi.id)
                }

                enrichedDay
            }

            baseItinerary.copy(days = enrichedDays)
        } else {
            baseItinerary
        }
    }

    override fun adjustTripState(
        currentItinerary: List<ItineraryPOI>,
        contextPayload: ContextPayload,
        destination: String,
        userProfile: UserProfile,
        dailyEndTime: String
    ): AdjustmentResponse {
        // Get all POIs for potential re-routing
        val allPOIs = poiRepository.getPOIsByCity(destination)

        // Use dynamic adjustment engine
        val adjustmentEngine = DynamicAdjustmentEngine(userProfile)
        return adjustmentEngine.adjustItinerary(
            currentItinerary = currentItinerary,
            contextPayload = contextPayload,
            allPOIs = allPOIs,
            dailyEndTime = dailyEndTime
        )
    }

    override fun getPOIsForDestination(
        destination: String,
        includeFillers: Boolean
    ): List<POI> {
        return if (includeFillers) {
            poiRepository.getPOIsByCity(destination) + poiRepository.getFillerPOIs(destination)
        } else {
            poiRepository.getPOIsByCity(destination)
        }
    }

    override fun calculatePOIValue(
        poi: POI,
        userProfile: UserProfile,
        weatherStatus: WeatherStatus
    ): Double {
        return EVCalculator.calculateEV(poi, userProfile, weatherStatus)
    }
}
