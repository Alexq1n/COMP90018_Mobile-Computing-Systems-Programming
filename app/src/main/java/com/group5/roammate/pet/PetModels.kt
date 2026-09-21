package com.group5.roammate.pet

/** RoamMate intentionally exposes one companion so its interaction can stay focused and personal. */
enum class CompanionStyle(
    val displayName: String,
) {
    Koala("Koala"),
}

enum class PetOutfit(
    val label: String,
) {
    Everyday("Everyday"),
    Sunshine("Sunshine"),
    Raincoat("Raincoat"),
    Windbreaker("Windbreaker"),
    Winter("Winter"),
    Explorer("Explorer"),
    Streetwear("Streetwear"),
    Festival("Festival"),
    Pajamas("Star pajamas"),
}

/** The carousel choice is separate from the sprite outfit resolved from live weather. */
enum class PetWardrobeChoice(
    val label: String,
    val manualOutfit: PetOutfit?,
) {
    Weather("Weather outfit", null),
    Explorer("Explorer", PetOutfit.Explorer),
    Streetwear("Streetwear", PetOutfit.Streetwear),
    Festival("Festival", PetOutfit.Festival),
    Pajamas("Star pajamas", PetOutfit.Pajamas),
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

enum class PetMood(
    val label: String,
) {
    Ready("Ready"),
    Excited("Excited"),
    Cozy("Cozy"),
    Tired("Tired"),
}

data class PetUiState(
    val companionStyle: CompanionStyle,
    val outfit: PetOutfit,
    val wardrobeChoice: PetWardrobeChoice,
    val weather: PetWeatherSnapshot,
    val mood: PetMood,
    val statusLine: String,
    val weatherOutfit: PetOutfit? = null,
    val wardrobeNote: String = "",
)

data class PetProfile(
    val wardrobeChoice: PetWardrobeChoice = PetWardrobeChoice.Weather,
)

data class PetWeatherSnapshot(
    val condition: PetWeatherCondition,
    val label: String,
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val locationLabel: String,
    val source: String,
    val observedAtMillis: Long = 0L,
    val todayHighC: Double? = null,
    val todayLowC: Double? = null,
    val todayRainChancePercent: Int? = null,
    val forecastDate: String? = null,
    val isDay: Boolean = true,
) {
    val isCurrent: Boolean get() = isFreshAt(System.currentTimeMillis())

    fun isFreshAt(nowMillis: Long): Boolean =
        source != "Demo fallback" && condition != PetWeatherCondition.Unknown &&
            temperatureC.isFinite() && windSpeedKmh.isFinite() &&
            observedAtMillis > 0L && nowMillis - observedAtMillis in 0L..45 * 60_000L

    companion object {
        /** No fabricated conditions are used while a live request is unavailable. */
        fun demo(): PetWeatherSnapshot = PetWeatherSnapshot(
            condition = PetWeatherCondition.Unknown,
            label = "Weather unavailable",
            temperatureC = 0.0,
            windSpeedKmh = 0.0,
            locationLabel = "Melbourne · selected city",
            source = "Demo fallback",
        )
    }
}

/** Small itinerary seam used by shake advice without coupling the pet to the Trip UI models. */
data class PetTripContext(
    val nextStopName: String? = null,
    val nextStopTime: String? = null,
    val isDemo: Boolean = true,
)
