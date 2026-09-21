package com.group5.roammate.pet

/** Visible actions used by the companion stage and the animation sprite set. */
enum class PetAction(val label: String) {
    Idle("Relaxing"),
    Blink("Blinking"),
    Curious("Curious"),
    Inspect("Exploring"),
    Walk("Walking"),
    Happy("Happy"),
    Sad("Needs a hug"),
    Yawn("Sleepy"),
    Sleep("Napping"),
    Petted("Loved"),
    Surprised("Surprised"),
    Treat("Snack time"),
    Dragged("Going for a ride"),
}

enum class PetInteraction {
    TapHead,
    TapBody,
    DoubleTap,
    LongPress,
    Drag,
    Treat,
}

data class PetBehaviorCue(
    val action: PetAction,
    val message: String,
    val durationMillis: Long,
)

/**
 * Pure behavior rules shared by the UI and tests.
 *
 * A mood is a durable state, not a playlist of random reactions. The avatar continuously animates
 * inside [restingAction] (breathing, blinking, tail/ear movement) until a real input temporarily
 * interrupts it. This keeps Buddy feeling alive without abruptly becoming happy, sad and asleep
 * every few seconds.
 */
object PetBehaviorEngine {
    fun restingAction(mood: PetMood): PetAction = when (mood) {
        PetMood.Ready -> PetAction.Idle
        PetMood.Excited -> PetAction.Happy
        PetMood.Cozy -> PetAction.Sleep
        PetMood.Tired -> PetAction.Sad
    }

    fun reaction(interaction: PetInteraction): PetBehaviorCue {
        val action = when (interaction) {
            PetInteraction.TapHead, PetInteraction.LongPress -> PetAction.Petted
            PetInteraction.TapBody -> PetAction.Happy
            PetInteraction.DoubleTap -> PetAction.Surprised
            PetInteraction.Drag -> PetAction.Dragged
            PetInteraction.Treat -> PetAction.Treat
        }

        val message = when (interaction) {
            PetInteraction.TapHead -> "A very slow, very happy koala smile."
            PetInteraction.TapBody -> "Buddy gives you a tiny eucalyptus-powered cheer."
            PetInteraction.DoubleTap -> "That woke Buddy up! Just for a moment."
            PetInteraction.LongPress -> "Buddy relaxes under your hand and almost falls asleep."
            PetInteraction.Drag -> "Easy does it—Buddy prefers the scenic route."
            PetInteraction.Treat -> "Fresh leaves! Buddy takes a calm little nibble."
        }

        return PetBehaviorCue(action, message, durationFor(action))
    }

    /**
     * A shake asks Buddy for context-aware help. It alternates between personal care and the next
     * itinerary stop, while severe weather always wins over a generic travel suggestion.
     */
    fun weatherAdvice(
        weather: PetWeatherSnapshot,
        tripContext: PetTripContext = PetTripContext(),
        variation: Int = 0,
    ): PetBehaviorCue {
        if (!weather.isCurrent) return PetBehaviorCue(
            PetAction.Curious, "I can't check fresh weather yet. Let's take a water break while it reconnects.", 5_500L,
        )
        val nextStop = tripContext.nextStopName
        val timedStop = when {
            nextStop == null -> null
            tripContext.nextStopTime.isNullOrBlank() -> nextStop
            else -> "$nextStop at ${tripContext.nextStopTime}"
        }
        val itineraryEnding = timedStop?.let { " ${if (tripContext.isDemo) "Sample plan" else "Next stop"}: $it. Allow extra travel time." }
            ?: " Keep the next part of your trip flexible."

        val message = when {
            weather.condition == PetWeatherCondition.Storm ->
                "The weather feed reports thunderstorms—stay under cover and postpone exposed outdoor stops." +
                    itineraryEnding
            weather.condition == PetWeatherCondition.Snow || weather.temperatureC <= 9.0 ->
                "It is ${weather.temperatureC.toInt()}°C—wear a warm layer and watch for slippery paths." +
                    itineraryEnding
            weather.condition == PetWeatherCondition.Rain ->
                "Rain is around ${weather.locationLabel}—take an umbrella and choose a covered route." +
                    itineraryEnding
            weather.windSpeedKmh >= 25.0 ->
                "Winds are near ${weather.windSpeedKmh.toInt()} km/h—secure loose items and check transport." +
                    itineraryEnding
            weather.condition == PetWeatherCondition.Fog ->
                "Visibility may be low—slow down and leave earlier for your next stop." +
                    itineraryEnding
            weather.isDay && weather.condition == PetWeatherCondition.Clear && weather.temperatureC >= 25.0 ->
                "It is warm and sunny—bring water, sunscreen and a shady break." +
                    itineraryEnding
            variation % 2 == 0 ->
                "${weather.label} right now—Buddy thinks a light layer and a water bottle are a safe bet."
            timedStop != null ->
                "${if (tripContext.isDemo) "Sample plan" else "Next up"}: $timedStop. Conditions look manageable, so this is a good time to get moving."
            else ->
                "Conditions look comfortable. Buddy votes for a relaxed walk and a photo stop."
        }

        val action = when {
            weather.condition == PetWeatherCondition.Storm -> PetAction.Surprised
            weather.condition == PetWeatherCondition.Snow || weather.temperatureC <= 9.0 ->
                PetAction.Curious
            else -> PetAction.Happy
        }
        return PetBehaviorCue(action, message, 7_000L)
    }

    fun durationFor(action: PetAction): Long = when (action) {
        PetAction.Blink -> 650L
        PetAction.Curious -> 2_300L
        PetAction.Inspect -> 2_600L
        PetAction.Walk -> 3_200L
        PetAction.Happy -> 2_000L
        PetAction.Sad -> 2_800L
        PetAction.Yawn -> 2_200L
        PetAction.Sleep -> 5_500L
        PetAction.Petted -> 2_400L
        PetAction.Surprised -> 1_700L
        PetAction.Treat -> 2_600L
        PetAction.Dragged -> 900L
        PetAction.Idle -> 2_000L
    }
}
