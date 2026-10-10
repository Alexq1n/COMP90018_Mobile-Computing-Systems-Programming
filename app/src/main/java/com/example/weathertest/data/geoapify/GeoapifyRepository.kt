package com.example.weathertest.data.geoapify

import com.example.weathertest.model.Coordinates
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
     * Convert a destination name/address from the UI
     * into latitude and longitude using Geoapify.
     *
     * Example:
     * "Melbourne" -> Coordinates(-37.81, 144.96)
     */
    suspend fun geocodeDestination(
        destinationName: String
    ): Coordinates? = withContext(Dispatchers.IO) {

        if (destinationName.isBlank()) {
            return@withContext null
        }

        val url =
            "https://api.geoapify.com/v1/geocode/search"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter(
                    "text",
                    destinationName
                )
                .addQueryParameter(
                    "format",
                    "geojson"
                )
                .addQueryParameter(
                    "limit",
                    "1"
                )
                .addQueryParameter(
                    "lang",
                    "en"
                )
                .addQueryParameter(
                    "apiKey",
                    apiKey
                )
                .build()

        val request =
            Request.Builder()
                .url(url)
                .get()
                .build()

        try {

            client.newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {
                        return@withContext null
                    }

                    val body =
                        response.body?.string()
                            ?: return@withContext null

                    val properties =
                        json
                            .decodeFromString<GeoapifyResponse>(
                                body
                            )
                            .features
                            .firstOrNull()
                            ?.properties
                            ?: return@withContext null

                    val latitude =
                        properties.lat
                            ?: return@withContext null

                    val longitude =
                        properties.lon
                            ?: return@withContext null

                    Coordinates(
                        latitude = latitude,
                        longitude = longitude
                    )
                }

        } catch (e: Exception) {

            null
        }
    }
    suspend fun fetchCategoryPois(
        category: POICategory,
        city: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 25_000,
        limit: Int = 10,
        enrichWithDetails: Boolean = false
    ): List<POI> = withContext(Dispatchers.IO) {

        require(limit in 1..20) {
            "For this starter, use limit between 1 and 20."
        }

        val categories =
            POICategoryConfig.geoapifyCategories[category]
                ?: error("No Geoapify mapping configured for $category")

        val rawLimit =
            minOf(
                20,
                maxOf(limit, limit * 2)
            )

        val response =
            fetchPlaces(
                categories = categories,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                limit = rawLimit
            )

        response.features
            .mapNotNull { feature ->

                val details =
                    if (enrichWithDetails) {
                        feature.properties.placeId?.let {
                            fetchPlaceDetails(it)
                        }
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

    suspend fun fetchAllCategories(
        city: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 20_000,
        enrichWithDetails: Boolean = false
    ): List<POI> {

        val result = mutableListOf<POI>()

        val categoryLimits =
            mapOf(
                POICategory.MUSEUM to 15,
                POICategory.PARK to 15,
                POICategory.RESTAURANT to 20,
                POICategory.SHOPPING to 10,
                POICategory.ENTERTAINMENT to 15,
                POICategory.HISTORICAL to 15,
                POICategory.NATURE to 15,
                POICategory.BEACH to 10,
                POICategory.SPORTS to 10,
                POICategory.CULTURAL to 15,
                POICategory.NIGHTLIFE to 10,
                POICategory.CAFE to 20,
                POICategory.LANDMARK to 20,
                POICategory.SCENIC_SPOT to 15
            )

        for (category in POICategory.entries) {

            val limit = categoryLimits.getValue(category)

            result +=
                fetchCategoryPois(
                    category = category,
                    city = city,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                    limit = limit,
                    enrichWithDetails = enrichWithDetails
                )
        }

        return removeDuplicatePois(result)
    }

    fun toJson(
        pois: List<POI>
    ): String {

        return json.encodeToString(pois)
    }


    private fun removeDuplicatePois(
        pois: List<POI>
    ): List<POI> {

        val result = mutableListOf<POI>()

        for (poi in pois) {

            val duplicate = result.any { existing ->

                if (existing.id == poi.id) {
                    true
                } else {
                    existing.name.equals(
                        poi.name,
                        ignoreCase = true
                    ) &&
                            distanceMeters(
                                existing.coordinates.latitude,
                                existing.coordinates.longitude,
                                poi.coordinates.latitude,
                                poi.coordinates.longitude
                            ) <= 50
                }
            }

            if (!duplicate) {
                result.add(poi)
            }
        }

        return result
    }

    private fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {

        val earthRadius = 6371000.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            kotlin.math.sin(dLat / 2) *
                    kotlin.math.sin(dLat / 2) +
                    kotlin.math.cos(Math.toRadians(lat1)) *
                    kotlin.math.cos(Math.toRadians(lat2)) *
                    kotlin.math.sin(dLon / 2) *
                    kotlin.math.sin(dLon / 2)

        val c =
            2 * kotlin.math.atan2(
                kotlin.math.sqrt(a),
                kotlin.math.sqrt(1 - a)
            )

        return earthRadius * c
    }

    private fun fetchPlaces(
        categories: List<String>,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        limit: Int
    ): GeoapifyResponse {

        val url =
            "https://api.geoapify.com/v2/places"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter(
                    "categories",
                    categories.joinToString(",")
                )
                .addQueryParameter(
                    "filter",
                    "circle:$longitude,$latitude,$radiusMeters"
                )
                .addQueryParameter(
                    "bias",
                    "proximity:$longitude,$latitude"
                )
                .addQueryParameter(
                    "limit",
                    limit.toString()
                )
                .addQueryParameter(
                    "lang",
                    "en"
                )
                .addQueryParameter(
                    "apiKey",
                    apiKey
                )
                .build()

        val request =
            Request.Builder()
                .url(url)
                .get()
                .build()

        client.newCall(request)
            .execute()
            .use { response ->

                if (!response.isSuccessful) {
                    error(
                        "Geoapify Places API failed: HTTP ${response.code}"
                    )
                }

                val body =
                    response.body?.string()
                        ?: error(
                            "Geoapify returned an empty response"
                        )

                return json.decodeFromString<GeoapifyResponse>(
                    body
                )
            }
    }

    private fun fetchPlaceDetails(
        placeId: String
    ): GeoapifyProperties? {

        val url =
            "https://api.geoapify.com/v2/place-details"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter(
                    "id",
                    placeId
                )
                .addQueryParameter(
                    "features",
                    "details"
                )
                .addQueryParameter(
                    "lang",
                    "en"
                )
                .addQueryParameter(
                    "apiKey",
                    apiKey
                )
                .build()

        val request =
            Request.Builder()
                .url(url)
                .get()
                .build()

        client.newCall(request)
            .execute()
            .use { response ->

                if (!response.isSuccessful) {
                    return null
                }

                val body =
                    response.body?.string()
                        ?: return null

                return json
                    .decodeFromString<GeoapifyResponse>(
                        body
                    )
                    .features
                    .firstOrNull()
                    ?.properties
            }
    }
}