package com.group5.roammate.ui.pet

import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.group5.roammate.R
import com.group5.roammate.pet.CompanionStyle
import com.group5.roammate.pet.PetMood
import com.group5.roammate.pet.PetOutfit
import com.group5.roammate.pet.PetUiState

@DrawableRes
fun CompanionStyle.imageRes(): Int = when (this) {
    CompanionStyle.Corgi -> R.drawable.pet_corgi
    CompanionStyle.Koala -> R.drawable.pet_koala
    CompanionStyle.Penguin -> R.drawable.pet_penguin
    CompanionStyle.Kangaroo -> R.drawable.pet_kangaroo
}

/**
 * A reusable avatar used by Home, Companions, Play and Camera. Weather equipment is drawn as a
 * separate transparent layer so it remains consistent for every companion image.
 */
@Composable
fun PetAvatar(
    state: PetUiState,
    modifier: Modifier = Modifier,
    animateIdle: Boolean = true,
    interactionTick: Int = 0,
) {
    val transition = rememberInfiniteTransition(label = "pet-idle")
    val idleOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animateIdle) -8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_250),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pet-idle-offset",
    )
    val idleScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animateIdle) 1.025f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_250),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pet-breathing",
    )
    val interaction = remember { Animatable(0f) }

    LaunchedEffect(interactionTick) {
        if (interactionTick > 0) {
            interaction.snapTo(0f)
            interaction.animateTo(1f, animationSpec = tween(180))
            interaction.animateTo(-0.7f, animationSpec = tween(150))
            interaction.animateTo(0f, animationSpec = tween(170))
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            translationY = idleOffset - interaction.value * 15f
            rotationZ = interaction.value * 7f
            scaleX = idleScale + interaction.value * 0.03f
            scaleY = idleScale + interaction.value * 0.03f
        },
    ) {
        Image(
            painter = painterResource(state.companionStyle.imageRes()),
            contentDescription = "${state.companionStyle.displayName} companion wearing ${state.outfit.label}",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit,
        )

        WeatherAccessoryLayer(
            outfit = state.outfit,
            mood = state.mood,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun WeatherAccessoryLayer(
    outfit: PetOutfit,
    mood: PetMood,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (outfit) {
            PetOutfit.Everyday -> Unit

            PetOutfit.Sunshine -> {
                val lensRadius = w * 0.072f
                val y = h * 0.32f
                val left = Offset(w * 0.43f, y)
                val right = Offset(w * 0.57f, y)
                drawCircle(Color(0xFF243849), lensRadius, left)
                drawCircle(Color(0xFF243849), lensRadius, right)
                drawLine(
                    color = Color(0xFF243849),
                    start = Offset(left.x + lensRadius, y),
                    end = Offset(right.x - lensRadius, y),
                    strokeWidth = w * 0.025f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color(0xFF243849),
                    start = Offset(left.x - lensRadius, y),
                    end = Offset(w * 0.31f, h * 0.29f),
                    strokeWidth = w * 0.018f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color(0xFF243849),
                    start = Offset(right.x + lensRadius, y),
                    end = Offset(w * 0.69f, h * 0.29f),
                    strokeWidth = w * 0.018f,
                    cap = StrokeCap.Round,
                )
                drawCircle(Color.White.copy(alpha = 0.75f), lensRadius * 0.22f, left - Offset(lensRadius * 0.3f, lensRadius * 0.3f))
                drawCircle(Color.White.copy(alpha = 0.75f), lensRadius * 0.22f, right - Offset(lensRadius * 0.3f, lensRadius * 0.3f))
            }

            PetOutfit.Raincoat -> {
                val coat = Path().apply {
                    moveTo(w * 0.32f, h * 0.52f)
                    lineTo(w * 0.68f, h * 0.52f)
                    lineTo(w * 0.74f, h * 0.85f)
                    quadraticBezierTo(w * 0.50f, h * 0.93f, w * 0.26f, h * 0.85f)
                    close()
                }
                drawPath(coat, Color(0xFFFFD34E).copy(alpha = 0.88f))
                drawPath(coat, Color(0xFFD19B11), style = Stroke(width = w * 0.012f))
                drawLine(
                    color = Color(0xFF73522A),
                    start = Offset(w * 0.77f, h * 0.23f),
                    end = Offset(w * 0.77f, h * 0.70f),
                    strokeWidth = w * 0.016f,
                    cap = StrokeCap.Round,
                )
                drawArc(
                    color = Color(0xFF5D72D9),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.55f, h * 0.10f),
                    size = Size(w * 0.44f, h * 0.27f),
                )
                drawArc(
                    color = Color(0xFF2F4299),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.55f, h * 0.10f),
                    size = Size(w * 0.44f, h * 0.27f),
                    style = Stroke(width = w * 0.012f),
                )
            }

            PetOutfit.Windbreaker -> {
                val jacket = Path().apply {
                    moveTo(w * 0.30f, h * 0.55f)
                    quadraticBezierTo(w * 0.50f, h * 0.45f, w * 0.70f, h * 0.55f)
                    lineTo(w * 0.73f, h * 0.82f)
                    quadraticBezierTo(w * 0.50f, h * 0.89f, w * 0.27f, h * 0.82f)
                    close()
                }
                drawPath(jacket, Color(0xFF38A7A5).copy(alpha = 0.87f))
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(w * 0.50f, h * 0.50f),
                    end = Offset(w * 0.50f, h * 0.86f),
                    strokeWidth = w * 0.014f,
                )
                drawPath(jacket, Color(0xFF13777A), style = Stroke(width = w * 0.012f))
            }

            PetOutfit.Winter -> {
                drawArc(
                    color = Color(0xFFEF7A68),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.30f, h * 0.07f),
                    size = Size(w * 0.40f, h * 0.30f),
                )
                drawRoundRect(
                    color = Color(0xFFDA5D50),
                    topLeft = Offset(w * 0.28f, h * 0.21f),
                    size = Size(w * 0.44f, h * 0.085f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f),
                )
                drawCircle(Color(0xFFFFD6CC), w * 0.045f, Offset(w * 0.50f, h * 0.07f))
                drawArc(
                    color = Color(0xFFEF7A68),
                    startAngle = 5f,
                    sweepAngle = 170f,
                    useCenter = false,
                    topLeft = Offset(w * 0.26f, h * 0.39f),
                    size = Size(w * 0.48f, h * 0.28f),
                    style = Stroke(width = w * 0.065f, cap = StrokeCap.Round),
                )
            }
        }

        if (mood == PetMood.Tired) {
            drawContext.canvas.nativeCanvas.drawText(
                "Z z",
                w * 0.72f,
                h * 0.25f,
                Paint().apply {
                    color = android.graphics.Color.rgb(50, 102, 130)
                    textSize = w * 0.09f
                    isFakeBoldText = true
                    isAntiAlias = true
                },
            )
        }
    }
}
