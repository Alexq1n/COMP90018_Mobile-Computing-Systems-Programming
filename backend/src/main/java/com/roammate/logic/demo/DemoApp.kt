package com.roammate.logic.demo

import com.roammate.logic.TripPlannerEngine
import com.roammate.logic.models.*
import com.roammate.logic.utils.MockPoiRepository

/**
 * Demo application to showcase the Trip Planner Engine functionality
 */
fun main() {
    println("=".repeat(60))
    println("RoamMate Trip Planner Engine - Demo")
    println("=".repeat(60))

    // Initialize engine
    val poiRepository = MockPoiRepository()
    val tripPlanner = TripPlannerEngine(poiRepository)

    // Create user profile
    val userProfile = UserProfile(
        interests = listOf(POICategory.MUSEUM, POICategory.PARK, POICategory.LANDMARK),
        budgetPreference = BudgetLevel.MEDIUM,
        transportMode = TransportMode.WALKING
    )

    println("\n📋 User Profile:")
    println("   Interests: ${userProfile.interests.joinToString(", ")}")
    println("   Budget: ${userProfile.budgetPreference}")
    println("   Transport: ${userProfile.transportMode}")

    // Demo 1: Generate Initial Trip
    println("\n" + "=".repeat(60))
    println("Demo 1: Generate 3-Day Melbourne Itinerary")
    println("=".repeat(60))

    val itinerary = tripPlanner.generateInitialTrip(
        tripId = "trip_demo_001",
        destination = "Melbourne",
        startDate = "2026-09-15",
        numberOfDays = 3,
        userProfile = userProfile,
        startingLocation = Coordinates(-37.8136, 144.9631), // Melbourne CBD
        dailyStartTime = "09:00",
        dailyEndTime = "18:00",
        weatherStatus = WeatherStatus.SUNNY,
        includeFillers = true
    )

    println("\n✅ Generated Itinerary:")
    println("   Trip ID: ${itinerary.tripId}")
    println("   Total Days: ${itinerary.days.size}")
    println("   Total POIs: ${itinerary.days.sumOf { it.pois.size }}")
    println("   Total Expected Value: ${"%.2f".format(itinerary.totalExpectedValue)}")
    println("   Estimated Cost: $${"%.2f".format(itinerary.totalEstimatedCost)}")

    itinerary.days.forEach { day ->
        println("\n📅 Day ${day.dayNumber} (${day.date}):")
        println("   POIs: ${day.pois.size}")
        println("   Total Duration: ${day.totalDuration} min")
        println("   Travel Time: ${day.totalTravelTime} min")
        println("   Visit Time: ${day.totalVisitTime} min")

        day.pois.forEachIndexed { index, poi ->
            val fillerTag = if (poi.isFiller) " 💎 [Hidden Gem]" else ""
            println("   ${index + 1}. ${poi.poi.name}$fillerTag")
            println("      ${poi.startTime} - ${poi.endTime} (${poi.poi.recommendedVisitDuration} min)")
            println("      EV: ${"%.2f".format(poi.expectedValue)}, Travel: ${poi.travelTimeFromPrevious} min")
        }
    }

    // Demo 2: Dynamic Adjustment with Delay
    println("\n" + "=".repeat(60))
    println("Demo 2: Dynamic Adjustment (2-hour delay + rainy weather)")
    println("=".repeat(60))

    val firstDay = itinerary.days[0]
    val completedPOIs = if (firstDay.pois.isNotEmpty()) {
        listOf(firstDay.pois[0].poi.id)
    } else {
        emptyList()
    }

    val contextPayload = ContextPayload(
        currentLocation = Coordinates(-37.8200, 144.9700),
        currentTime = "2026-09-15T13:00:00",
        weatherStatus = WeatherStatus.RAINY,
        itineraryProgress = ItineraryProgress(
            completedPOIs = completedPOIs,
            delayMinutes = 120
        )
    )

    println("\n⚠️ Context Change:")
    println("   Current Time: 13:00 (2-hour delay)")
    println("   Weather: RAINY ☔")
    println("   Completed POIs: ${completedPOIs.size}")

    val adjustment = tripPlanner.adjustTripState(
        currentItinerary = firstDay.pois,
        contextPayload = contextPayload,
        destination = "Melbourne",
        userProfile = userProfile,
        dailyEndTime = "18:00"
    )

    println("\n🔄 Adjustment Options Generated:")

    // Option A
    println("\n📊 Option A (Experience First):")
    println("   Strategy: ${adjustment.optionA.strategy}")
    println("   Remaining POIs: ${adjustment.optionA.remainingItinerary.size}")
    println("   Total EV: ${"%.2f".format(adjustment.optionA.totalExpectedValue)}")
    println("   Travel Time: ${adjustment.optionA.totalTravelTime} min")
    println("   Dropped POIs: ${adjustment.optionA.droppedPOIs.size}")
    println("   Description: ${adjustment.optionA.description}")

    if (adjustment.optionA.remainingItinerary.isNotEmpty()) {
        println("\n   Remaining POIs:")
        adjustment.optionA.remainingItinerary.forEach { poi ->
            val envIcon = when (poi.poi.environment) {
                Environment.INDOOR -> "🏠"
                Environment.OUTDOOR -> "🌳"
                Environment.MIXED -> "🏢"
            }
            println("      - $envIcon ${poi.poi.name} (${poi.startTime}-${poi.endTime})")
        }
    }

    // Option B
    println("\n📊 Option B (Efficiency First):")
    println("   Strategy: ${adjustment.optionB.strategy}")
    println("   Remaining POIs: ${adjustment.optionB.remainingItinerary.size}")
    println("   Total EV: ${"%.2f".format(adjustment.optionB.totalExpectedValue)}")
    println("   Travel Time: ${adjustment.optionB.totalTravelTime} min")
    println("   Dropped POIs: ${adjustment.optionB.droppedPOIs.size}")
    println("   Description: ${adjustment.optionB.description}")

    if (adjustment.optionB.remainingItinerary.isNotEmpty()) {
        println("\n   Remaining POIs:")
        adjustment.optionB.remainingItinerary.forEach { poi ->
            val envIcon = when (poi.poi.environment) {
                Environment.INDOOR -> "🏠"
                Environment.OUTDOOR -> "🌳"
                Environment.MIXED -> "🏢"
            }
            println("      - $envIcon ${poi.poi.name} (${poi.startTime}-${poi.endTime})")
        }
    }

    // Demo 3: POI Value Calculation
    println("\n" + "=".repeat(60))
    println("Demo 3: POI Expected Value Calculation")
    println("=".repeat(60))

    val samplePOIs = poiRepository.getPOIsByCity("Melbourne").take(5)

    println("\n☀️ Sunny Weather:")
    samplePOIs.forEach { poi ->
        val ev = tripPlanner.calculatePOIValue(poi, userProfile, WeatherStatus.SUNNY)
        val envIcon = when (poi.environment) {
            Environment.INDOOR -> "🏠"
            Environment.OUTDOOR -> "🌳"
            Environment.MIXED -> "🏢"
        }
        println("   $envIcon ${poi.name}: EV = ${"%.2f".format(ev)} (Base: ${poi.baseScore})")
    }

    println("\n☔ Rainy Weather:")
    samplePOIs.forEach { poi ->
        val ev = tripPlanner.calculatePOIValue(poi, userProfile, WeatherStatus.RAINY)
        val evChange = tripPlanner.calculatePOIValue(poi, userProfile, WeatherStatus.SUNNY)
        val changePercent = ((ev - evChange) / evChange * 100)
        val envIcon = when (poi.environment) {
            Environment.INDOOR -> "🏠"
            Environment.OUTDOOR -> "🌳"
            Environment.MIXED -> "🏢"
        }
        println("   $envIcon ${poi.name}: EV = ${"%.2f".format(ev)} (${"%.0f".format(changePercent)}% change)")
    }

    // Summary
    println("\n" + "=".repeat(60))
    println("✅ Demo Complete!")
    println("=".repeat(60))
    println("\nKey Achievements:")
    println("   ✓ Multi-day itinerary generation")
    println("   ✓ Geographic clustering")
    println("   ✓ Dynamic adjustment with dual strategies")
    println("   ✓ Weather-aware EV calculation")
    println("   ✓ Hidden gems injection")
    println("   ✓ Rollover queue for high-value POIs")
    println("\n")
}
