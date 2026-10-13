package com.group5.roammate.backend_alex

import com.group5.roammate.ui.screens.ExplorePlaceCategory
import kotlinx.serialization.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


// ==================================================
// Data Models
// ==================================================

@Serializable
data class Place(
    val id: String,
    val name: String,

    // Optional enrichment fields
    val photoUrl: String? = null,
    val website: String? = null,
    val phone: String? = null,

    val baseScore: Double? = null,
    val category: POICategory,
    val environment: Environment,
    val coordinates: Coordinates,
    val recommendedVisitDuration: Int,
    val operatingHours: OperatingHours,
    val isFiller: Boolean,
    val city: String,
    val address: String?,
    val description: String?
)

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class OperatingHours(
    val openTime: String,
    val closeTime: String
)

@Serializable
enum class POICategory {
    MUSEUM,
    PARK,
    RESTAURANT,
    SHOPPING,
    ENTERTAINMENT,
    HISTORICAL,
    NATURE,
    BEACH,
    SPORTS,
    CULTURAL,
    NIGHTLIFE,
    CAFE,
    LANDMARK,
    SCENIC_SPOT
}

@Serializable
enum class Environment {
    INDOOR,
    OUTDOOR,
    MIXED
}

data class SearchResult(
    val place: Place,
    val score: Double,
    val distanceMeters: Double? = null,
    val tagText: String = ""
)



// ==================================================
// Main Search Function
// ==================================================

fun searchPlaces(
    query: String,
    places: List<Place>,
    maxResults: Int = 10,

    // Optional user GPS
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    category: ExplorePlaceCategory? = null
): List<SearchResult> {

    // ------------------------------------------
    // 1. Text Normalization
    // ------------------------------------------
    val normalizedQuery = normalizeText(query)
    val isDistancePriority = normalizedQuery.isEmpty()

    // ------------------------------------------
    // 2. Tokenization
    // ------------------------------------------
    val queryTokens = tokenize(normalizedQuery)

    // ------------------------------------------
    // 3. Validate User GPS
    // ------------------------------------------
    val hasValidLocation =
        userLatitude != null &&
                userLongitude != null &&
                userLatitude.isFinite() &&
                userLongitude.isFinite() &&
                userLatitude in -90.0..90.0 &&
                userLongitude in -180.0..180.0

    // ------------------------------------------
    // 4. Matching + Scoring
    // ------------------------------------------
    val results = places.map { place ->

        val name = normalizeText(place.name)
        val categoryName = normalizeText(place.category.name)
        val city = normalizeText(place.city)
        val environment = normalizeText(place.environment.name)

        val nameTokens = tokenize(name)
        val categoryTokens = tokenize(categoryName)
        val cityTokens = tokenize(city)
        val environmentTokens = tokenize(environment)

        var score = 0.0
        var distanceMeters: Double? = null
        var tagText = ""

        for (queryToken in queryTokens) {

            // ------------------------------------------
            // Name Matching
            // ------------------------------------------
            var bestNameScore = 0.0

            for (nameToken in nameTokens) {

                val currentScore = when {

                    // Exact name token match
                    nameToken == queryToken -> {
                        100.0
                    }

                    // Fuzzy match
                    levenshteinDistance(
                        queryToken,
                        nameToken
                    ) <= fuzzyThreshold(queryToken) -> {
                        fuzzyScore(
                            queryToken,
                            nameToken
                        )
                    }

                    else -> {
                        0.0
                    }
                }

                bestNameScore = maxOf(
                    bestNameScore,
                    currentScore
                )
            }

            score += bestNameScore

            // ------------------------------------------
            // Category Matching
            // ------------------------------------------
            var bestCategoryScore = 0.0

            for (categoryToken in categoryTokens) {

                val currentScore = when {

                    // Exact category token match
                    categoryToken == queryToken -> {
                        60.0
                    }

                    // Fuzzy match
                    levenshteinDistance(
                        queryToken,
                        categoryToken
                    ) <= fuzzyThreshold(queryToken) -> {
                        fuzzyScore(
                            queryToken,
                            categoryToken
                        )
                    }

                    else -> {
                        0.0
                    }
                }

                bestCategoryScore = maxOf(
                    bestCategoryScore,
                    currentScore
                )
            }

            score += bestCategoryScore

            // ------------------------------------------
            // City Matching
            // ------------------------------------------
            var bestCityScore = 0.0

            for (cityToken in cityTokens) {

                val currentScore = when {

                    // Exact city token match
                    cityToken == queryToken -> {
                        50.0
                    }

                    // Fuzzy match
                    levenshteinDistance(
                        queryToken,
                        cityToken
                    ) <= fuzzyThreshold(queryToken) -> {
                        fuzzyScore(
                            queryToken,
                            cityToken
                        )
                    }

                    else -> {
                        0.0
                    }
                }

                bestCityScore = maxOf(
                    bestCityScore,
                    currentScore
                )
            }

            score += bestCityScore

            // ------------------------------------------
            // Environment Matching
            // ------------------------------------------
            var bestEnvironmentScore = 0.0

            for (environmentToken in environmentTokens) {

                val currentScore = when {

                    // Exact environment token match
                    environmentToken == queryToken -> {
                        40.0
                    }

                    // Fuzzy match
                    levenshteinDistance(
                        queryToken,
                        environmentToken
                    ) <= fuzzyThreshold(queryToken) -> {
                        fuzzyScore(
                            queryToken,
                            environmentToken
                        )
                    }

                    else -> {
                        0.0
                    }
                }

                bestEnvironmentScore = maxOf(
                    bestEnvironmentScore,
                    currentScore
                )
            }

            score += bestEnvironmentScore
        }

        // ------------------------------------------
        // 5. Distance Calculation
        // ------------------------------------------
        if (hasValidLocation) {

            val placeLatitude = place.coordinates.latitude
            val placeLongitude = place.coordinates.longitude

            val validPlaceCoordinates =
                placeLatitude.isFinite() &&
                        placeLongitude.isFinite() &&
                        placeLatitude in -90.0..90.0 &&
                        placeLongitude in -180.0..180.0

            if (validPlaceCoordinates) {

                distanceMeters = calculateDistanceMeters(
                    userLatitude!!,
                    userLongitude!!,
                    placeLatitude,
                    placeLongitude
                )

                if (isDistancePriority) {

                    // Distance Priority Mode:
                    // Hidden gems within 1000m receive
                    // priority during final sorting.

                    if (
                        place.isFiller == true
                    ) {
                        tagText = "Hidden gem"
                    }

                } else {

                    // Normal Text Search Mode:
                    // Distance contributes to the score.
                    score += calculateDistanceScore(
                        distanceMeters
                    )
                }
            }
        }

        // ------------------------------------------
        // 6. Build SearchResult
        // ------------------------------------------
        SearchResult(
            place = place,
            score = score,
            distanceMeters = distanceMeters,
            tagText = tagText
        )
    }

    // ------------------------------------------
    // 7. Ranking
    // ------------------------------------------
    return if (isDistancePriority) {

        // Distance Priority Mode:
        //
        // Priority 1: Category match
        // Priority 2: Hidden gem
        // Priority 3: Nearest distance

        results
            .sortedWith(
                compareByDescending<SearchResult> { result ->

                    // Priority 1: Category matching
                    when (category) {

                        null -> true

                        ExplorePlaceCategory.Indoor ->
                            result.place.environment ==
                                    Environment.INDOOR

                        ExplorePlaceCategory.Outdoor ->
                            result.place.environment ==
                                    Environment.OUTDOOR

                        ExplorePlaceCategory.Cafes ->
                            result.place.category ==
                                    POICategory.CAFE

                        ExplorePlaceCategory.Food ->
                            result.place.category ==
                                    POICategory.RESTAURANT
                    }

                }.thenByDescending { result ->

                    // Priority 2: Hidden gem
                    result.place.isFiller == true &&
                            (result.place.baseScore ?: 0.0) >= 8.0 &&
                            result.distanceMeters != null &&
                            result.distanceMeters <= 500

                }.thenBy { result ->

                    // Priority 3: Nearest distance
                    // Unknown distances are placed last.
                    result.distanceMeters
                        ?: Double.POSITIVE_INFINITY
                }
            )
            .take(maxResults.coerceAtLeast(0))

    } else {

        // Normal Text Search Mode:
        // Text matching score + distance score

        results
            .sortedByDescending { it.score }
            .take(maxResults.coerceAtLeast(0))
    }
}








