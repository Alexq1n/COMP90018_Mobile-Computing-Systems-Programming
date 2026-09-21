package com.group5.roammate.ui.pet

import com.group5.roammate.pet.CompanionStyle
import com.group5.roammate.pet.PetAction
import com.group5.roammate.pet.PetOutfit
import org.junit.Assert.assertEquals
import org.junit.Test

class PetFrameResourcesTest {
    @Test
    fun everyOutfitLoopAndFrameHasADeterministicResourceName() {
        val expectedOutfitKeys = listOf(
            "everyday",
            "sunshine",
            "raincoat",
            "windbreaker",
            "winter",
            "explorer",
            "streetwear",
            "festival",
            "pajamas",
        )
        val representativeActions = listOf(
            PetAction.Idle to "idle",
            PetAction.Walk to "walk",
            PetAction.Sleep to "sleep",
            PetAction.Happy to "happy",
            PetAction.Sad to "sad",
            PetAction.Petted to "petted",
        )

        assertEquals(expectedOutfitKeys, PetOutfit.values().map { it.name.lowercase() })

        val actualNames = buildList {
            PetOutfit.values().forEach { outfit ->
                representativeActions.forEach { (action, loopKey) ->
                    repeat(4) { frame ->
                        val expected =
                            "pet_pixel_koala_${outfit.name.lowercase()}_${loopKey}_$frame"
                        val actual = CompanionStyle.Koala.frameResourceName(outfit, action, frame)
                        assertEquals(expected, actual)
                        add(actual)
                    }
                }
            }
        }

        assertEquals(9 * 6 * 4, actualNames.size)
        assertEquals(actualNames.size, actualNames.toSet().size)
    }

    @Test
    fun framePhaseWrapsWithinTheFourAuthoredFrames() {
        assertEquals(
            "pet_pixel_koala_raincoat_idle_3",
            CompanionStyle.Koala.frameResourceName(PetOutfit.Raincoat, PetAction.Blink, -1),
        )
        assertEquals(
            "pet_pixel_koala_winter_happy_0",
            CompanionStyle.Koala.frameResourceName(PetOutfit.Winter, PetAction.Surprised, 4),
        )
    }
}
