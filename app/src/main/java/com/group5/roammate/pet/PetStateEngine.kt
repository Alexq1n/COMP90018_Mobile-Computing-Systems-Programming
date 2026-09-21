package com.group5.roammate.pet

object PetStateEngine {
    fun buildUiState(profile: PetProfile): PetUiState = PetUiState(
        companionStyle = CompanionStyle.Koala,
        outfit = profile.selectedOutfit,
        mood = PetMood.Ready,
        statusLine = statusFor(profile.selectedOutfit),
    )

    fun outfitAfter(current: PetOutfit, steps: Int): PetOutfit {
        val outfits = PetOutfit.entries
        return outfits[Math.floorMod(outfits.indexOf(current) + steps, outfits.size)]
    }

    private fun statusFor(outfit: PetOutfit): String = when (outfit) {
        PetOutfit.Everyday -> "Buddy is relaxed and ready to play."
        PetOutfit.Sunshine -> "Buddy is feeling bright in those tiny shades."
        PetOutfit.Raincoat -> "Buddy looks snug in the little raincoat."
        PetOutfit.Windbreaker -> "Buddy is ready for a breezy adventure."
        PetOutfit.Winter -> "Buddy is cozy and warm in this outfit."
    }
}
