package com.group5.roammate.pet

import android.content.Context

/** Persists whether Buddy follows weather or wears one explicitly selected fashion outfit. */
class PetPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun loadProfile(): PetProfile {
        val savedChoice = preferences.getString(KEY_WARDROBE_CHOICE, null)
        val wardrobeChoice = PetWardrobeChoice.entries.firstOrNull { it.name == savedChoice }
            ?: legacyChoice(
                preferences.getString(KEY_SELECTED_OUTFIT, null)
                    ?: preferences.getString(LEGACY_KEY_OUTFIT_MODE, null),
            )
        return PetProfile(wardrobeChoice = wardrobeChoice)
    }

    fun saveProfile(profile: PetProfile) {
        preferences.edit()
            .putString(KEY_WARDROBE_CHOICE, profile.wardrobeChoice.name)
            .remove(KEY_SELECTED_OUTFIT)
            .remove(LEGACY_KEY_OUTFIT_MODE)
            .apply()
    }

    private fun legacyChoice(savedOutfit: String?): PetWardrobeChoice {
        val outfit = PetOutfit.entries.firstOrNull { it.name == savedOutfit }
            ?: return PetWardrobeChoice.Weather
        return PetWardrobeChoice.entries.firstOrNull { it.manualOutfit == outfit }
            ?: PetWardrobeChoice.Weather
    }

    private companion object {
        const val FILE_NAME = "roammate_pet_preferences"
        const val KEY_WARDROBE_CHOICE = "wardrobe_choice"
        const val KEY_SELECTED_OUTFIT = "selected_outfit"
        const val LEGACY_KEY_OUTFIT_MODE = "outfit_mode"
    }
}
