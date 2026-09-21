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
fun CompanionStyle.imageRes(): Int = R.drawable.pet_pixel_koala_everyday_idle_0

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
            delay(frameDelay(loop, framePhase))
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

private fun frameDelay(loop: PetSpriteLoop, phase: Int): Long = when (loop) {
    PetSpriteLoop.Idle -> when (phase) {
        0 -> 1_850L
        1 -> 1_050L
        2 -> 110L
        else -> 145L
    }

    PetSpriteLoop.Walk -> 270L
    PetSpriteLoop.Sleep -> 760L
    PetSpriteLoop.Happy -> 250L
    PetSpriteLoop.Sad -> 620L
    PetSpriteLoop.Petted -> 310L
}
