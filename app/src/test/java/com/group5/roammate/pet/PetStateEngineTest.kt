package com.group5.roammate.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetStateEngineTest {
    @Test
    fun autoWardrobePrefersRaincoatForRain() {
        val environment = environment(
            condition = PetWeatherCondition.Rain,
            temperatureC = 18.0,
            windSpeedKmh = 35.0,
        )

        assertEquals(
            PetOutfit.Raincoat,
            PetStateEngine.resolveOutfit(environment, PetOutfitMode.Auto),
        )
    }

    @Test
    fun autoWardrobeUsesWinterGearForColdWeather() {
        val environment = environment(
            condition = PetWeatherCondition.Cloudy,
            temperatureC = 8.5,
        )

        assertEquals(
            PetOutfit.Winter,
            PetStateEngine.resolveOutfit(environment, PetOutfitMode.Auto),
        )
    }

    @Test
    fun manualWardrobeOverridesLiveWeather() {
        val rainy = environment(condition = PetWeatherCondition.Rain)

        assertEquals(
            PetOutfit.Sunshine,
            PetStateEngine.resolveOutfit(rainy, PetOutfitMode.Sunshine),
        )
    }

    @Test
    fun activityThresholdMakesBuddyTired() {
        assertEquals(
            PetMood.Tired,
            PetStateEngine.resolveMood(environment(activeMinutes = 40)),
        )
        assertEquals(
            PetMood.Tired,
            PetStateEngine.resolveMood(environment(stepsSinceBreak = 4_000)),
        )
    }

    @Test
    fun checkInsUnlockTravelLooks() {
        val unlocked = PetStateEngine.unlockedStyles(
            setOf("Melbourne CBD", "St Kilda Beach", "Great Ocean Road"),
        )

        assertTrue(unlocked.containsAll(CompanionStyle.entries))
    }

    @Test
    fun wmoCodesMapToPetWeather() {
        assertEquals(PetWeatherCondition.Clear, PetWeatherAdapter.fromWmoCode(0))
        assertEquals(PetWeatherCondition.Rain, PetWeatherAdapter.fromWmoCode(63))
        assertEquals(PetWeatherCondition.Snow, PetWeatherAdapter.fromWmoCode(75))
        assertEquals(PetWeatherCondition.Storm, PetWeatherAdapter.fromWmoCode(96))
    }

    private fun environment(
        condition: PetWeatherCondition = PetWeatherCondition.Clear,
        temperatureC: Double = 20.0,
        windSpeedKmh: Double = 5.0,
        activeMinutes: Int = 0,
        stepsSinceBreak: Int = 0,
    ) = PetEnvironmentSnapshot(
        condition = condition,
        conditionLabel = PetWeatherAdapter.label(condition),
        temperatureC = temperatureC,
        windSpeedKmh = windSpeedKmh,
        activeMinutes = activeMinutes,
        stepsSinceBreak = stepsSinceBreak,
        locationLabel = "Test location",
        source = "Test",
    )
}
