package com.group5.roammate.poi.data.geoapify

import android.content.Context
import android.util.Log
import com.example.localdatebase.database.LocalDatabaseProvider
import com.example.localdatebase.database.PoiData
import com.group5.roammate.poi.model.POI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PoiDatabaseStore(
    context: Context
) {

    private val database =
        LocalDatabaseProvider.pois(context.applicationContext)

    /**
     * Save a list of POIs into the local SQLite database.
     *
     * Existing POIs with the same id/category will be updated
     * rather than duplicated.
     */
    suspend fun save(pois: List<POI>) = withContext(Dispatchers.IO) {

        if (pois.isEmpty()) {
            return@withContext
        }

        val poiDataList = pois.map { poi ->

            PoiData(
                poi.id,
                poi.name,
                poi.baseScore ?: 0.0,
                poi.category.name,
                poi.environment.name,
                poi.coordinates.latitude,
                poi.coordinates.longitude,
                poi.recommendedVisitDuration,
                poi.operatingHours.openTime,
                poi.operatingHours.closeTime,
                poi.isFiller,
                poi.city,
                poi.address ?: "",
                poi.description ?: "",
                poi.photoUrl ?: "",
                poi.website ?: "",
                poi.phone ?: ""
            )
        }

        database.upsertPois(poiDataList)
        val count = database.countPois()

        Log.d(
            "POI_DATABASE",
            "Saved ${poiDataList.size} POIs. Database now contains $count POIs."
        )
    }
}