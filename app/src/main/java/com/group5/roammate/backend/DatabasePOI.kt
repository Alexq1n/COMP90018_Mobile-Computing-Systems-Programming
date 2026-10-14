package com.group5.roammate.backend

import android.content.Context
import com.example.localdatebase.poi.PoiRepository

class DatabasePOI(
    context: Context
) : PlaceRepository {

    private val repository =
        PoiRepository(context.applicationContext)

    override fun getPlaces(): List<Place> {
        ensureBundledPoisAreCurrent()

        return repository.getAllPois().map { poi ->

            Place(
                id = poi.id,
                name = poi.name,
                photoUrl = poi.photoUrl.takeIf(String::isNotBlank),
                website = poi.website.takeIf(String::isNotBlank),
                phone = poi.phone.takeIf(String::isNotBlank),

                baseScore = poi.baseScore,

                category = POICategory.valueOf(
                    poi.category.uppercase()
                ),

                environment = Environment.valueOf(
                    poi.environment.uppercase()
                ),

                coordinates = Coordinates(
                    latitude = poi.latitude,
                    longitude = poi.longitude
                ),

                recommendedVisitDuration = 60,

                operatingHours = OperatingHours(
                    openTime = poi.openTime,
                    closeTime = poi.closeTime
                ),
                isFiller = poi.filler,

                city = "Melbourne",
                address = poi.address,
                description = poi.description
            )
        }
    }

    private fun ensureBundledPoisAreCurrent() {
        if (bundledPoisSynced) return
        synchronized(importLock) {
            if (!bundledPoisSynced) {
                // Upsert once per app process so asset updates and schema migrations
                // populate every field without duplicating records.
                repository.importBundledPois()
                bundledPoisSynced = true
            }
        }
    }

    private companion object {
        private val importLock = Any()

        @Volatile
        private var bundledPoisSynced = false
    }
}
