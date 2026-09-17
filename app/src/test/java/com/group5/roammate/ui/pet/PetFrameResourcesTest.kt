package com.group5.roammate.ui.pet

import com.group5.roammate.pet.CompanionStyle
import com.group5.roammate.pet.PetAction
import com.group5.roammate.pet.PetOutfit
import org.junit.Assert.assertEquals
import org.junit.Test

class PetFrameResourcesTest {
    @Test
    fun idleBlinkLivesInsideTheIdleFrameSequence() {
        assertEquals(
            "pet_pixel_corgi_raincoat_idle_2",
            CompanionStyle.Corgi.frameResourceName(
                PetOutfit.Raincoat,
                PetAction.Idle,
                2,
            ),
        )
        assertEquals(PetSpriteLoop.Idle, PetAction.Blink.spriteLoop())
    }

    @Test
    fun reactionsSelectTheirAuthoredContinuousLoops() {
        assertEquals(PetSpriteLoop.Happy, PetAction.Treat.spriteLoop())
        assertEquals(PetSpriteLoop.Walk, PetAction.Dragged.spriteLoop())
        assertEquals(PetSpriteLoop.Petted, PetAction.Petted.spriteLoop())
        assertEquals(
            "pet_pixel_penguin_winter_happy_3",
            CompanionStyle.Penguin.frameResourceName(
                PetOutfit.Winter,
                PetAction.Surprised,
                -1,
            ),
        )
    }
}
