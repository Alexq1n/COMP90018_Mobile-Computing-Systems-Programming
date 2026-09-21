package com.group5.roammate.pet

import org.junit.Assert.assertEquals
import org.junit.Test

class PetStateEngineTest {
    @Test
    fun productStateAlwaysUsesTheKoala() {
        val state = PetStateEngine.buildUiState(
            profile = PetProfile(),
            weather = PetWeatherSnapshot.demo(),
        )

        assertEquals(CompanionStyle.Koala, state.companionStyle)
        assertEquals(PetMood.Ready, state.mood)
    }

    @Test
    fun weatherWardrobeUsesWinterGearForSnowOrColdConditions() {
        assertEquals(
            PetOutfit.Winter,
            weatherState(condition = PetWeatherCondition.Snow, temperatureC = 15.0).outfit,
        )
        assertEquals(
            PetOutfit.Winter,
            weatherState(condition = PetWeatherCondition.Cloudy, temperatureC = 9.0).outfit,
        )
    }

    @Test
    fun weatherWardrobePrefersRaincoatForRainAndStorms() {
        assertEquals(
            PetOutfit.Raincoat,
            weatherState(condition = PetWeatherCondition.Rain, windSpeedKmh = 40.0).outfit,
        )
        assertEquals(
            PetOutfit.Raincoat,
            weatherState(condition = PetWeatherCondition.Storm, temperatureC = 18.0).outfit,
        )
    }

    @Test
    fun weatherWardrobeUsesWindbreakerForStrongWind() {
        assertEquals(
            PetOutfit.Windbreaker,
            weatherState(
                condition = PetWeatherCondition.Cloudy,
                temperatureC = 18.0,
                windSpeedKmh = 25.0,
            ).outfit,
        )
    }

    @Test
    fun weatherWardrobeUsesSunshineLookOnlyWhenClearAndHot() {
        assertEquals(
            PetOutfit.Sunshine,
            weatherState(
                condition = PetWeatherCondition.Clear,
                temperatureC = 25.0,
                windSpeedKmh = 8.0,
            ).outfit,
        )
        assertEquals(
            PetOutfit.Everyday,
            weatherState(
                condition = PetWeatherCondition.Clear,
                temperatureC = 24.9,
                windSpeedKmh = 8.0,
            ).outfit,
        )
    }

    @Test
    fun weatherWardrobeFallsBackToEverydayForMildConditions() {
        assertEquals(
            PetOutfit.Everyday,
            weatherState(
                condition = PetWeatherCondition.Fog,
                temperatureC = 17.0,
                windSpeedKmh = 7.0,
            ).outfit,
        )
    }

    @Test
    fun protectiveGearOverridesButPreservesSelectedFashion() {
        val rainy = weather(condition = PetWeatherCondition.Rain)
        val state = PetStateEngine.buildUiState(
            profile = PetProfile(wardrobeChoice = PetWardrobeChoice.Explorer),
            weather = rainy,
        )

        assertEquals(PetOutfit.Raincoat, state.outfit)
        assertEquals(PetWardrobeChoice.Explorer, state.wardrobeChoice)
        val mild = rainy.copy(condition = PetWeatherCondition.Cloudy)
        assertEquals(PetOutfit.Explorer, PetStateEngine.buildUiState(PetProfile(state.wardrobeChoice), mild).outfit)
    }

    @Test
    fun wardrobeNavigationWrapsInBothDirections() {
        assertEquals(
            PetWardrobeChoice.Explorer,
            PetStateEngine.wardrobeAfter(PetWardrobeChoice.Weather, 1),
        )
        assertEquals(
            PetWardrobeChoice.Pajamas,
            PetStateEngine.wardrobeAfter(PetWardrobeChoice.Weather, -1),
        )
        assertEquals(
            PetWardrobeChoice.Weather,
            PetStateEngine.wardrobeAfter(PetWardrobeChoice.Pajamas, 1),
        )
    }

    @Test
    fun wmoCodesCoverEveryAnimatedWeatherScene() {
        assertEquals(PetWeatherCondition.Clear, PetWeatherAdapter.fromWmoCode(0))
        assertEquals(PetWeatherCondition.Cloudy, PetWeatherAdapter.fromWmoCode(2))
        assertEquals(PetWeatherCondition.Fog, PetWeatherAdapter.fromWmoCode(45))
        assertEquals(PetWeatherCondition.Rain, PetWeatherAdapter.fromWmoCode(63))
        assertEquals(PetWeatherCondition.Snow, PetWeatherAdapter.fromWmoCode(75))
        assertEquals(PetWeatherCondition.Storm, PetWeatherAdapter.fromWmoCode(95))
        assertEquals(PetWeatherCondition.Unknown, PetWeatherAdapter.fromWmoCode(-1))
    }

    @Test
    fun staleWeatherCannotForceClothing() {
        val oldRain = weather(PetWeatherCondition.Rain).copy(observedAtMillis = System.currentTimeMillis() - 46 * 60_000L)
        assertEquals(PetOutfit.Pajamas, PetStateEngine.resolveOutfit(oldRain, PetWardrobeChoice.Pajamas))
        assertEquals(PetOutfit.Everyday, PetStateEngine.resolveOutfit(PetWeatherSnapshot.demo(), PetWardrobeChoice.Weather))
    }

    @Test
    fun nightDoesNotSelectSunHat() {
        val night = weather(PetWeatherCondition.Clear, temperatureC = 29.0).copy(isDay = false)
        assertEquals(PetOutfit.Everyday, PetStateEngine.resolveOutfit(night, PetWardrobeChoice.Weather))
    }

    @Test
    fun staleAndFutureTimestampsAreRejected() {
        val now = 1_000_000L
        val weather = weather().copy(observedAtMillis = now)
        org.junit.Assert.assertTrue(weather.isFreshAt(now))
        org.junit.Assert.assertFalse(weather.isFreshAt(now - 1))
        org.junit.Assert.assertFalse(weather.isFreshAt(now + 46 * 60_000L))
    }

    private fun weatherState(
        condition: PetWeatherCondition,
        temperatureC: Double = 18.0,
        windSpeedKmh: Double = 8.0,
    ): PetUiState = PetStateEngine.buildUiState(
        profile = PetProfile(wardrobeChoice = PetWardrobeChoice.Weather),
        weather = weather(condition, temperatureC, windSpeedKmh),
    )

    private fun weather(
        condition: PetWeatherCondition = PetWeatherCondition.Clear,
        temperatureC: Double = 18.0,
        windSpeedKmh: Double = 8.0,
    ) = PetWeatherSnapshot(
        condition = condition,
        label = condition.name,
        temperatureC = temperatureC,
        windSpeedKmh = windSpeedKmh,
        locationLabel = "Test location",
        source = "Unit test",
        observedAtMillis = System.currentTimeMillis(),
    )
}
