package com.roammate.logic.interfaces

import com.roammate.logic.models.POI
import com.roammate.logic.models.POICategory

/**
 * Interface for POI data access
 */
interface IPoiRepository {
    /**
     * Get all POIs for a specific city
     */
    fun getPOIsByCity(city: String): List<POI>

    /**
     * Get POIs by category
     */
    fun getPOIsByCategory(city: String, category: POICategory): List<POI>

    /**
     * Get all filler POIs (Hidden Gems) for a city
     */
    fun getFillerPOIs(city: String): List<POI>

    /**
     * Get POIs within a radius (in meters) from a coordinate
     */
    fun getPOIsWithinRadius(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        city: String
    ): List<POI>

    /**
     * Get a POI by ID
     */
    fun getPOIById(id: String): POI?
}
