package com.example.weathertest.data.geoapify

import com.example.weathertest.model.POICategory

/**
 * Mapping from RoamMate categories to Geoapify Places API categories.
 * Multiple Geoapify categories are OR-ed in the request.
 */
object POICategoryConfig {
    val geoapifyCategories: Map<POICategory, List<String>> = mapOf(
        POICategory.MUSEUM to listOf(
            "entertainment.museum"
        ),
        POICategory.PARK to listOf(
            "leisure.park"
        ),
        POICategory.RESTAURANT to listOf(
            "catering.restaurant"
        ),
        POICategory.SHOPPING to listOf(
            "commercial.shopping_mall",
            "commercial.department_store"
        ),
        POICategory.ENTERTAINMENT to listOf(
            "entertainment.cinema",
            "entertainment.bowling_alley",
            "entertainment.amusement_arcade",
            "entertainment.theme_park",
            "entertainment.aquarium",
            "entertainment.zoo"
        ),
        POICategory.HISTORICAL to listOf(
            "tourism.sights.archaeological_site",
            "tourism.sights.castle",
            "tourism.sights.fort",
            "tourism.sights.memorial",
            "tourism.sights.ruines",
            "building.historic"
        ),
        POICategory.NATURE to listOf(
            "natural",
            "national_park",
            "leisure.park.nature_reserve"
        ),
        POICategory.BEACH to listOf(
            "beach"
        ),
        POICategory.SPORTS to listOf(
            "sport.stadium",
            "sport.sports_centre",
            "sport.sports_hall",
            "sport.golf_course",
            "activity.sport_club"
        ),
        POICategory.CULTURAL to listOf(
            "entertainment.culture"
        ),
        POICategory.NIGHTLIFE to listOf(
            "catering.bar",
            "catering.pub",
            "adult.nightclub"
        ),
        POICategory.CAFE to listOf(
            "catering.cafe"
        ),
        POICategory.LANDMARK to listOf(
            "tourism.sights.building",
            "tourism.sights.bridge",
            "tourism.sights.city_hall",
            "tourism.sights.lighthouse",
            "tourism.sights.tower"
        ),
        POICategory.SCENIC_SPOT to listOf(
            "tourism.attraction.viewpoint",
            "natural.mountain.peak",
            "natural.coastal"
        )
    )
}