package com.group5.roammate.backend

import android.content.Context
import com.example.localdatebase.poi.PoiRepository

class DatabasePOI(
    context: Context
) : PlaceRepository {

    private val repository =
        PoiRepository(context.applicationContext)

    override fun getPlaces(): List<Place> {

        // Only import bundled POIs if SQLite is empty
        if (repository.count() == 0) {
            repository.importBundledPois()
        }


        return repository.getAllPois().map { poi ->

            Place(
                id = poi.id,
                name = poi.name,

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
}