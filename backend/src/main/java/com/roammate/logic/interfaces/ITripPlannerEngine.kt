package com.roammate.logic.interfaces

import com.roammate.logic.models.*

/**
 * Main interface for the Trip Planning Engine
 * Provides clean entry points for UI/ViewModel teams
 */
interface ITripPlannerEngine {

    /**
     * Generate initial multi-day trip itinerary
     *
     * @param tripId Unique identifier for the trip
     * @param destination City name (e.g., "Melbourne")
     * @param startDate Trip start date (format: "yyyy-MM-dd")
     * @param numberOfDays Number of days for the trip
     * @param userProfile User preferences and profile
     * @param startingLocation Starting coordinates (e.g., hotel location)
     * @param dailyStartTime Daily start time (default: "09:00")
     * @param dailyEndTime Daily end time (default: "18:00")
     * @param weatherStatus Initial weather status (default: SUNNY)
     * @param includeFillers Whether to inject hidden gems (default: true)
     * @return Complete itinerary with daily plans
     */
    fun generateInitialTrip(
        tripId: String,
        destination: String,
        startDate: String,
        numberOfDays: Int,
        userProfile: UserProfile,
        startingLocation: Coordinates,
        dailyStartTime: String = "09:00",
        dailyEndTime: String = "18:00",
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY,
        includeFillers: Boolean = true
    ): Itinerary

    /**
     * Adjust trip dynamically based on real-time context changes
     * Returns two strategic options for the user to choose from
     *
     * @param currentItinerary The current day's itinerary
     * @param contextPayload Real-time context (location, time, weather, delays)
     * @param destination City name
     * @param userProfile User preferences
     * @param dailyEndTime Daily end time (default: "18:00")
     * @return Adjustment response with Option A and Option B
     */
    fun adjustTripState(
        currentItinerary: List<ItineraryPOI>,
        contextPayload: ContextPayload,
        destination: String,
        userProfile: UserProfile,
        dailyEndTime: String = "18:00"
    ): AdjustmentResponse

    /**
     * Get all POIs for a destination
     *
     * @param destination City name
     * @param includeFillers Whether to include filler POIs (default: false)
     * @return List of POIs
     */
    fun getPOIsForDestination(
        destination: String,
        includeFillers: Boolean = false
    ): List<POI>

    /**
     * Calculate Expected Value for a specific POI
     *
     * @param poi The POI to evaluate
     * @param userProfile User preferences
     * @param weatherStatus Current weather
     * @return Calculated EV score
     */
    fun calculatePOIValue(
        poi: POI,
        userProfile: UserProfile,
        weatherStatus: WeatherStatus = WeatherStatus.SUNNY
    ): Double
}
