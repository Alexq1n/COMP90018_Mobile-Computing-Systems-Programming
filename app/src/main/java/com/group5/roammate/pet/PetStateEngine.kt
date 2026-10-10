package com.group5.roammate.pet

/** One resolved appearance drives the stage, Home, camera and spoken wardrobe description. */
object PetStateEngine {
    fun buildUiState(
        profile: PetProfile,
        weather: PetWeatherSnapshot = PetWeatherSnapshot.demo(),
    ): PetUiState {
        val gear = protectiveOutfit(weather)
        val outfit = resolveOutfit(weather, profile.wardrobeChoice)
        val savedFashion = profile.wardrobeChoice.manualOutfit
        val note = when {
            !weather.isCurrent -> "Weather unavailable · personal style"
            gear != null && savedFashion != null -> "${gear.label} on · ${savedFashion.label} saved"
            gear != null -> "${gear.label} · weather matched"
            else -> "Comfortable weather · ${outfit.label}"
        }
        return PetUiState(
            companionStyle = CompanionStyle.Koala,
            outfit = outfit,
            wardrobeChoice = profile.wardrobeChoice,
            weather = weather,
            mood = PetMood.Ready,
            statusLine = when {
                !weather.isCurrent -> "I'm here with you. Let's check the weather when it reconnects."
                gear == PetOutfit.Raincoat -> "Raincoat on! Let's find a sheltered route together."
                gear == PetOutfit.Winter -> "Wrapped up warm. How about a cosy break?"
                gear == PetOutfit.Windbreaker -> "Zipped up for the wind. Stay close, little explorer."
                gear == PetOutfit.Sunshine -> "Sun hat ready! Let's bring water and find some shade."
                else -> "${outfit.label} today. Ready when you are!"
            },
            weatherOutfit = gear,
            wardrobeNote = note,
        )
    }

    /** Weather protection takes priority; the chosen fashion is remembered, never overwritten. */
    fun resolveOutfit(weather: PetWeatherSnapshot, wardrobeChoice: PetWardrobeChoice): PetOutfit =
        protectiveOutfit(weather) ?: wardrobeChoice.manualOutfit ?: PetOutfit.Everyday

    fun protectiveOutfit(weather: PetWeatherSnapshot): PetOutfit? {
        if (!weather.isCurrent) return null
        return when {
            weather.condition == PetWeatherCondition.Storm ||
                weather.condition == PetWeatherCondition.Rain -> PetOutfit.Raincoat
            weather.condition == PetWeatherCondition.Snow || weather.temperatureC <= 9.0 -> PetOutfit.Winter
            weather.windSpeedKmh >= 25.0 -> PetOutfit.Windbreaker
            weather.isDay && weather.condition == PetWeatherCondition.Clear &&
                weather.temperatureC >= 25.0 -> PetOutfit.Sunshine
            else -> null
        }
    }

    fun wardrobeAfter(current: PetWardrobeChoice, steps: Int): PetWardrobeChoice {
        val choices = PetWardrobeChoice.entries
        return choices[Math.floorMod(choices.indexOf(current) + steps, choices.size)]
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
