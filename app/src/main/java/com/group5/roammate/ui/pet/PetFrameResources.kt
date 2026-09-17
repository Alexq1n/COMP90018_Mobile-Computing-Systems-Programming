package com.group5.roammate.ui.pet

import androidx.annotation.DrawableRes
import com.group5.roammate.R
import com.group5.roammate.pet.CompanionStyle
import com.group5.roammate.pet.PetAction
import com.group5.roammate.pet.PetOutfit

private const val FRAME_IDLE = 0
private const val FRAME_BLINK = 1
private const val FRAME_CURIOUS = 2
private const val FRAME_INSPECT = 3
private const val FRAME_WALK_A = 4
private const val FRAME_WALK_B = 5
private const val FRAME_HAPPY = 6
private const val FRAME_SAD = 7
private const val FRAME_YAWN = 8
private const val FRAME_SLEEP = 9
private const val FRAME_PETTED = 10
private const val FRAME_SURPRISED = 11

/**
 * Explicit resource mapping keeps all weather outfits safe from resource shrinking. Every set
 * contains twelve artwork frames drawn for that exact animal and outfit; no generic clothing
 * overlay is stretched across different body shapes.
 */
@DrawableRes
internal fun CompanionStyle.frameRes(
    outfit: PetOutfit,
    action: PetAction,
    phase: Int,
): Int {
    val frameIndex = when (action) {
        PetAction.Idle -> FRAME_IDLE
        PetAction.Blink -> FRAME_BLINK
        PetAction.Curious -> FRAME_CURIOUS
        PetAction.Inspect, PetAction.Treat -> FRAME_INSPECT
        PetAction.Walk -> if (phase % 2 == 0) FRAME_WALK_A else FRAME_WALK_B
        PetAction.Happy -> FRAME_HAPPY
        PetAction.Sad -> FRAME_SAD
        PetAction.Yawn -> FRAME_YAWN
        PetAction.Sleep -> FRAME_SLEEP
        PetAction.Petted -> FRAME_PETTED
        PetAction.Surprised, PetAction.Dragged -> FRAME_SURPRISED
    }
    return frameSet(outfit)[frameIndex]
}
private fun CompanionStyle.frameSet(outfit: PetOutfit): IntArray = when (this) {
    CompanionStyle.Corgi -> when (outfit) {
        PetOutfit.Everyday -> intArrayOf(
            R.drawable.pet_corgi_idle,
            R.drawable.pet_corgi_blink,
            R.drawable.pet_corgi_curious,
            R.drawable.pet_corgi_inspect,
            R.drawable.pet_corgi_walk_a,
            R.drawable.pet_corgi_walk_b,
            R.drawable.pet_corgi_happy,
            R.drawable.pet_corgi_sad,
            R.drawable.pet_corgi_yawn,
            R.drawable.pet_corgi_sleep,
            R.drawable.pet_corgi_petted,
            R.drawable.pet_corgi_surprised,
        )
        PetOutfit.Sunshine -> intArrayOf(
            R.drawable.pet_corgi_sunshine_idle,
            R.drawable.pet_corgi_sunshine_blink,
            R.drawable.pet_corgi_sunshine_curious,
            R.drawable.pet_corgi_sunshine_inspect,
            R.drawable.pet_corgi_sunshine_walk_a,
            R.drawable.pet_corgi_sunshine_walk_b,
            R.drawable.pet_corgi_sunshine_happy,
            R.drawable.pet_corgi_sunshine_sad,
            R.drawable.pet_corgi_sunshine_yawn,
            R.drawable.pet_corgi_sunshine_sleep,
            R.drawable.pet_corgi_sunshine_petted,
            R.drawable.pet_corgi_sunshine_surprised,
        )
        PetOutfit.Raincoat -> intArrayOf(
            R.drawable.pet_corgi_raincoat_idle,
            R.drawable.pet_corgi_raincoat_blink,
            R.drawable.pet_corgi_raincoat_curious,
            R.drawable.pet_corgi_raincoat_inspect,
            R.drawable.pet_corgi_raincoat_walk_a,
            R.drawable.pet_corgi_raincoat_walk_b,
            R.drawable.pet_corgi_raincoat_happy,
            R.drawable.pet_corgi_raincoat_sad,
            R.drawable.pet_corgi_raincoat_yawn,
            R.drawable.pet_corgi_raincoat_sleep,
            R.drawable.pet_corgi_raincoat_petted,
            R.drawable.pet_corgi_raincoat_surprised,
        )
        PetOutfit.Windbreaker -> intArrayOf(
            R.drawable.pet_corgi_windbreaker_idle,
            R.drawable.pet_corgi_windbreaker_blink,
            R.drawable.pet_corgi_windbreaker_curious,
            R.drawable.pet_corgi_windbreaker_inspect,
            R.drawable.pet_corgi_windbreaker_walk_a,
            R.drawable.pet_corgi_windbreaker_walk_b,
            R.drawable.pet_corgi_windbreaker_happy,
            R.drawable.pet_corgi_windbreaker_sad,
            R.drawable.pet_corgi_windbreaker_yawn,
            R.drawable.pet_corgi_windbreaker_sleep,
            R.drawable.pet_corgi_windbreaker_petted,
            R.drawable.pet_corgi_windbreaker_surprised,
        )
        PetOutfit.Winter -> intArrayOf(
            R.drawable.pet_corgi_winter_idle,
            R.drawable.pet_corgi_winter_blink,
            R.drawable.pet_corgi_winter_curious,
            R.drawable.pet_corgi_winter_inspect,
            R.drawable.pet_corgi_winter_walk_a,
            R.drawable.pet_corgi_winter_walk_b,
            R.drawable.pet_corgi_winter_happy,
            R.drawable.pet_corgi_winter_sad,
            R.drawable.pet_corgi_winter_yawn,
            R.drawable.pet_corgi_winter_sleep,
            R.drawable.pet_corgi_winter_petted,
            R.drawable.pet_corgi_winter_surprised,
        )
    }
    CompanionStyle.Koala -> when (outfit) {
        PetOutfit.Everyday -> intArrayOf(
            R.drawable.pet_koala_idle,
            R.drawable.pet_koala_blink,
            R.drawable.pet_koala_curious,
            R.drawable.pet_koala_inspect,
            R.drawable.pet_koala_walk_a,
            R.drawable.pet_koala_walk_b,
            R.drawable.pet_koala_happy,
            R.drawable.pet_koala_sad,
            R.drawable.pet_koala_yawn,
            R.drawable.pet_koala_sleep,
            R.drawable.pet_koala_petted,
            R.drawable.pet_koala_surprised,
        )
        PetOutfit.Sunshine -> intArrayOf(
            R.drawable.pet_koala_sunshine_idle,
            R.drawable.pet_koala_sunshine_blink,
            R.drawable.pet_koala_sunshine_curious,
            R.drawable.pet_koala_sunshine_inspect,
            R.drawable.pet_koala_sunshine_walk_a,
            R.drawable.pet_koala_sunshine_walk_b,
            R.drawable.pet_koala_sunshine_happy,
            R.drawable.pet_koala_sunshine_sad,
            R.drawable.pet_koala_sunshine_yawn,
            R.drawable.pet_koala_sunshine_sleep,
            R.drawable.pet_koala_sunshine_petted,
            R.drawable.pet_koala_sunshine_surprised,
        )
        PetOutfit.Raincoat -> intArrayOf(
            R.drawable.pet_koala_raincoat_idle,
            R.drawable.pet_koala_raincoat_blink,
            R.drawable.pet_koala_raincoat_curious,
            R.drawable.pet_koala_raincoat_inspect,
            R.drawable.pet_koala_raincoat_walk_a,
            R.drawable.pet_koala_raincoat_walk_b,
            R.drawable.pet_koala_raincoat_happy,
            R.drawable.pet_koala_raincoat_sad,
            R.drawable.pet_koala_raincoat_yawn,
            R.drawable.pet_koala_raincoat_sleep,
            R.drawable.pet_koala_raincoat_petted,
            R.drawable.pet_koala_raincoat_surprised,
        )
        PetOutfit.Windbreaker -> intArrayOf(
            R.drawable.pet_koala_windbreaker_idle,
            R.drawable.pet_koala_windbreaker_blink,
            R.drawable.pet_koala_windbreaker_curious,
            R.drawable.pet_koala_windbreaker_inspect,
            R.drawable.pet_koala_windbreaker_walk_a,
            R.drawable.pet_koala_windbreaker_walk_b,
            R.drawable.pet_koala_windbreaker_happy,
            R.drawable.pet_koala_windbreaker_sad,
            R.drawable.pet_koala_windbreaker_yawn,
            R.drawable.pet_koala_windbreaker_sleep,
            R.drawable.pet_koala_windbreaker_petted,
            R.drawable.pet_koala_windbreaker_surprised,
        )
        PetOutfit.Winter -> intArrayOf(
            R.drawable.pet_koala_winter_idle,
            R.drawable.pet_koala_winter_blink,
            R.drawable.pet_koala_winter_curious,
            R.drawable.pet_koala_winter_inspect,
            R.drawable.pet_koala_winter_walk_a,
            R.drawable.pet_koala_winter_walk_b,
            R.drawable.pet_koala_winter_happy,
            R.drawable.pet_koala_winter_sad,
            R.drawable.pet_koala_winter_yawn,
            R.drawable.pet_koala_winter_sleep,
            R.drawable.pet_koala_winter_petted,
            R.drawable.pet_koala_winter_surprised,
        )
    }
    CompanionStyle.Penguin -> when (outfit) {
        PetOutfit.Everyday -> intArrayOf(
            R.drawable.pet_penguin_idle,
            R.drawable.pet_penguin_blink,
            R.drawable.pet_penguin_curious,
            R.drawable.pet_penguin_inspect,
            R.drawable.pet_penguin_walk_a,
            R.drawable.pet_penguin_walk_b,
            R.drawable.pet_penguin_happy,
            R.drawable.pet_penguin_sad,
            R.drawable.pet_penguin_yawn,
            R.drawable.pet_penguin_sleep,
            R.drawable.pet_penguin_petted,
            R.drawable.pet_penguin_surprised,
        )
        PetOutfit.Sunshine -> intArrayOf(
            R.drawable.pet_penguin_sunshine_idle,
            R.drawable.pet_penguin_sunshine_blink,
            R.drawable.pet_penguin_sunshine_curious,
            R.drawable.pet_penguin_sunshine_inspect,
            R.drawable.pet_penguin_sunshine_walk_a,
            R.drawable.pet_penguin_sunshine_walk_b,
            R.drawable.pet_penguin_sunshine_happy,
            R.drawable.pet_penguin_sunshine_sad,
            R.drawable.pet_penguin_sunshine_yawn,
            R.drawable.pet_penguin_sunshine_sleep,
            R.drawable.pet_penguin_sunshine_petted,
            R.drawable.pet_penguin_sunshine_surprised,
        )
        PetOutfit.Raincoat -> intArrayOf(
            R.drawable.pet_penguin_raincoat_idle,
            R.drawable.pet_penguin_raincoat_blink,
            R.drawable.pet_penguin_raincoat_curious,
            R.drawable.pet_penguin_raincoat_inspect,
            R.drawable.pet_penguin_raincoat_walk_a,
            R.drawable.pet_penguin_raincoat_walk_b,
            R.drawable.pet_penguin_raincoat_happy,
            R.drawable.pet_penguin_raincoat_sad,
            R.drawable.pet_penguin_raincoat_yawn,
            R.drawable.pet_penguin_raincoat_sleep,
            R.drawable.pet_penguin_raincoat_petted,
            R.drawable.pet_penguin_raincoat_surprised,
        )
        PetOutfit.Windbreaker -> intArrayOf(
            R.drawable.pet_penguin_windbreaker_idle,
            R.drawable.pet_penguin_windbreaker_blink,
            R.drawable.pet_penguin_windbreaker_curious,
            R.drawable.pet_penguin_windbreaker_inspect,
            R.drawable.pet_penguin_windbreaker_walk_a,
            R.drawable.pet_penguin_windbreaker_walk_b,
            R.drawable.pet_penguin_windbreaker_happy,
            R.drawable.pet_penguin_windbreaker_sad,
            R.drawable.pet_penguin_windbreaker_yawn,
            R.drawable.pet_penguin_windbreaker_sleep,
            R.drawable.pet_penguin_windbreaker_petted,
            R.drawable.pet_penguin_windbreaker_surprised,
        )
        PetOutfit.Winter -> intArrayOf(
            R.drawable.pet_penguin_winter_idle,
            R.drawable.pet_penguin_winter_blink,
            R.drawable.pet_penguin_winter_curious,
            R.drawable.pet_penguin_winter_inspect,
            R.drawable.pet_penguin_winter_walk_a,
            R.drawable.pet_penguin_winter_walk_b,
            R.drawable.pet_penguin_winter_happy,
            R.drawable.pet_penguin_winter_sad,
            R.drawable.pet_penguin_winter_yawn,
            R.drawable.pet_penguin_winter_sleep,
            R.drawable.pet_penguin_winter_petted,
            R.drawable.pet_penguin_winter_surprised,
        )
    }
    CompanionStyle.Kangaroo -> when (outfit) {
        PetOutfit.Everyday -> intArrayOf(
            R.drawable.pet_kangaroo_idle,
            R.drawable.pet_kangaroo_blink,
            R.drawable.pet_kangaroo_curious,
            R.drawable.pet_kangaroo_inspect,
            R.drawable.pet_kangaroo_walk_a,
            R.drawable.pet_kangaroo_walk_b,
            R.drawable.pet_kangaroo_happy,
            R.drawable.pet_kangaroo_sad,
            R.drawable.pet_kangaroo_yawn,
            R.drawable.pet_kangaroo_sleep,
            R.drawable.pet_kangaroo_petted,
            R.drawable.pet_kangaroo_surprised,
        )
        PetOutfit.Sunshine -> intArrayOf(
            R.drawable.pet_kangaroo_sunshine_idle,
            R.drawable.pet_kangaroo_sunshine_blink,
            R.drawable.pet_kangaroo_sunshine_curious,
            R.drawable.pet_kangaroo_sunshine_inspect,
            R.drawable.pet_kangaroo_sunshine_walk_a,
            R.drawable.pet_kangaroo_sunshine_walk_b,
            R.drawable.pet_kangaroo_sunshine_happy,
            R.drawable.pet_kangaroo_sunshine_sad,
            R.drawable.pet_kangaroo_sunshine_yawn,
            R.drawable.pet_kangaroo_sunshine_sleep,
            R.drawable.pet_kangaroo_sunshine_petted,
            R.drawable.pet_kangaroo_sunshine_surprised,
        )
        PetOutfit.Raincoat -> intArrayOf(
            R.drawable.pet_kangaroo_raincoat_idle,
            R.drawable.pet_kangaroo_raincoat_blink,
            R.drawable.pet_kangaroo_raincoat_curious,
            R.drawable.pet_kangaroo_raincoat_inspect,
            R.drawable.pet_kangaroo_raincoat_walk_a,
            R.drawable.pet_kangaroo_raincoat_walk_b,
            R.drawable.pet_kangaroo_raincoat_happy,
            R.drawable.pet_kangaroo_raincoat_sad,
            R.drawable.pet_kangaroo_raincoat_yawn,
            R.drawable.pet_kangaroo_raincoat_sleep,
            R.drawable.pet_kangaroo_raincoat_petted,
            R.drawable.pet_kangaroo_raincoat_surprised,
        )
        PetOutfit.Windbreaker -> intArrayOf(
            R.drawable.pet_kangaroo_windbreaker_idle,
            R.drawable.pet_kangaroo_windbreaker_blink,
            R.drawable.pet_kangaroo_windbreaker_curious,
            R.drawable.pet_kangaroo_windbreaker_inspect,
            R.drawable.pet_kangaroo_windbreaker_walk_a,
            R.drawable.pet_kangaroo_windbreaker_walk_b,
            R.drawable.pet_kangaroo_windbreaker_happy,
            R.drawable.pet_kangaroo_windbreaker_sad,
            R.drawable.pet_kangaroo_windbreaker_yawn,
            R.drawable.pet_kangaroo_windbreaker_sleep,
            R.drawable.pet_kangaroo_windbreaker_petted,
            R.drawable.pet_kangaroo_windbreaker_surprised,
        )
        PetOutfit.Winter -> intArrayOf(
            R.drawable.pet_kangaroo_winter_idle,
            R.drawable.pet_kangaroo_winter_blink,
            R.drawable.pet_kangaroo_winter_curious,
            R.drawable.pet_kangaroo_winter_inspect,
            R.drawable.pet_kangaroo_winter_walk_a,
            R.drawable.pet_kangaroo_winter_walk_b,
            R.drawable.pet_kangaroo_winter_happy,
            R.drawable.pet_kangaroo_winter_sad,
            R.drawable.pet_kangaroo_winter_yawn,
            R.drawable.pet_kangaroo_winter_sleep,
            R.drawable.pet_kangaroo_winter_petted,
            R.drawable.pet_kangaroo_winter_surprised,
        )
    }
}
