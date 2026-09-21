package com.group5.roammate.pet

object PetStateEngine {
    fun buildUiState(
        profile: PetProfile,
        weather: PetWeatherSnapshot = PetWeatherSnapshot.demo(),
    ): PetUiState {
        val resolvedOutfit = resolveOutfit(weather, profile.wardrobeChoice)
        return PetUiState(
            companionStyle = CompanionStyle.Koala,
            outfit = resolvedOutfit,
            wardrobeChoice = profile.wardrobeChoice,
            weather = weather,
            // Weather changes the scene and default clothes, not Buddy's durable emotion.
            mood = PetMood.Ready,
            statusLine = statusFor(profile.wardrobeChoice, resolvedOutfit),
        )
    }

    fun resolveOutfit(
        weather: PetWeatherSnapshot,
        wardrobeChoice: PetWardrobeChoice,
    ): PetOutfit {
        wardrobeChoice.manualOutfit?.let { return it }

        return when {
            weather.condition == PetWeatherCondition.Snow || weather.temperatureC <= 9.0 ->
                PetOutfit.Winter
            weather.condition == PetWeatherCondition.Rain ||
                weather.condition == PetWeatherCondition.Storm -> PetOutfit.Raincoat
            weather.windSpeedKmh >= 25.0 -> PetOutfit.Windbreaker
            weather.condition == PetWeatherCondition.Clear && weather.temperatureC >= 25.0 ->
                PetOutfit.Sunshine
            else -> PetOutfit.Everyday
        }
    }

    fun wardrobeAfter(
        current: PetWardrobeChoice,
        steps: Int,
    ): PetWardrobeChoice {
        val choices = PetWardrobeChoice.entries
        return choices[Math.floorMod(choices.indexOf(current) + steps, choices.size)]
    }

    private fun statusFor(
        choice: PetWardrobeChoice,
        outfit: PetOutfit,
    ): String = if (choice == PetWardrobeChoice.Weather) {
        "Ready to roam in ${outfit.label.lowercase()}."
    } else {
        "Buddy loves this ${choice.label.lowercase()} look."
    }
}

object PetWeatherAdapter {
    fun fromWmoCode(code: Int): PetWeatherCondition = when (code) {
        0 -> PetWeatherCondition.Clear
        in 1..3 -> PetWeatherCondition.Cloudy
        45, 48 -> PetWeatherCondition.Fog
        in 51..67, in 80..82 -> PetWeatherCondition.Rain
        in 71..77, in 85..86 -> PetWeatherCondition.Snow
        in 95..99 -> PetWeatherCondition.Storm
        else -> PetWeatherCondition.Unknown
    }

    fun label(condition: PetWeatherCondition): String = when (condition) {
        PetWeatherCondition.Clear -> "Clear"
        PetWeatherCondition.Cloudy -> "Cloudy"
        PetWeatherCondition.Fog -> "Foggy"
        PetWeatherCondition.Rain -> "Rain"
        PetWeatherCondition.Storm -> "Thunderstorm"
        PetWeatherCondition.Snow -> "Snow"
        PetWeatherCondition.Unknown -> "Weather update"
    }
}
