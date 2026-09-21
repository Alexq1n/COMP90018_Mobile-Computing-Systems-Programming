package com.group5.roammate.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetDialogueEngineTest {
    @Test fun missingWeatherDoesNotInventRainOrTemperature() {
        val reply = reply("What's the weather?", weather = PetWeatherSnapshot.demo())
        assertTrue(reply.text.contains("don't have fresh weather"))
        assertFalse(reply.text.contains("18 degrees"))
        assertFalse(reply.text.contains("Bring rain"))
    }

    @Test fun staleWeatherIsNotDescribedAsCurrent() {
        val reply = reply("weather", weather = weather().copy(observedAtMillis = System.currentTimeMillis() - 46 * 60_000L))
        assertTrue(reply.text.contains("don't have fresh weather"))
        assertFalse(reply.text.contains("rainy"))
    }

    @Test fun currentWeatherIsNotClaimedToBeAnAllDayForecast() {
        val reply = reply("today's weather")
        assertTrue(reply.text.contains("rainy, 17 degrees"))
        assertTrue(reply.text.contains("not an all-day forecast"))
    }

    @Test fun availableDailyForecastIncludesDateAndRainProbability() {
        val reply = reply("weather", weather = weather().copy(todayLowC = 12.0, todayHighC = 21.0, todayRainChancePercent = 80, forecastDate = "2026-09-21"))
        assertTrue(reply.text.contains("2026-09-21"))
        assertTrue(reply.text.contains("12 to 21"))
        assertTrue(reply.text.contains("80 percent"))
    }

    @Test fun itineraryDemonstrationDataIsClearlyLabelled() {
        val reply = reply("Where do we go next?", trip = PetTripContext("Museum", "10:00", isDemo = true))
        assertEquals(PetDialogueTopic.Trip, reply.topic)
        assertTrue(reply.text.contains("sample itinerary"))
        assertTrue(reply.text.contains("Museum at 10:00"))
    }

    @Test fun absentItineraryDoesNotInventADestination() {
        assertTrue(reply("next stop").text.contains("isn't a next stop"))
    }

    @Test fun notificationRequestDoesNotClaimToScheduleAnAlarm() {
        assertTrue(reply("Set a reminder for my trip").text.contains("haven't set a notification"))
    }

    @Test fun whyFollowUpRemembersClothingTopic() {
        val state = state(weather()).copy(outfit = PetOutfit.Raincoat, wardrobeChoice = PetWardrobeChoice.Explorer)
        val reply = PetDialogueEngine.reply("why?", state, PetTripContext(), false, PetDialogueLanguage.English, PetDialogueTopic.Clothes)
        assertEquals(PetDialogueTopic.Clothes, reply.topic)
        assertTrue(reply.text.contains("Raincoat"))
        assertTrue(reply.text.contains("Explorer is saved"))
    }

    @Test fun chineseCareUsesMovementContextAndDoesNotDiagnoseFatigue() {
        val reply = PetDialogueEngine.reply("我有点累", state(weather()), PetTripContext(), true, PetDialogueLanguage.Chinese)
        assertEquals(PetDialogueTopic.Care, reply.topic)
        assertTrue(reply.text.contains("正在走动"))
        assertTrue(reply.text.contains("歇一会儿"))
    }

    @Test fun greetingDoesNotMatchInsideUnrelatedEnglishWords() {
        assertEquals(PetDialogueTopic.Help, reply("this is a test").topic)
    }

    @Test fun hotNightOffersWaterAndACoolBreakInsteadOfSunProtection() {
        val reply = reply("weather", weather = weather().copy(
            condition = PetWeatherCondition.Clear, temperatureC = 28.0, isDay = false,
        ))
        assertTrue(reply.text.contains("after dark"))
        assertTrue(reply.text.contains("Bring water"))
        assertFalse(reply.text.contains("shade"))
        assertFalse(reply.text.contains("sunhat"))
    }

    @Test fun tomorrowRequestDoesNotImplyCurrentConditionsAreTomorrowsForecast() {
        val reply = reply("What's the forecast tomorrow?")
        assertTrue(reply.text.startsWith("I don't have a forecast for that future date."))
        assertTrue(reply.text.contains("latest weather"))
    }

    private fun reply(input: String, weather: PetWeatherSnapshot = weather(), trip: PetTripContext = PetTripContext()): PetDialogueReply =
        PetDialogueEngine.reply(input, state(weather), trip, false, PetDialogueLanguage.English)

    private fun state(weather: PetWeatherSnapshot) = PetUiState(
        companionStyle = CompanionStyle.Koala,
        outfit = PetOutfit.Raincoat,
        wardrobeChoice = PetWardrobeChoice.Weather,
        weather = weather,
        mood = PetMood.Ready,
        statusLine = "",
    )

    private fun weather() = PetWeatherSnapshot(
        condition = PetWeatherCondition.Rain, label = "Rain", temperatureC = 17.0, windSpeedKmh = 8.0,
        locationLabel = "Melbourne", source = "Open-Meteo", observedAtMillis = System.currentTimeMillis(),
    )
}
