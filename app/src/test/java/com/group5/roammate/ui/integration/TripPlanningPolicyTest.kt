package com.group5.roammate.ui.integration

import com.group5.roammate.logic.TripPlannerEngine
import com.group5.roammate.logic.interfaces.IPoiRepository
import com.group5.roammate.logic.models.BudgetLevel
import com.group5.roammate.logic.models.Coordinates
import com.group5.roammate.logic.models.Environment
import com.group5.roammate.logic.models.Itinerary
import com.group5.roammate.logic.models.OperatingHours
import com.group5.roammate.logic.models.POI
import com.group5.roammate.logic.models.POICategory
import com.group5.roammate.logic.models.TransportMode
import com.group5.roammate.logic.models.UserProfile
import com.group5.roammate.logic.models.WeatherStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlanningPolicyTest {
    private val firstPoi = POI(
        id = "shot-tower",
        name = "Shot Tower Museum",
        baseScore = 7.0,
        category = POICategory.MUSEUM,
        budgetLevel = BudgetLevel.MEDIUM,
        environment = Environment.INDOOR,
        coordinates = Coordinates(-37.810453, 144.9631448),
        recommendedVisitDuration = 120,
        operatingHours = OperatingHours("09:00", "18:00"),
        city = "Melbourne",
    )
    private val pois = listOf(
        firstPoi,
        firstPoi.copy(
            id = "hellenic",
            name = "Hellenic Museum",
            coordinates = Coordinates(-37.812171, 144.957084),
            operatingHours = OperatingHours("10:00", "16:00"),
        ),
    )
    private val profile = UserProfile(
        interests = listOf(POICategory.MUSEUM),
        budgetPreference = BudgetLevel.MEDIUM,
        transportMode = TransportMode.PUBLIC_TRANSIT,
    )
    private val engine = TripPlannerEngine(object : IPoiRepository {
        override fun getPOIsByCity(city: String) = pois.filter { it.city == city && !it.isFiller }
        override fun getPOIsByCategory(city: String, category: POICategory) =
            getPOIsByCity(city).filter { it.category == category }
        override fun getFillerPOIs(city: String) = emptyList<POI>()
        override fun getPOIsWithinRadius(
            latitude: Double,
            longitude: Double,
            radiusMeters: Double,
            city: String,
        ) = emptyList<POI>()
        override fun getPOIById(id: String) = pois.find { it.id == id }
    })

    @Test
    fun planningOriginUsesAnAttractionInsideTheDestination() {
        assertEquals(firstPoi.coordinates, destinationPlanningOrigin(pois))
    }

    @Test
    fun planningOriginPrefersMainAttractionsOverFillers() {
        val filler = firstPoi.copy(id = "filler", isFiller = true, coordinates = Coordinates(-37.9, 145.0))
        assertEquals(firstPoi.coordinates, destinationPlanningOrigin(listOf(filler) + pois))
    }

    @Test
    fun planningOriginIsNotHardcodedToMelbourne() {
        val sydneyPoi = firstPoi.copy(city = "Sydney", coordinates = Coordinates(-33.8568, 151.2153))
        assertEquals(sydneyPoi.coordinates, destinationPlanningOrigin(listOf(sydneyPoi)))
    }

    @Test
    fun unsupportedDestinationFailsWithoutInventingAttractions() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            destinationPlanningOrigin(emptyList())
        }
        assertTrue(error.message.orEmpty().contains("No attractions"))
    }

    @Test
    fun fillersAloneDoNotCreateAnInitialPlan() {
        assertThrows(IllegalArgumentException::class.java) {
            destinationPlanningOrigin(listOf(firstPoi.copy(isFiller = true)))
        }
    }

    @Test
    fun destinationPlanningFixesTheRemoteGpsEmptyTripRegression() {
        val farGps = Coordinates(37.4219983, -122.084)
        assertTrue(generate(farGps).days.all { it.pois.isEmpty() })

        val planned = generate(destinationPlanningOrigin(pois)).requireScheduledStops()
        assertTrue(planned.days.all { it.pois.isNotEmpty() })
    }

    @Test
    fun futureTripCanBeGeneratedWithoutGpsOrAKnownWeatherForecast() {
        val planned = generate(
            origin = destinationPlanningOrigin(pois),
            startDate = "2026-12-07",
            weather = WeatherStatus.CLOUDY,
        ).requireScheduledStops()

        assertEquals(listOf("2026-12-07", "2026-12-08"), planned.days.map { it.date })
        assertTrue(planned.days.all { it.pois.isNotEmpty() })
    }

    @Test
    fun allEmptyDaysAreNotAcceptedAsASuccessfulGeneration() {
        val empty = generate(Coordinates(37.4219983, -122.084))
        val error = assertThrows(IllegalArgumentException::class.java) {
            empty.requireScheduledStops()
        }
        assertTrue(error.message.orEmpty().contains("No stops fit"))
    }

    @Test
    fun itineraryWithoutDaysIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Itinerary("empty", emptyList(), 0.0, 0.0).requireScheduledStops()
        }
    }

    @Test
    fun oneEmptyDayDoesNotDiscardAValidPlanForAnotherDay() {
        val planned = generate(destinationPlanningOrigin(pois))
        val partlyEmpty = planned.copy(days = planned.days.mapIndexed { index, day ->
            if (index == 0) day.copy(pois = emptyList()) else day
        })
        assertSame(partlyEmpty, partlyEmpty.requireScheduledStops())
    }

    @Test
    fun noAvailableVisitingTimeDoesNotBecomeAnEmptySuccess() {
        val empty = generate(destinationPlanningOrigin(pois), startTime = "18:00")
        assertThrows(IllegalArgumentException::class.java) {
            empty.requireScheduledStops()
        }
    }

    private fun generate(
        origin: Coordinates,
        startDate: String = "2026-10-14",
        startTime: String = "10:00",
        weather: WeatherStatus = WeatherStatus.SUNNY,
    ) = engine.generateInitialTrip(
        tripId = "planned-trip",
        destination = "Melbourne",
        startDate = startDate,
        numberOfDays = 2,
        userProfile = profile,
        startingLocation = origin,
        dailyStartTime = startTime,
        dailyEndTime = "18:00",
        weatherStatus = weather,
        includeFillers = true,
        pinnedPOIs = emptyList(),
    )
}
