package com.group5.roammate.pet

import android.content.Context

/** Persists the one user choice that belongs to the single-koala experience: its outfit. */
class PetPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun loadProfile(): PetProfile {
        val savedOutfit = preferences.getString(KEY_SELECTED_OUTFIT, null)
            ?: preferences.getString(LEGACY_KEY_OUTFIT_MODE, null)
        val outfit = PetOutfit.entries.firstOrNull { it.name == savedOutfit }
            ?: PetOutfit.Everyday
        return PetProfile(selectedOutfit = outfit)
    }

    fun saveProfile(profile: PetProfile) {
        preferences.edit()
            .putString(KEY_SELECTED_OUTFIT, profile.selectedOutfit.name)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "roammate_pet_preferences"
        const val KEY_SELECTED_OUTFIT = "selected_outfit"
        const val LEGACY_KEY_OUTFIT_MODE = "outfit_mode"
    }
}
