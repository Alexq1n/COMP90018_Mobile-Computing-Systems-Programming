package com.group5.roammate.ui.pet

import com.group5.roammate.pet.CompanionStyle
import com.group5.roammate.pet.PetAction
import com.group5.roammate.pet.PetOutfit

/** Six authored pixel-animation loops shared by the sprite catalogue and fitted outfits. */
internal enum class PetSpriteLoop(val resourceKey: String) {
    Idle("idle"),
    Walk("walk"),
    Sleep("sleep"),
    Happy("happy"),
    Sad("sad"),
    Petted("petted"),
}

internal fun PetAction.spriteLoop(): PetSpriteLoop = when (this) {
    PetAction.Idle,
    PetAction.Blink,
    PetAction.Curious,
    PetAction.Inspect,
    -> PetSpriteLoop.Idle

    PetAction.Walk,
    PetAction.Dragged,
    -> PetSpriteLoop.Walk

    PetAction.Happy,
    PetAction.Surprised,
    PetAction.Treat,
    -> PetSpriteLoop.Happy

    PetAction.Sad -> PetSpriteLoop.Sad
    PetAction.Yawn,
    PetAction.Sleep,
    -> PetSpriteLoop.Sleep

    PetAction.Petted -> PetSpriteLoop.Petted
}

/**
 * Resource names are deterministic: 4 companions × 5 fitted outfits × 6 loops × 4 frames.
 * Keeping this function pure makes the entire sprite catalogue easy to validate in unit tests.
 */
internal fun CompanionStyle.frameResourceName(
    outfit: PetOutfit,
    action: PetAction,
    phase: Int,
): String = buildString {
    append("pet_pixel_")
    append(name.lowercase())
    append('_')
    append(outfit.name.lowercase())
    append('_')
    append(action.spriteLoop().resourceKey)
    append('_')
    append(Math.floorMod(phase, 4))
}
