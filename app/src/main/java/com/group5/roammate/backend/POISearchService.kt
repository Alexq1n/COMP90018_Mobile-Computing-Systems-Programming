package com.group5.roammate.backend

import com.group5.roammate.ui.screens.ExplorePlaceCategory

class POISearchService(
    private val repository: PlaceRepository
) {

    fun search(
        query: String,
        userLatitude: Double?,
        userLongitude: Double?,
        category: ExplorePlaceCategory? = null,
        limit: Int,
    ): List<SearchResult> {

        val places = repository.getPlaces()

        return searchPlaces(
            query = query,
            places = places,
            maxResults = limit,
            userLatitude = userLatitude,
            userLongitude = userLongitude,
            category = category
        )
    }
}