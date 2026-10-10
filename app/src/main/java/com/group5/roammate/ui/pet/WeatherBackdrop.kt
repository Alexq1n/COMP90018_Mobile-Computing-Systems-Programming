package com.group5.roammate.ui.pet

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.group5.roammate.pet.PetWeatherCondition
import com.group5.roammate.pet.PetWeatherSnapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A lightweight animated weather scene intended to sit directly behind [PetAvatar].
 *
 * The animation is drawn entirely with Compose primitives, so there are no video assets to load
 * and the backdrop scales cleanly from compact phones to tablets. Motion is deliberately gentle:
 * clouds and fog drift, the sun breathes, and precipitation loops without changing the pet's state.
 */
@Composable
fun AnimatedWeatherBackdrop(
    weather: PetWeatherSnapshot,
    modifier: Modifier = Modifier,
) {
    val sceneCondition = if (weather.isCurrent) weather.condition else PetWeatherCondition.Unknown
    val wind = weather.windSpeedKmh.toFloat().takeIf { it.isFinite() }?.coerceIn(0f, 90f) ?: 0f
    val temperature = weather.temperatureC.toFloat().takeIf { it.isFinite() } ?: 18f
    val transition = rememberInfiniteTransition(label = "petWeatherBackdrop")

    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (18_000f - wind * 110f).toInt().coerceIn(8_000, 18_000),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cloudDrift",
    )
    val precipitation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "precipitation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sunPulse",
    )
    val atmosphere by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "atmosphere",
    )

    Box(modifier = modifier.clip(RoundedCornerShape(28.dp))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawWeatherSky(
                condition = sceneCondition,
                temperature = temperature,
            )

            when (sceneCondition) {
                PetWeatherCondition.Clear -> if (weather.isDay) {
                    drawClearScene(drift = drift, pulse = pulse, temperature = temperature)
                } else {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF253B59), Color(0xFF7899A7))))
                    val moon = Offset(size.width * .78f, size.height * .24f)
                    drawCircle(Color(0xFFFFF2CC), size.minDimension * .08f, moon)
                    drawCircle(Color(0xFF354F6B), size.minDimension * .073f,
                        moon + Offset(size.minDimension * .033f, -size.minDimension * .025f))
                    repeat(12) { i ->
                        drawCircle(Color.White.copy(alpha = .4f + (pulse - .92f) * 3f),
                            2.dp.toPx(), Offset(size.width * ((i * .137f + .06f) % 1f),
                                size.height * ((i * .087f + .03f) % .62f)))
                    }
                }

                PetWeatherCondition.Cloudy -> drawCloudyScene(drift = drift)
                PetWeatherCondition.Fog -> drawFogScene(drift = drift)
                PetWeatherCondition.Rain -> {
                    drawCloudyScene(drift = drift, dark = true)
                    drawRain(phase = precipitation, wind = wind, storm = false)
                }

                PetWeatherCondition.Storm -> {
                    drawCloudyScene(drift = drift, dark = true)
                    drawStormFlash(phase = atmosphere)
                    drawRain(phase = precipitation, wind = wind + 22f, storm = true)
                }

                PetWeatherCondition.Snow -> {
                    drawCloudyScene(drift = drift, snow = true)
                    drawSnow(phase = precipitation, drift = drift)
                }

                PetWeatherCondition.Unknown -> drawUnknownScene(
                    drift = drift,
                    pulse = pulse,
                )
            }

            drawSoftGround(condition = sceneCondition)
        }
    }
}

private fun DrawScope.drawWeatherSky(
    condition: PetWeatherCondition,
    temperature: Float,
) {
    val colors = when (condition) {
        PetWeatherCondition.Clear -> if (temperature >= 28f) {
            listOf(Color(0xFF69C8ED), Color(0xFFFFE8B2), Color(0xFFFFF5D8))
        } else {
            listOf(Color(0xFF71CDEB), Color(0xFFC9F0F4), Color(0xFFF1FBF8))
        }

        PetWeatherCondition.Cloudy ->
            listOf(Color(0xFF8AAFC2), Color(0xFFC9DDE3), Color(0xFFF0F5F3))

        PetWeatherCondition.Fog ->
            listOf(Color(0xFFAEBFC6), Color(0xFFD8E1E1), Color(0xFFF5F7F2))

        PetWeatherCondition.Rain ->
            listOf(Color(0xFF527895), Color(0xFF8FB1C0), Color(0xFFD6E4E5))

        PetWeatherCondition.Storm ->
            listOf(Color(0xFF243651), Color(0xFF4B6175), Color(0xFF9BAEB6))

        PetWeatherCondition.Snow ->
            listOf(Color(0xFF9ED0E6), Color(0xFFDCEEF4), Color(0xFFF9FCFA))

        PetWeatherCondition.Unknown ->
            listOf(Color(0xFF68B6BE), Color(0xFFB9E5DD), Color(0xFFF0F8F2))
    }

    drawRect(brush = Brush.verticalGradient(colors = colors))
}

