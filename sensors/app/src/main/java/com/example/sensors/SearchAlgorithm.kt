package com.example.sensors

data class Place(
    val name: String,
    val category: String,
    val suburb: String
)

data class SearchResult(
    val place: Place,
    val score: Double
)

fun searchPlaces(
    query: String,
    places: List<Place>,
    maxResults: Int = 10
): List<SearchResult> {

    // 1. Text Normalization
    val normalizedQuery = normalizeText(query)

    if (normalizedQuery.isEmpty()) {
        return emptyList()
    }

    // 2. Tokenization
    val queryTokens = tokenize(normalizedQuery)

    // 3–6. Matching + Scoring
    val results = places.mapNotNull { place ->

        val name = normalizeText(place.name)
        val category = normalizeText(place.category)
        val suburb = normalizeText(place.suburb)

        val nameTokens = tokenize(name)
        val categoryTokens = tokenize(category)
        val suburbTokens = tokenize(suburb)

        var score = 0.0

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

                    // Partial name token match
                    nameToken.contains(queryToken) -> {
                        70.0
                    }

                    // Fuzzy name match
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

                    // Partial category token match
                    categoryToken.contains(queryToken) -> {
                        40.0
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
            // Suburb Matching
            // ------------------------------------------

            var bestSuburbScore = 0.0

            for (suburbToken in suburbTokens) {

                val currentScore = when {

                    // Exact suburb token match
                    suburbToken == queryToken -> {
                        50.0
                    }

                    // Partial suburb token match
                    suburbToken.contains(queryToken) -> {
                        30.0
                    }

                    else -> {
                        0.0
                    }
                }

                bestSuburbScore = maxOf(
                    bestSuburbScore,
                    currentScore
                )
            }

            score += bestSuburbScore
        }

        if (score > 0) {
            SearchResult(place, score)
        } else {
            null
        }
    }

    // 7. Ranking
    return results
        .sortedByDescending { it.score }
        .take(maxResults)
}


// --------------------------------------------------
// Text Normalization
// --------------------------------------------------

fun normalizeText(text: String): String {

    return text
        .lowercase()
        .trim()
        .replace(Regex("[^a-z0-9\\s]"), "")
        .replace(Regex("\\s+"), " ")
}


// --------------------------------------------------
// Tokenization
// --------------------------------------------------

fun tokenize(text: String): List<String> {

    return text
        .split(" ")
        .filter { it.isNotBlank() }
}


// --------------------------------------------------
// Fuzzy Matching
// --------------------------------------------------

fun fuzzyThreshold(word: String): Int {

    return when {
        word.length <= 4 -> 1
        word.length <= 7 -> 2
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


// --------------------------------------------------
// Levenshtein Distance
// --------------------------------------------------

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
