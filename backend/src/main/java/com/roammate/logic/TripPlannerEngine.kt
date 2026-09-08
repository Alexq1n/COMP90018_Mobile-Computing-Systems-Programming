package com.roammate.logic

import com.roammate.logic.engine.*
import com.roammate.logic.interfaces.IPoiRepository
import com.roammate.logic.interfaces.ITripPlannerEngine
import com.roammate.logic.models.*

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
        includeFillers: Boolean
    ): Itinerary {
        // Get all POIs for destination
        val allPOIs = poiRepository.getPOIsByCity(destination)

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
            weatherStatus = weatherStatus
        )

        // Inject fillers if requested
        return if (includeFillers) {
            val fillerService = FillerService(poiRepository, userProfile)
            val enrichedDays = baseItinerary.days.map { dayItinerary ->
                fillerService.injectFillers(dayItinerary, destination, weatherStatus)
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