private fun DrawScope.drawClearScene(
    drift: Float,
    pulse: Float,
    temperature: Float,
) {
    val sun = Offset(size.width * 0.78f, size.height * 0.21f)
    val radius = size.minDimension * 0.095f * pulse
    val rayColor = if (temperature >= 28f) Color(0xFFFFB23D) else Color(0xFFFFC94F)

    drawCircle(
        color = rayColor.copy(alpha = 0.18f),
        radius = radius * 1.65f,
        center = sun,
    )
    repeat(10) { index ->
        val angle = index * (2f * PI.toFloat() / 10f)
        val startRadius = radius * 1.28f
        val endRadius = radius * (1.62f + 0.06f * pulse)
        drawLine(
            color = rayColor.copy(alpha = 0.62f),
            start = Offset(
                x = sun.x + cos(angle) * startRadius,
                y = sun.y + sin(angle) * startRadius,
            ),
            end = Offset(
                x = sun.x + cos(angle) * endRadius,
                y = sun.y + sin(angle) * endRadius,
            ),
            strokeWidth = size.minDimension * 0.012f,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(color = Color(0xFFFFD45E), radius = radius, center = sun)

    drawCloud(
        center = Offset(
            x = wrappedX(size.width * (0.06f + drift * 1.02f), size.width, size.width * 0.22f),
            y = size.height * 0.24f,
        ),
        scale = size.minDimension * 0.0041f,
        color = Color.White.copy(alpha = 0.66f),
    )
    drawCloud(
        center = Offset(
            x = wrappedX(size.width * (0.58f + drift * 0.72f), size.width, size.width * 0.18f),
            y = size.height * 0.37f,
        ),
        scale = size.minDimension * 0.0032f,
        color = Color.White.copy(alpha = 0.46f),
    )
}

private fun DrawScope.drawCloudyScene(
    drift: Float,
    dark: Boolean = false,
    snow: Boolean = false,
) {
    val cloudColor = when {
        dark -> Color(0xFFD3DEE2).copy(alpha = 0.76f)
        snow -> Color(0xFFF8FCFC).copy(alpha = 0.90f)
        else -> Color(0xFFF2F7F7).copy(alpha = 0.82f)
    }
    val shadow = when {
        dark -> Color(0xFF496477).copy(alpha = 0.40f)
        snow -> Color(0xFF86B5C7).copy(alpha = 0.23f)
        else -> Color(0xFF6D8F9C).copy(alpha = 0.25f)
    }

    drawCloud(
        center = Offset(
            x = wrappedX(size.width * (-0.10f + drift * 1.18f), size.width, size.width * 0.32f),
            y = size.height * 0.18f,
        ),
        scale = size.minDimension * 0.0061f,
        color = shadow,
    )
    drawCloud(
        center = Offset(
            x = wrappedX(size.width * (0.36f + drift * 0.92f), size.width, size.width * 0.28f),
            y = size.height * 0.27f,
        ),
        scale = size.minDimension * 0.0054f,
        color = cloudColor,
    )
    drawCloud(
        center = Offset(
            x = wrappedX(size.width * (0.78f + drift * 0.72f), size.width, size.width * 0.24f),
            y = size.height * 0.13f,
        ),
        scale = size.minDimension * 0.0045f,
        color = cloudColor.copy(alpha = cloudColor.alpha * 0.88f),
    )
}

private fun DrawScope.drawFogScene(drift: Float) {
    drawCloudyScene(drift = drift, dark = false)
    repeat(5) { index ->
        val bandWidth = size.width * (0.52f + index * 0.05f)
        val travel = size.width + bandWidth
        val offset = if (index % 2 == 0) drift else 1f - drift
        val x = (offset * travel + index * size.width * 0.19f) % travel - bandWidth
        val y = size.height * (0.37f + index * 0.105f)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f + index * 0.035f),
            topLeft = Offset(x = x, y = y),
            size = Size(width = bandWidth, height = size.height * 0.047f),
            cornerRadius = CornerRadius(size.height * 0.03f),
        )
    }
}

