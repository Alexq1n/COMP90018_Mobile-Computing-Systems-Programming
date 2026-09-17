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

    fun reaction(
        style: CompanionStyle,
        interaction: PetInteraction,
        locationLabel: String,
    ): PetBehaviorCue {
        val action = when (interaction) {
            PetInteraction.TapHead, PetInteraction.LongPress -> PetAction.Petted
            PetInteraction.TapBody -> PetAction.Happy
            PetInteraction.DoubleTap, PetInteraction.Shake -> PetAction.Surprised
            PetInteraction.Drag -> PetAction.Dragged
            PetInteraction.Treat -> PetAction.Treat
        }

        val message = when (style) {
            CompanionStyle.Corgi -> when (interaction) {
                PetInteraction.TapHead -> "Buddy leans into your hand. Tail-wag level: maximum!"
                PetInteraction.TapBody -> "Play time? Buddy is ready to race around $locationLabel."
                PetInteraction.DoubleTap -> "Two taps! Buddy thought someone rang the doorbell."
                PetInteraction.LongPress -> "Buddy closes their eyes and asks for more pats."
                PetInteraction.Drag -> "Wheee! Put Buddy somewhere with a good view."
                PetInteraction.Shake -> "Adventure detected! Buddy is checking every direction."
                PetInteraction.Treat -> "Crunch! Buddy carefully checks the ground for one more snack."
            }

            CompanionStyle.Koala -> when (interaction) {
                PetInteraction.TapHead -> "A very slow, very happy koala smile."
                PetInteraction.TapBody -> "Buddy gives you a tiny eucalyptus-powered cheer."
                PetInteraction.DoubleTap -> "That woke Buddy up! Just for a moment."
                PetInteraction.LongPress -> "Buddy relaxes under your hand and almost falls asleep."
                PetInteraction.Drag -> "Easy does it—Buddy prefers the scenic route."
                PetInteraction.Shake -> "Buddy hugs the eucalyptus branch and looks around."
                PetInteraction.Treat -> "Fresh leaves! Buddy takes a calm little nibble."
            }

            CompanionStyle.Penguin -> when (interaction) {
                PetInteraction.TapHead -> "Buddy chirps and happily flaps both wings."
                PetInteraction.TapBody -> "A wobbly dance begins in the middle of $locationLabel."
                PetInteraction.DoubleTap -> "Surprise! Buddy nearly waddled the wrong way."
                PetInteraction.LongPress -> "Buddy stays perfectly still for the warm head pat."
                PetInteraction.Drag -> "Sliding practice! Buddy is enjoying the ride."
                PetInteraction.Shake -> "Buddy spins, checks the route, and strikes a pose."
                PetInteraction.Treat -> "One travel snack, swallowed with an enthusiastic chirp."
            }

            CompanionStyle.Kangaroo -> when (interaction) {
                PetInteraction.TapHead -> "Buddy's ears perk up before a grateful little nuzzle."
                PetInteraction.TapBody -> "One happy bounce—and Buddy is ready to explore."
                PetInteraction.DoubleTap -> "Buddy freezes, ears up, then checks the horizon."
                PetInteraction.LongPress -> "Buddy settles down and lets both ears relax."
                PetInteraction.Drag -> "A shortcut without hopping? Buddy could get used to this."
                PetInteraction.Shake -> "Route check complete! Buddy is alert and ready."
                PetInteraction.Treat -> "Snack secured. Buddy checks the pouch for later."
            }
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