// ==================================================
// Text Normalization
// ==================================================

fun normalizeText(text: String): String {

    return text
        .lowercase()
        .trim()
        .replace("_", " ")
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

// ==================================================
// Tokenization
// ==================================================

fun tokenize(text: String): List<String> {

    return text
        .split(" ")
        .filter { it.isNotBlank() }
}

// ==================================================
// Fuzzy Matching
// ==================================================

fun fuzzyThreshold(word: String): Int {

    return when {
        word.length <= 5 -> 1
        word.length <= 8 -> 2
        else -> 3
    }
}

fun fuzzyScore(
    query: String,
    target: String
): Double {

    val distance = levenshteinDistance(
        query,
        target
    )

    if (distance == 0) {
        return 0.0
    }

    val maxLength = maxOf(
        query.length,
        target.length
    )

    return 30.0 * (
            1.0 - distance.toDouble() / maxLength
            )
}

// ==================================================
// Levenshtein Distance
// ==================================================

fun levenshteinDistance(
    a: String,
    b: String
): Int {

    val dp = Array(a.length + 1) {
        IntArray(b.length + 1)
    }

    for (i in 0..a.length) {
        dp[i][0] = i
    }

    for (j in 0..b.length) {
        dp[0][j] = j
    }

    for (i in 1..a.length) {

        for (j in 1..b.length) {

            val cost =
                if (a[i - 1] == b[j - 1]) {
                    0
                } else {
                    1
                }

            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }

    return dp[a.length][b.length]
}

// ==================================================
// Distance Calculation
// Haversine Formula
// Output: Distance in meters
// ==================================================

fun calculateDistanceMeters(
    userLatitude: Double,
    userLongitude: Double,
    placeLatitude: Double,
    placeLongitude: Double
): Double {

    val earthRadius = 6371000.0

    val lat1 = Math.toRadians(userLatitude)
    val lat2 = Math.toRadians(placeLatitude)

    val deltaLat = Math.toRadians(
        placeLatitude - userLatitude
    )

    val deltaLon = Math.toRadians(
        placeLongitude - userLongitude
    )

    val a =
        sin(deltaLat / 2).pow(2.0) +
                cos(lat1) * cos(lat2) *
                sin(deltaLon / 2).pow(2.0)

    val safeA = a.coerceIn(0.0, 1.0)

    val c = 2.0 * atan2(
        sqrt(safeA),
        sqrt(1.0 - safeA)
    )

    return earthRadius * c
}

// ==================================================
// Distance Scoring
// Maximum: 30 points
// Score halves every 2 km
// ==================================================

fun calculateDistanceScore(
    distanceMeters: Double
): Double {

    val maxScore = 30.0
    val halfDistance = 2000.0

    return maxScore * 2.0.pow(
        -distanceMeters.coerceAtLeast(0.0) / halfDistance
    )
}
