package com.group5.roammate.ui.pet

import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.group5.roammate.R
import com.group5.roammate.pet.CompanionStyle
import com.group5.roammate.pet.PetAction
import com.group5.roammate.pet.PetMood
import com.group5.roammate.pet.PetUiState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@DrawableRes
fun CompanionStyle.imageRes(): Int = when (this) {
    CompanionStyle.Corgi -> R.drawable.pet_corgi_idle
    CompanionStyle.Koala -> R.drawable.pet_koala_idle
    CompanionStyle.Penguin -> R.drawable.pet_penguin_idle
    CompanionStyle.Kangaroo -> R.drawable.pet_kangaroo_idle
}

@Composable
fun PetAvatar(
    state: PetUiState,
    modifier: Modifier = Modifier,
    action: PetAction = PetAction.Idle,
    animateIdle: Boolean = true,
    interactionTick: Int = 0,
) {
    var framePhase by remember(action) { mutableIntStateOf(0) }
    LaunchedEffect(action, interactionTick) {
        framePhase = 0
        if (action == PetAction.Walk) {
            while (true) {
                delay(
                    when (state.companionStyle) {
                        CompanionStyle.Koala -> 420L
                        CompanionStyle.Penguin -> 210L
                        CompanionStyle.Kangaroo -> 280L
                        CompanionStyle.Corgi -> 180L
                    },
                )
                framePhase = 1 - framePhase
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "pet-motion")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pet-motion-cycle",
    )
    val wave = sin(cycle.toDouble()).toFloat()
    val bounce = abs(wave)
    val motion = motionFor(state.companionStyle, action, wave, bounce, animateIdle)

    Box(
        modifier = modifier.graphicsLayer {
            translationX = motion.translationX
            translationY = motion.translationY
            rotationZ = motion.rotation
            scaleX = motion.scaleX
            scaleY = motion.scaleY
        },
    ) {
        Image(
            painter = painterResource(state.companionStyle.frameRes(state.outfit, action, framePhase)),
            contentDescription =
                "${state.companionStyle.displayName} companion, ${action.label.lowercase()}, wearing ${state.outfit.label}",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit,
        )
        EmotionEffectLayer(
            action = action,
            mood = state.mood,
            modifier = Modifier.matchParentSize(),
        )
    }
}

private data class PetMotion(
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
)

private fun motionFor(
    style: CompanionStyle,
    action: PetAction,
    wave: Float,
    bounce: Float,
    animateIdle: Boolean,
): PetMotion {
    if (!animateIdle) return PetMotion()
    return when (style) {
        CompanionStyle.Corgi -> when (action) {
            PetAction.Walk -> PetMotion(wave * 22f, -bounce * 9f, wave * 2.8f)
            PetAction.Happy -> PetMotion(translationY = -bounce * 28f, rotation = wave * 5f)
            PetAction.Curious -> PetMotion(rotation = wave * 2f, scaleX = 1.02f, scaleY = 1.02f)
            PetAction.Petted -> PetMotion(translationY = bounce * 3f, rotation = wave * 1.2f, scaleY = 0.98f)
            PetAction.Surprised -> PetMotion(translationY = -bounce * 12f, scaleX = 1.04f, scaleY = 1.04f)
            PetAction.Sleep -> PetMotion(translationY = bounce * 2f, scaleX = 1f + bounce * 0.012f)
            PetAction.Dragged -> PetMotion(rotation = wave * 7f)
            else -> PetMotion(translationY = -bounce * 4f, scaleX = 1f + bounce * 0.008f, scaleY = 1f + bounce * 0.008f)
        }
        CompanionStyle.Koala -> when (action) {
            PetAction.Walk -> PetMotion(wave * 11f, -bounce * 3f, wave * 2f)
            PetAction.Happy -> PetMotion(translationY = -bounce * 17f, rotation = wave * 3f)
            PetAction.Yawn -> PetMotion(rotation = wave * 0.8f, scaleY = 1f + bounce * 0.018f)
            PetAction.Sleep -> PetMotion(translationY = bounce * 1.5f, scaleX = 1f + bounce * 0.01f)
            PetAction.Petted -> PetMotion(rotation = -2f + wave * 0.7f, scaleY = 0.99f)
            PetAction.Dragged -> PetMotion(rotation = wave * 4f)
            else -> PetMotion(translationY = -bounce * 2f, rotation = wave * 0.7f, scaleY = 1f + bounce * 0.008f)
        }
        CompanionStyle.Penguin -> when (action) {
            PetAction.Walk -> PetMotion(wave * 18f, -bounce * 4f, wave * 8f)
            PetAction.Happy -> PetMotion(translationY = -bounce * 24f, rotation = wave * 7f)
            PetAction.Curious -> PetMotion(rotation = wave * 4f)
            PetAction.Petted -> PetMotion(translationY = bounce * 3f, rotation = wave * 1.5f, scaleY = 0.98f)
            PetAction.Surprised -> PetMotion(translationY = -bounce * 10f, rotation = wave * 4f)
            PetAction.Dragged -> PetMotion(rotation = wave * 10f)
            else -> PetMotion(translationY = -bounce * 3f, rotation = wave * 1.4f, scaleY = 1f + bounce * 0.01f)
        }
        CompanionStyle.Kangaroo -> when (action) {
            PetAction.Walk -> PetMotion(wave * 24f, -bounce * 34f, wave * 4f)
            PetAction.Happy -> PetMotion(translationY = -bounce * 42f, rotation = wave * 4f)
            PetAction.Curious -> PetMotion(translationY = -bounce * 5f, scaleY = 1f + bounce * 0.018f)
            PetAction.Petted -> PetMotion(rotation = wave, scaleY = 0.98f)
            PetAction.Surprised -> PetMotion(translationY = -bounce * 18f, scaleY = 1.04f)
            PetAction.Dragged -> PetMotion(rotation = wave * 6f)
            else -> PetMotion(translationY = -bounce * 5f, scaleY = 1f + bounce * 0.012f)
        }
    }
}

@Composable
private fun EmotionEffectLayer(
    action: PetAction,
    mood: PetMood,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (action == PetAction.Happy || action == PetAction.Petted) {
            drawCircle(Color(0xFFFF6F61).copy(alpha = 0.88f), w * 0.025f, Offset(w * 0.78f, h * 0.23f))
            drawCircle(Color(0xFFFF9B91).copy(alpha = 0.88f), w * 0.018f, Offset(w * 0.84f, h * 0.17f))
        }
        if (action == PetAction.Sad) {
            drawOval(
                color = Color(0xFF65BCE8).copy(alpha = 0.85f),
                topLeft = Offset(w * 0.65f, h * 0.42f),
                size = Size(w * 0.025f, h * 0.055f),
            )
        }
        if (action == PetAction.Sleep || action == PetAction.Yawn || mood == PetMood.Tired && action == PetAction.Idle) {
            drawContext.canvas.nativeCanvas.drawText(
                "Z z",
                w * 0.73f,
                h * 0.22f,
                Paint().apply {
                    color = android.graphics.Color.rgb(50, 102, 130)
                    textSize = w * 0.085f
                    isFakeBoldText = true
                    isAntiAlias = true
                },
            )
        }
        if (action == PetAction.Surprised) {
            repeat(3) { index ->
                drawLine(
                    color = Color(0xFFFFB300),
                    start = Offset(w * (0.76f + index * 0.045f), h * (0.19f - index * 0.01f)),
                    end = Offset(w * (0.79f + index * 0.055f), h * (0.13f - index * 0.015f)),
                    strokeWidth = w * 0.014f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
