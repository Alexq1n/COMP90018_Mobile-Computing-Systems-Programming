package com.group5.roammate.pet

/**
 * The one active RoamMate companion can use several travel looks. Keeping the selected look
 * separate from the weather outfit lets the same Buddy wear rain, sun or cold-weather gear.
 */
enum class CompanionStyle(
    val displayName: String,
    val unlockHint: String,
) {
    Corgi("Corgi", "Starter companion"),
    Koala("Koala", "Check in around Melbourne"),
    Penguin("Penguin", "Check in at St Kilda"),
    Kangaroo("Kangaroo", "Visit the Great Ocean Road"),
}

enum class PetWeatherCondition {
    Clear,
    Cloudy,
    Fog,
    Rain,
    Storm,
    Snow,
    Unknown,
}

enum class PetOutfit(
    val label: String,
) {
    Everyday("Everyday"),
    Sunshine("Sunshine"),
    Raincoat("Raincoat"),
    Windbreaker("Windbreaker"),
    Winter("Winter"),
}

enum class PetOutfitMode(
    val label: String,
) {
    Auto("Auto"),
    Everyday("Everyday"),
    Sunshine("Sun"),
    Raincoat("Rain"),
    Windbreaker("Wind"),
    Winter("Cold"),
}

enum class PetMood(
    val label: String,
) {
    Ready("Ready"),
    Excited("Excited"),
    Cozy("Cozy"),
    Tired("Tired"),
}

data class PetEnvironmentSnapshot(
    val condition: PetWeatherCondition,
    val conditionLabel: String,
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val activeMinutes: Int,
    val stepsSinceBreak: Int,
    val locationLabel: String,
    val source: String,
) {
    companion object {
        fun demo(): PetEnvironmentSnapshot = PetEnvironmentSnapshot(
            condition = PetWeatherCondition.Rain,
            conditionLabel = "Rain",
            temperatureC = 14.0,
            windSpeedKmh = 18.0,
            activeMinutes = 46,
            stepsSinceBreak = 4_300,
            locationLabel = "Melbourne Museum",
            source = "Demo data",
        )
    }
}

data class PetUiState(
    val companionStyle: CompanionStyle,
    val outfit: PetOutfit,
    val mood: PetMood,
    val statusLine: String,
    val travelTip: String,
    val environment: PetEnvironmentSnapshot,
)
data class PetProfile(
    val selectedStyle: CompanionStyle = CompanionStyle.Corgi,
    val outfitMode: PetOutfitMode = PetOutfitMode.Auto,
    val checkedInPlaces: Set<String> = setOf(
        "Melbourne",
        "St Kilda",
        "Great Ocean Road",
    ),
)

data class PetWeatherResult(
    val condition: PetWeatherCondition,
    val conditionLabel: String,
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val source: String,
)
