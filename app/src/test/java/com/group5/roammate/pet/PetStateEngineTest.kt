package com.group5.roammate.pet

import org.junit.Assert.assertEquals
import org.junit.Test

class PetStateEngineTest {
    @Test
    fun productStateAlwaysUsesTheKoala() {
        val state = PetStateEngine.buildUiState(PetProfile())

        assertEquals(CompanionStyle.Koala, state.companionStyle)
        assertEquals(PetMood.Ready, state.mood)
    }

    @Test
    fun selectedOutfitIsUsedWithoutWeatherRules() {
        val state = PetStateEngine.buildUiState(
            PetProfile(selectedOutfit = PetOutfit.Winter),
        )

        assertEquals(PetOutfit.Winter, state.outfit)
    }

    @Test
    fun outfitNavigationWrapsInBothDirections() {
        assertEquals(
            PetOutfit.Sunshine,
            PetStateEngine.outfitAfter(PetOutfit.Everyday, 1),
        )
        assertEquals(
            PetOutfit.Winter,
            PetStateEngine.outfitAfter(PetOutfit.Everyday, -1),
        )
        assertEquals(
            PetOutfit.Everyday,
            PetStateEngine.outfitAfter(PetOutfit.Winter, 1),
        )
    }
}