private fun DrawScope.drawRain(
    phase: Float,
    wind: Float,
    storm: Boolean,
) {
    val slant = (wind.coerceIn(0f, 100f) / 100f) * size.width * 0.035f
    val dropColor = if (storm) Color(0xFFA9D8F4) else Color(0xFF3F92C1)
    val count = if (storm) 44 else 34
    repeat(count) { index ->
        val seedX = ((index * 37) % 101) / 101f
        val seedY = ((index * 61) % 103) / 103f
        val y = ((seedY + phase * (1.25f + index % 3 * 0.14f)) % 1f) * size.height
        val x = (seedX * size.width + phase * slant * 5f) % (size.width + slant) - slant
        val length = size.height * (0.035f + (index % 4) * 0.006f)
        drawLine(
            color = dropColor.copy(alpha = 0.42f + (index % 3) * 0.13f),
            start = Offset(x = x, y = y),
            end = Offset(x = x - slant, y = y + length),
            strokeWidth = size.minDimension * if (storm) 0.006f else 0.0045f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawStormFlash(phase: Float) {
    val flash = when {
        phase in 0.70f..0.735f -> (phase - 0.70f) / 0.035f
        phase in 0.735f..0.78f -> 1f - (phase - 0.735f) / 0.045f
        phase in 0.82f..0.84f -> 0.58f - (phase - 0.82f) / 0.02f * 0.58f
        else -> 0f
    }.coerceIn(0f, 1f)

    if (flash <= 0f) return
    drawRect(color = Color.White.copy(alpha = flash * 0.16f))

    val bolt = Path().apply {
        moveTo(size.width * 0.77f, size.height * 0.19f)
        lineTo(size.width * 0.66f, size.height * 0.42f)
        lineTo(size.width * 0.73f, size.height * 0.41f)
        lineTo(size.width * 0.62f, size.height * 0.67f)
        lineTo(size.width * 0.82f, size.height * 0.36f)
        lineTo(size.width * 0.74f, size.height * 0.37f)
        close()
    }
    drawPath(color = Color(0xFFFFE675).copy(alpha = flash * 0.92f), path = bolt)
    drawPath(
        color = Color.White.copy(alpha = flash * 0.72f),
        path = bolt,
        style = Stroke(width = size.minDimension * 0.007f),
    )
}

private fun DrawScope.drawSnow(
    phase: Float,
    drift: Float,
) {
    repeat(34) { index ->
        val seedX = ((index * 47) % 101) / 101f
        val seedY = ((index * 67) % 103) / 103f
        val y = ((seedY + phase * (0.32f + (index % 4) * 0.045f)) % 1f) * size.height
        val sway = sin((phase * 2f + seedY + drift) * PI.toFloat() * 2f) * size.width * 0.025f
        val x = (seedX * size.width + sway + size.width) % size.width
        val radius = size.minDimension * (0.005f + (index % 4) * 0.0023f)
        drawCircle(
            color = Color.White.copy(alpha = 0.58f + (index % 3) * 0.13f),
            radius = radius,
            center = Offset(x = x, y = y),
        )
    }
}

private fun DrawScope.drawUnknownScene(
    drift: Float,
    pulse: Float,
) {
    repeat(14) { index ->
        val angle = index * 0.83f + drift * PI.toFloat() * 2f
        val orbit = size.minDimension * (0.15f + (index % 5) * 0.035f)
        val center = Offset(
            x = size.width * 0.76f + cos(angle) * orbit,
            y = size.height * 0.28f + sin(angle) * orbit * 0.55f,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.19f + (index % 3) * 0.08f),
            radius = size.minDimension * 0.008f * pulse,
            center = center,
        )
    }
    drawCloud(
        center = Offset(
            x = wrappedX(size.width * (0.12f + drift * 0.82f), size.width, size.width * 0.22f),
            y = size.height * 0.25f,
        ),
        scale = size.minDimension * 0.0044f,
        color = Color.White.copy(alpha = 0.48f),
    )
}

private fun DrawScope.drawSoftGround(condition: PetWeatherCondition) {
    val ground = when (condition) {
        PetWeatherCondition.Storm -> Color(0xFF284D55)
        PetWeatherCondition.Rain -> Color(0xFF5D8E91)
        PetWeatherCondition.Snow -> Color(0xFFF4FBFA)
        PetWeatherCondition.Fog -> Color(0xFFB8D2CC)
        else -> Color(0xFF82C8B1)
    }
    drawOval(
        color = ground.copy(alpha = 0.42f),
        topLeft = Offset(x = -size.width * 0.12f, y = size.height * 0.78f),
        size = Size(width = size.width * 1.24f, height = size.height * 0.42f),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.13f),
        topLeft = Offset(x = size.width * 0.15f, y = size.height * 0.84f),
        size = Size(width = size.width * 0.70f, height = size.height * 0.10f),
    )
}

private fun DrawScope.drawCloud(
    center: Offset,
    scale: Float,
    color: Color,
) {
    val unit = 10f * scale
    drawCircle(color = color, radius = unit * 4.4f, center = center + Offset(-unit * 3.1f, 0f))
    drawCircle(color = color, radius = unit * 5.8f, center = center + Offset(unit * 0.8f, -unit * 2.2f))
    drawCircle(color = color, radius = unit * 4.7f, center = center + Offset(unit * 5.3f, -unit * 0.2f))
    drawRoundRect(
        color = color,
        topLeft = center + Offset(-unit * 7.1f, -unit * 0.7f),
        size = Size(width = unit * 16f, height = unit * 5.7f),
        cornerRadius = CornerRadius(unit * 2.85f),
    )
}

private fun wrappedX(rawX: Float, width: Float, margin: Float): Float {
    val span = width + margin * 2f
    return ((rawX + margin) % span) - margin
}
