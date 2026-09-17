package com.group5.roammate.pet

object PetStateEngine {
    private const val TIRED_ACTIVE_MINUTES = 40
    private const val TIRED_STEP_COUNT = 4_000
    private const val WINDY_KMH = 25.0
    private const val COLD_CELSIUS = 9.0
    private const val HOT_CELSIUS = 25.0

    fun buildUiState(
        environment: PetEnvironmentSnapshot,
        profile: PetProfile,
    ): PetUiState {
        val outfit = resolveOutfit(environment, profile.outfitMode)
        val mood = resolveMood(environment)

        return PetUiState(
            companionStyle = profile.selectedStyle,
            outfit = outfit,
            mood = mood,
            statusLine = buildStatusLine(environment, mood, outfit),
            travelTip = buildTravelTip(environment, mood, outfit),
            environment = environment,
        )
    }

    fun resolveOutfit(
        environment: PetEnvironmentSnapshot,
        mode: PetOutfitMode,
    ): PetOutfit {
        if (mode != PetOutfitMode.Auto) {
            return when (mode) {
                PetOutfitMode.Auto -> error("Auto is handled before this branch")
                PetOutfitMode.Everyday -> PetOutfit.Everyday
                PetOutfitMode.Sunshine -> PetOutfit.Sunshine
                PetOutfitMode.Raincoat -> PetOutfit.Raincoat
                PetOutfitMode.Windbreaker -> PetOutfit.Windbreaker
                PetOutfitMode.Winter -> PetOutfit.Winter
            }
        }

        return when {
            environment.condition == PetWeatherCondition.Snow ||
                environment.temperatureC <= COLD_CELSIUS -> PetOutfit.Winter

            environment.condition == PetWeatherCondition.Rain ||
                environment.condition == PetWeatherCondition.Storm -> PetOutfit.Raincoat

            environment.windSpeedKmh >= WINDY_KMH -> PetOutfit.Windbreaker

            environment.condition == PetWeatherCondition.Clear &&
                environment.temperatureC >= HOT_CELSIUS -> PetOutfit.Sunshine

            else -> PetOutfit.Everyday
        }
    }

    fun resolveMood(environment: PetEnvironmentSnapshot): PetMood {
        return when {
            environment.activeMinutes >= TIRED_ACTIVE_MINUTES ||
                environment.stepsSinceBreak >= TIRED_STEP_COUNT -> PetMood.Tired

            environment.condition == PetWeatherCondition.Storm ||
                environment.condition == PetWeatherCondition.Snow -> PetMood.Cozy

            environment.condition == PetWeatherCondition.Clear -> PetMood.Excited
            else -> PetMood.Ready
        }
    }

    fun unlockedStyles(checkedInPlaces: Set<String>): Set<CompanionStyle> {
        val normalized = checkedInPlaces.map { it.lowercase() }
        return buildSet {
            add(CompanionStyle.Corgi)
            if (normalized.any { it.contains("melbourne") }) add(CompanionStyle.Koala)
            if (normalized.any { it.contains("st kilda") }) add(CompanionStyle.Penguin)
            if (normalized.any { it.contains("great ocean road") }) add(CompanionStyle.Kangaroo)
        }
    }

    private fun buildStatusLine(
        environment: PetEnvironmentSnapshot,
        mood: PetMood,
        outfit: PetOutfit,
    ): String {
        val weather = "${environment.conditionLabel} · ${environment.temperatureC.toInt()}°C"
        return when (mood) {
            PetMood.Tired -> "$weather · ready for a short break"
            PetMood.Cozy -> "$weather · staying warm in ${outfit.label.lowercase()} gear"
            PetMood.Excited -> "$weather · perfect exploring weather"
            PetMood.Ready -> "$weather · dressed for the trip"
        }
    }

    private fun buildTravelTip(
        environment: PetEnvironmentSnapshot,
        mood: PetMood,
        outfit: PetOutfit,
    ): String {
        if (mood == PetMood.Tired) {
            return "You have been moving for ${environment.activeMinutes} minutes. " +
                "Buddy suggests a short rest and some water."
        }

        return when (outfit) {
            PetOutfit.Raincoat -> "Rain gear is on. Consider an indoor stop and keep the camera dry."
            PetOutfit.Windbreaker -> "It is windy outside. Buddy has packed a windbreaker."
            PetOutfit.Winter -> "It is chilly. Add a warm layer before the next stop."
            PetOutfit.Sunshine -> "Sunny day ahead. Sunglasses and water are ready."
            PetOutfit.Everyday -> "Buddy is ready to explore ${environment.locationLabel}."
        }
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
        PetWeatherCondition.Storm -> "Storm"
        PetWeatherCondition.Snow -> "Snow"
        PetWeatherCondition.Unknown -> "Weather"
    }
}
