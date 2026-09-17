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
            style = CompanionStyle.Corgi,
            interaction = PetInteraction.TapHead,
            locationLabel = "Melbourne",
        )
        val drag = PetBehaviorEngine.reaction(
            style = CompanionStyle.Penguin,
            interaction = PetInteraction.Drag,
            locationLabel = "St Kilda",
        )

        assertEquals(PetAction.Petted, pat.action)
        assertEquals(PetAction.Dragged, drag.action)
        assertTrue(pat.message.isNotBlank())
        assertTrue(drag.durationMillis > 0)
    }
}
