package com.example.weathertest.data.geoapify

import com.example.weathertest.model.Coordinates
import com.example.weathertest.model.Environment
import com.example.weathertest.model.OperatingHours
import com.example.weathertest.model.POI
import com.example.weathertest.model.POICategory

object GeoapifyPoiMapper {

    fun toPoi(
        feature: GeoapifyFeature,
        targetCategory: POICategory,
        requestedCity: String,
        details: GeoapifyProperties? = null
    ): POI? {
        val p = feature.properties
        val name = p.name?.trim().orEmpty()
        val lat = p.lat
        val lon = p.lon

        // Skip unusable API records.
        if (name.isBlank() || lat == null || lon == null) return null

        val merged = mergeProperties(p, details)

        return POI(
            // Geoapify place_id is already a unique place identifier.
            id = merged.placeId ?: fallbackId(name, lat, lon),
            name = name,

            // IMPORTANT: Geoapify does not provide a user rating / quality score.
            // Keep this as a neutral prototype value until your team defines scoring.
            baseScore = 7.0,

            category = targetCategory,
            environment = environmentFor(targetCategory),
            coordinates = Coordinates(latitude = lat, longitude = lon),
            recommendedVisitDuration = visitDurationFor(targetCategory),
            operatingHours = parseOpeningHours(merged.openingHours)
                ?: fallbackHoursForPrototype(targetCategory),

            // Geoapify cannot tell whether a place is a "Hidden Gem" in your app's sense.
            // Keep false initially; calculate this later from your own ranking/popularity logic.
            isFiller = isFillerCategory(targetCategory),
            city = merged.city ?: requestedCity,
            address = buildAddress(merged),
            description = merged.description
        )
    }

    private fun mergeProperties(
        place: GeoapifyProperties,
        details: GeoapifyProperties?
    ): GeoapifyProperties {
        if (details == null) return place
        return place.copy(
            name = details.name ?: place.name,
            city = details.city ?: place.city,
            lat = details.lat ?: place.lat,
            lon = details.lon ?: place.lon,
            categories = if (details.categories.isNotEmpty()) details.categories else place.categories,
            placeId = details.placeId ?: place.placeId,
            formatted = details.formatted ?: place.formatted,
            openingHours = details.openingHours ?: place.openingHours,
            description = details.description ?: place.description,
            website = details.website ?: place.website,
            phone = details.phone ?: place.phone
        )
    }

    /**
     * OSM opening_hours can be complex, e.g. "Mo-Fr 09:00-17:00; Sa 10:00-16:00".
     * For the current POI model (one open/close pair only), use the first time range.
     * This is deliberately lossy. A weekday-aware model is better long-term.
     */
    fun parseOpeningHours(raw: String?): OperatingHours? {
        if (raw.isNullOrBlank()) return null
        val match = Regex("(\\d{2}:\\d{2})-(\\d{2}:\\d{2})").find(raw) ?: return null
        return OperatingHours(
            openTime = match.groupValues[1],
            closeTime = match.groupValues[2]
        )
    }

    private fun environmentFor(category: POICategory): Environment = when (category) {
        POICategory.MUSEUM,
        POICategory.RESTAURANT,
        POICategory.SHOPPING,
        POICategory.CAFE -> Environment.INDOOR

        POICategory.PARK,
        POICategory.NATURE,
        POICategory.BEACH,
        POICategory.SCENIC_SPOT -> Environment.OUTDOOR

        POICategory.ENTERTAINMENT,
        POICategory.HISTORICAL,
        POICategory.SPORTS,
        POICategory.CULTURAL,
        POICategory.NIGHTLIFE,
        POICategory.LANDMARK -> Environment.MIXED
    }

    private fun visitDurationFor(category: POICategory): Int = when (category) {
        POICategory.MUSEUM -> 120
        POICategory.PARK -> 90
        POICategory.RESTAURANT -> 90
        POICategory.SHOPPING -> 120
        POICategory.ENTERTAINMENT -> 120
        POICategory.HISTORICAL -> 60
        POICategory.NATURE -> 120
        POICategory.BEACH -> 120
        POICategory.SPORTS -> 90
        POICategory.CULTURAL -> 90
        POICategory.NIGHTLIFE -> 150
        POICategory.CAFE -> 60
        POICategory.LANDMARK -> 45
        POICategory.SCENIC_SPOT -> 45
    }

    /**
     * Prototype fallback only. These are NOT guaranteed real opening hours.
     * If your scheduling logic depends on hours, enable Place Details enrichment
     * or make POI.operatingHours nullable.
     */
    private fun fallbackHoursForPrototype(category: POICategory): OperatingHours = when (category) {
        POICategory.PARK,
        POICategory.NATURE,
        POICategory.BEACH,
        POICategory.LANDMARK,
        POICategory.SCENIC_SPOT -> OperatingHours("06:00", "22:00")

        POICategory.RESTAURANT -> OperatingHours("11:00", "22:00")
        POICategory.CAFE -> OperatingHours("07:00", "17:00")
        POICategory.NIGHTLIFE -> OperatingHours("18:00", "02:00")
        POICategory.SHOPPING -> OperatingHours("09:00", "18:00")
        else -> OperatingHours("09:00", "18:00")
    }

    private fun fallbackId(name: String, lat: Double, lon: Double): String {
        val raw = "$name|$lat|$lon"
        return "poi_${raw.hashCode().toUInt().toString(16)}"
    }

    private fun buildAddress(properties: GeoapifyProperties): String? {

        val streetAddress = listOfNotNull(
            properties.housenumber,
            properties.street
        )
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val cityStatePostcode = listOfNotNull(
            properties.city,
            properties.state,
            properties.postcode
        )
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val address = listOf(
            streetAddress,
            cityStatePostcode,
            properties.country
        )
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(", ")

        return address.ifBlank { null }
    }

    private fun isFillerCategory(category: POICategory): Boolean {
        return when (category) {
            POICategory.CAFE,
            POICategory.PARK,
            POICategory.SHOPPING,
            POICategory.LANDMARK,
            POICategory.SCENIC_SPOT -> true

            else -> false
        }
    }
}