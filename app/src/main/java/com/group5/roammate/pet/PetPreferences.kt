package com.group5.roammate.pet

import android.content.Context

/**
 * Small local persistence layer for the currently selected Buddy appearance and wardrobe mode.
 * Yuxiang can later mirror these two string values in the user's Firebase document without
 * changing the UI-facing PetProfile model.
 */
class PetPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun loadProfile(): PetProfile {
        val style = preferences.getString(KEY_STYLE, null)
            ?.let { saved -> CompanionStyle.entries.firstOrNull { it.name == saved } }
            ?: CompanionStyle.Corgi
        val outfitMode = preferences.getString(KEY_OUTFIT_MODE, null)
            ?.let { saved -> PetOutfitMode.entries.firstOrNull { it.name == saved } }
            ?: PetOutfitMode.Auto
        val checkIns = preferences.getStringSet(KEY_CHECK_INS, null)
            ?.toSet()
            ?: PetProfile().checkedInPlaces

        return PetProfile(
            selectedStyle = style,
            outfitMode = outfitMode,
            checkedInPlaces = checkIns,
        )
    }

    fun saveProfile(profile: PetProfile) {
        preferences.edit()
            .putString(KEY_STYLE, profile.selectedStyle.name)
            .putString(KEY_OUTFIT_MODE, profile.outfitMode.name)
            .putStringSet(KEY_CHECK_INS, profile.checkedInPlaces)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "roammate_pet_preferences"
        const val KEY_STYLE = "selected_style"
        const val KEY_OUTFIT_MODE = "outfit_mode"
        const val KEY_CHECK_INS = "checked_in_places"
    }
}
