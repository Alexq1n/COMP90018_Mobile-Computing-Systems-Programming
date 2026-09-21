package com.group5.roammate.pet

/** RoamMate intentionally exposes one companion so its interaction can stay focused and personal. */
enum class CompanionStyle(
    val displayName: String,
) {
    Koala("Koala"),
}

enum class PetOutfit(
    val label: String,
) {
    Everyday("Everyday"),
    Sunshine("Sunshine"),
    Raincoat("Raincoat"),
    Windbreaker("Windbreaker"),
    Winter("Winter"),
}

enum class PetMood(
    val label: String,
) {
    Ready("Ready"),
    Excited("Excited"),
    Cozy("Cozy"),
    Tired("Tired"),
}

data class PetUiState(
    val companionStyle: CompanionStyle,
    val outfit: PetOutfit,
    val mood: PetMood,
    val statusLine: String,
)

data class PetProfile(
    val selectedOutfit: PetOutfit = PetOutfit.Everyday,
)
