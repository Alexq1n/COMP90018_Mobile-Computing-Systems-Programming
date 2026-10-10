package com.example.localdatebase.planner

import android.content.Context
import com.example.localdatebase.database.LocalDatabaseProvider
import com.example.localdatebase.database.PoiData
import com.example.localdatebase.database.PoiDataStore
import com.roammate.logic.interfaces.IPoiRepository
import com.roammate.logic.models.BudgetLevel
import com.roammate.logic.models.Coordinates
import com.roammate.logic.models.Environment
import com.roammate.logic.models.OperatingHours
import com.roammate.logic.models.POI
import com.roammate.logic.models.POICategory
import com.roammate.logic.utils.GeoUtils

/** Connects the RoamMate planning engine to the app's persistent SQLite POI store. */
class LocalPoiRepository(private val store: PoiDataStore) : IPoiRepository {
    constructor(context: Context) : this(LocalDatabaseProvider.pois(context.applicationContext))

    override fun getPOIsByCity(city: String): List<POI> =
        query(city = city).filterNot { it.filler }.map { it.toPlannerPoi() }

    override fun getPOIsByCategory(city: String, category: POICategory): List<POI> =
        query(category = category.name, city = city)
            .filterNot { it.filler }
            .map { it.toPlannerPoi() }

    override fun getFillerPOIs(city: String): List<POI> =
        query(city = city).filter { it.filler }.map { it.toPlannerPoi() }

    override fun getPOIsWithinRadius(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        city: String
    ): List<POI> {
        require(radiusMeters >= 0) { "radiusMeters 不能小于 0" }
        return query(city = city).filter {
            GeoUtils.calculateDistance(latitude, longitude, it.latitude, it.longitude) <= radiusMeters
        }.map { it.toPlannerPoi() }
    }

    override fun getPOIById(id: String): POI? = store.getPoiById(id)?.toPlannerPoi()

    private fun query(category: String = "", city: String = "") =
        store.getPois(category, city, "", 10_000)

    private fun PoiData.toPlannerPoi() = POI(
        id = id,
        name = name,
        baseScore = baseScore,
        category = enumOrDefault(category, POICategory.LANDMARK),
        budgetLevel = BudgetLevel.MEDIUM,
        environment = enumOrDefault(environment, Environment.MIXED),
        coordinates = Coordinates(latitude, longitude),
        recommendedVisitDuration = recommendedVisitDuration,
        operatingHours = OperatingHours(
            normalizePlannerTime(openTime, "00:00"),
            normalizePlannerTime(closeTime, "23:59")
        ),
        isFiller = filler,
        city = city,
        description = description.ifBlank { null }
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: fallback

    /**
     * The source data uses values such as 26:00 to mean 02:00 on the next day.
     * The planner only schedules within one calendar day, so 24:00 and later are
     * represented as the end of that planning day.
     */
    private fun normalizePlannerTime(value: String?, fallback: String): String {
        val parts = value?.trim()?.split(':') ?: return fallback
        if (parts.size != 2) return fallback
        val hour = parts[0].toIntOrNull() ?: return fallback
        val minute = parts[1].toIntOrNull() ?: return fallback
        if (hour < 0 || minute !in 0..59) return fallback
        if (hour >= 24) return "23:59"
        return "%02d:%02d".format(java.util.Locale.US, hour, minute)
    }
}

