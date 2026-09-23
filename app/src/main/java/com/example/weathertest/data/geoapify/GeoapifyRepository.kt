package com.example.weathertest.data.geoapify

import com.example.weathertest.model.POI
import com.example.weathertest.model.POICategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class GeoapifyRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val apiKey = "d314c90db3df40408b80296b56a0a5ae"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Fetch one app category and return clean POI objects.
     * We request up to 20 raw records so that after dropping nameless/invalid records
     * we can usually keep the requested 10.
     */
    suspend fun fetchCategoryPois(
        category: POICategory,
        city: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 25_000,
        limit: Int = 10,
        enrichWithDetails: Boolean = false
    ): List<POI> = withContext(Dispatchers.IO) {
        require(limit in 1..20) { "For this starter, use limit between 1 and 20." }

        val categories = POICategoryConfig.geoapifyCategories[category]
            ?: error("No Geoapify mapping configured for $category")

        val rawLimit = minOf(20, maxOf(limit, limit * 2))
        val response = fetchPlaces(
            categories = categories,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            limit = rawLimit
        )

        response.features
            .mapNotNull { feature ->
                val details = if (enrichWithDetails) {
                    feature.properties.placeId?.let { fetchPlaceDetails(it) }
                } else {
                    null
                }

                GeoapifyPoiMapper.toPoi(
                    feature = feature,
                    targetCategory = category,
                    requestedCity = city,
                    details = details
                )
            }
            .distinctBy { it.id }
            .take(limit)
    }

    /** Fetch approximately `perCategory` POIs for every RoamMate category. */
    suspend fun fetchAllCategories(
        city: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 20_000,
        perCategory: Int = 10,
        enrichWithDetails: Boolean = false
    ): List<POI> {
        val result = mutableListOf<POI>()

        // Sequential calls are intentional: simple, predictable, and gentle on API rate limits.
        for (category in POICategory.entries) {
            result += fetchCategoryPois(
                category = category,
                city = city,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                limit = perCategory,
                enrichWithDetails = enrichWithDetails
            )
        }

        return result
    }

    /** Convert the final List<POI> into a local JSON string for caching/assets/debugging. */
    fun toJson(pois: List<POI>): String = json.encodeToString(pois)

    private fun fetchPlaces(
        categories: List<String>,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        limit: Int
    ): GeoapifyResponse {
        val url = "https://api.geoapify.com/v2/places".toHttpUrl().newBuilder()
            .addQueryParameter("categories", categories.joinToString(","))
            .addQueryParameter("filter", "circle:$longitude,$latitude,$radiusMeters")
            .addQueryParameter("bias", "proximity:$longitude,$latitude")
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("lang", "en")
            .addQueryParameter("apiKey", apiKey)
            .build()

        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Geoapify Places API failed: HTTP ${response.code}")
            }
            val body = response.body?.string() ?: error("Geoapify returned an empty response")
            return json.decodeFromString<GeoapifyResponse>(body)
        }
    }

    /**
     * Optional enrichment for description + opening_hours.
     * Do NOT call this for every POI on every app launch; cache the result.
     */
    private fun fetchPlaceDetails(placeId: String): GeoapifyProperties? {
        val url = "https://api.geoapify.com/v2/place-details".toHttpUrl().newBuilder()
            .addQueryParameter("id", placeId)
            .addQueryParameter("features", "details")
            .addQueryParameter("lang", "en")
            .addQueryParameter("apiKey", apiKey)
            .build()

        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            return json.decodeFromString<GeoapifyResponse>(body)
                .features
                .firstOrNull()
                ?.properties
        }
    }
}