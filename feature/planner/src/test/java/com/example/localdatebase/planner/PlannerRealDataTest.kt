package com.example.localdatebase.planner

import com.example.localdatebase.database.PoiData
import com.example.localdatebase.database.PoiDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.roammate.logic.TripPlannerEngine
import com.roammate.logic.models.Coordinates
import com.roammate.logic.models.WeatherStatus
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class PlannerRealDataTest {
    @Test fun generatesTripFromBundledPoiData() {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream("pois.json"))
        val type = object : TypeToken<List<JsonPoi>>() {}.type
        val source: List<JsonPoi> = InputStreamReader(stream).use { Gson().fromJson(it, type) }
        val values = source.map {
            PoiData(
                it.id, it.name, it.baseScore, it.category, it.environment,
                it.coordinates.latitude, it.coordinates.longitude,
                it.recommendedVisitDuration, it.operatingHours.openTime,
                it.operatingHours.closeTime, it.isFiller, it.city,
                it.address.orEmpty(), it.description.orEmpty()
            )
        }
        val engine = TripPlannerEngine(LocalPoiRepository(FakeStore(values)))
        val trip = engine.generateInitialTrip(
            tripId = "real-data-test",
            destination = "Melbourne",
            startDate = "2026-10-10",
            numberOfDays = 1,
            userProfile = FirebasePlannerRepository.defaultProfile(),
            startingLocation = Coordinates(-37.8136, 144.9631),
            dailyStartTime = "09:00",
            dailyEndTime = "18:00",
            weatherStatus = WeatherStatus.SUNNY,
            includeFillers = true
        )
        assertTrue(trip.days.single().pois.isNotEmpty())
    }

    private data class JsonPoi(
        val id: String,
        val name: String,
        val baseScore: Double,
        val category: String,
        val environment: String,
        val coordinates: JsonCoordinates,
        val recommendedVisitDuration: Int,
        val operatingHours: JsonHours,
        val isFiller: Boolean,
        val city: String,
        val address: String?,
        val description: String?
    )
    private data class JsonCoordinates(val latitude: Double, val longitude: Double)
    private data class JsonHours(val openTime: String, val closeTime: String)

    private class FakeStore(private val values: List<PoiData>) : PoiDataStore {
        override fun upsertPois(pois: MutableList<PoiData>?) = Unit
        override fun getPoiById(id: String?): PoiData? = values.firstOrNull { it.id == id }
        override fun getAllPois(): MutableList<PoiData> = values.toMutableList()
        override fun getPois(category: String?, city: String?, search: String?, limit: Int): MutableList<PoiData> =
            values.filter {
                (category.isNullOrBlank() || it.category.equals(category, true)) &&
                    (city.isNullOrBlank() || it.city.equals(city, true))
            }.take(limit).toMutableList()
        override fun countPois(): Int = values.size
    }
}
