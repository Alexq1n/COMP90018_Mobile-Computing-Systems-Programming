package com.group5.roammate.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetBehaviorEngineTest {
    @Test
    fun readyMoodKeepsAStableIdleState() {
        assertEquals(PetAction.Idle, PetBehaviorEngine.restingAction(PetMood.Ready))
    }

    @Test
    fun moodChangesSelectOneDurableLoop() {
        assertEquals(PetAction.Happy, PetBehaviorEngine.restingAction(PetMood.Excited))
        assertEquals(PetAction.Sleep, PetBehaviorEngine.restingAction(PetMood.Cozy))
        assertEquals(PetAction.Sad, PetBehaviorEngine.restingAction(PetMood.Tired))
    }

    @Test
    fun interactionMapsToExpectedVisibleReaction() {
        val pat = PetBehaviorEngine.reaction(
            interaction = PetInteraction.TapHead,
        )
        val drag = PetBehaviorEngine.reaction(
            interaction = PetInteraction.Drag,
        )

        assertEquals(PetAction.Petted, pat.action)
        assertEquals(PetAction.Dragged, drag.action)
        assertTrue(pat.message.isNotBlank())
        assertTrue(drag.durationMillis > 0)
    }

    @Test
    fun rainyWeatherAdviceMentionsAnUmbrellaAndTheNextStop() {
        val cue = advice(
            weather = weather(PetWeatherCondition.Rain),
            trip = PetTripContext(
                nextStopName = "Melbourne Museum",
                nextStopTime = "14:30",
            ),
        )

        assertContains(cue.message, "umbrella")
        assertContains(cue.message, "Melbourne Museum")
        assertTrue(cue.durationMillis > 0)
    }

    @Test
    fun stormAdvicePrioritisesSafety() {
        val cue = advice(weather(PetWeatherCondition.Storm))

        assertContainsAny(cue.message, "indoors", "shelter", "safe", "storm")
    }

    @Test
    fun coldAdviceSuggestsWarmClothing() {
        val cue = advice(
            weather(
                condition = PetWeatherCondition.Cloudy,
                temperatureC = 9.0,
            ),
        )

        assertContainsAny(cue.message, "warm", "coat", "layer")
    }

    @Test
    fun windyAdviceAcknowledgesStrongWind() {
        val cue = advice(
            weather(
                condition = PetWeatherCondition.Cloudy,
                windSpeedKmh = 25.0,
            ),
        )

        assertContainsAny(cue.message, "wind", "breezy", "gust")
    }

    @Test
    fun hotClearAdviceSuggestsHydrationOrSunProtection() {
        val cue = advice(
            weather(
                condition = PetWeatherCondition.Clear,
                temperatureC = 29.0,
            ),
        )

        assertContainsAny(cue.message, "water", "hydrate", "sunscreen", "shade")
    }

    @Test
    fun fogAdviceWarnsAboutVisibilityOrExtraTravelTime() {
        val cue = advice(weather(PetWeatherCondition.Fog))

        assertContainsAny(cue.message, "visibility", "see", "slow", "extra time")
    }

    private fun advice(
        weather: PetWeatherSnapshot,
        trip: PetTripContext = PetTripContext(
            nextStopName = "Federation Square",
            nextStopTime = "12:00",
        ),
    ): PetBehaviorCue = PetBehaviorEngine.weatherAdvice(
        weather = weather,
        tripContext = trip,
        variation = 0,
    )

    private fun weather(
        condition: PetWeatherCondition,
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

    private fun assertContains(actual: String, expected: String) {
        assertTrue(
            "Expected <$actual> to contain <$expected>",
            actual.contains(expected, ignoreCase = true),
        )
    }

    private fun assertContainsAny(actual: String, vararg expected: String) {
        assertTrue(
            "Expected <$actual> to contain one of ${expected.toList()}",
            expected.any { actual.contains(it, ignoreCase = true) },
        )
    }
}
