package com.group5.roammate.ui.pet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import com.group5.roammate.R
import com.group5.roammate.pet.CompanionStyle
import com.group5.roammate.pet.PetAction
import com.group5.roammate.pet.PetBehaviorEngine
import com.group5.roammate.pet.PetUiState
import kotlinx.coroutines.delay

@DrawableRes
fun CompanionStyle.imageRes(): Int = when (this) {
    CompanionStyle.Corgi -> R.drawable.pet_pixel_corgi_everyday_idle_0
    CompanionStyle.Koala -> R.drawable.pet_pixel_koala_everyday_idle_0
    CompanionStyle.Penguin -> R.drawable.pet_pixel_penguin_everyday_idle_0
    CompanionStyle.Kangaroo -> R.drawable.pet_pixel_kangaroo_everyday_idle_0
}

/**
 * Pixel-art companion renderer.
 *
 * Every durable state owns a four-frame loop. In particular, Idle contains breathing, tail/ear
 * movement and an occasional two-frame blink; it does not have to switch to a fake "Blink" mood.
 * Nearest-neighbour sampling keeps the desktop-pet pixels crisp at every UI size.
 */
@Composable
fun PetAvatar(
    state: PetUiState,
    modifier: Modifier = Modifier,
    action: PetAction = PetBehaviorEngine.restingAction(state.mood),
    animateIdle: Boolean = true,
    interactionTick: Int = 0,
) {
    var framePhase by remember(action, state.companionStyle, state.outfit) {
        mutableIntStateOf(0)
    }
    val loop = action.spriteLoop()

    LaunchedEffect(
        action,
        interactionTick,
        state.companionStyle,
        state.outfit,
        animateIdle,
    ) {
        framePhase = 0
        if (!animateIdle) return@LaunchedEffect
        while (true) {
            delay(state.companionStyle.frameDelay(loop, framePhase))
            framePhase = (framePhase + 1) % 4
        }
    }

    val context = LocalContext.current
    val resourceName = state.companionStyle.frameResourceName(
        outfit = state.outfit,
        action = action,
        phase = framePhase,
    )
    val frameRes = remember(resourceName, context.packageName) {
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            .takeIf { it != 0 }
            ?: state.companionStyle.imageRes()
    }
    val bitmap = ImageBitmap.imageResource(id = frameRes)

    Image(
        bitmap = bitmap,
        contentDescription =
            "${state.companionStyle.displayName} companion, ${action.label.lowercase()}, wearing ${state.outfit.label}",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )
}

private fun CompanionStyle.frameDelay(loop: PetSpriteLoop, phase: Int): Long = when (loop) {
    PetSpriteLoop.Idle -> when (phase) {
        0 -> when (this) {
            CompanionStyle.Corgi -> 1_350L
            CompanionStyle.Koala -> 1_850L
            CompanionStyle.Penguin -> 1_450L
            CompanionStyle.Kangaroo -> 1_600L
        }

        1 -> when (this) {
            CompanionStyle.Corgi -> 800L
            CompanionStyle.Koala -> 1_050L
            CompanionStyle.Penguin -> 850L
            CompanionStyle.Kangaroo -> 950L
        }

        2 -> 110L
        else -> 145L
    }

    PetSpriteLoop.Walk -> when (this) {
        CompanionStyle.Corgi -> 145L
        CompanionStyle.Koala -> 270L
        CompanionStyle.Penguin -> 190L
        CompanionStyle.Kangaroo -> 175L
    }

    PetSpriteLoop.Sleep -> when (this) {
        CompanionStyle.Corgi -> 620L
        CompanionStyle.Koala -> 760L
        CompanionStyle.Penguin -> 680L
        CompanionStyle.Kangaroo -> 650L
    }

    PetSpriteLoop.Happy -> when (this) {
        CompanionStyle.Corgi -> 180L
        CompanionStyle.Koala -> 250L
        CompanionStyle.Penguin -> 170L
        CompanionStyle.Kangaroo -> 185L
    }

    PetSpriteLoop.Sad -> 620L
    PetSpriteLoop.Petted -> when (this) {
        CompanionStyle.Corgi -> 220L
        CompanionStyle.Koala -> 310L
        CompanionStyle.Penguin -> 210L
        CompanionStyle.Kangaroo -> 235L
    }
}
