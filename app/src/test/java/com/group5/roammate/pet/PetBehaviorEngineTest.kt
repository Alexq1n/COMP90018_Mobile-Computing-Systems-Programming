package com.group5.roammate.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetBehaviorEngineTest {
    @Test
    fun companionsHaveDifferentAutonomousPersonalities() {
        val corgi = PetBehaviorEngine.autonomousActions(CompanionStyle.Corgi, PetMood.Ready)
        val koala = PetBehaviorEngine.autonomousActions(CompanionStyle.Koala, PetMood.Ready)
        val penguin = PetBehaviorEngine.autonomousActions(CompanionStyle.Penguin, PetMood.Ready)

        assertTrue(PetAction.Inspect in corgi)
        assertTrue(PetAction.Sleep in koala)
        assertTrue(PetAction.Walk in penguin)
        assertTrue(corgi != koala)
    }

    @Test
    fun tiredCompanionCanYawnSleepAndAskForComfort() {
        val actions = PetBehaviorEngine.autonomousActions(
            CompanionStyle.Kangaroo,
            PetMood.Tired,
        )

        assertTrue(PetAction.Yawn in actions)
        assertTrue(PetAction.Sleep in actions)
        assertTrue(PetAction.Sad in actions)
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
