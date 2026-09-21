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
    Shake,
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
            PetInteraction.DoubleTap, PetInteraction.Shake -> PetAction.Surprised
            PetInteraction.Drag -> PetAction.Dragged
            PetInteraction.Treat -> PetAction.Treat
        }

        val message = when (interaction) {
            PetInteraction.TapHead -> "A very slow, very happy koala smile."
            PetInteraction.TapBody -> "Buddy gives you a tiny eucalyptus-powered cheer."
            PetInteraction.DoubleTap -> "That woke Buddy up! Just for a moment."
            PetInteraction.LongPress -> "Buddy relaxes under your hand and almost falls asleep."
            PetInteraction.Drag -> "Easy does it—Buddy prefers the scenic route."
            PetInteraction.Shake -> "Buddy hugs the eucalyptus branch and looks around."
            PetInteraction.Treat -> "Fresh leaves! Buddy takes a calm little nibble."
        }

        return PetBehaviorCue(action, message, durationFor(action))
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
