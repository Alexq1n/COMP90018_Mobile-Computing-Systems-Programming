package com.group5.roammate.ui.pet

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.pet.PetAction
import com.group5.roammate.pet.PetBehaviorCue
import com.group5.roammate.pet.PetBehaviorEngine
import com.group5.roammate.pet.PetInteraction
import com.group5.roammate.pet.PetUiState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private val StageTeal = Color(0xFF008B8F)
private val StageText = Color(0xFF17212B)
private val StageMuted = Color(0xFF68777F)

/**
 * The Pet tab's always-live companion. Autonomous behavior and direct touch coexist in one state
 * loop, so interaction interrupts the current action and naturally returns to independent life.
 */
@Composable
fun InteractivePetStage(
    state: PetUiState,
    treatTick: Int,
    modifier: Modifier = Modifier,
) {
    var action by remember(state.companionStyle) { mutableStateOf(PetAction.Idle) }
    var message by remember(state.companionStyle) { mutableStateOf(state.statusLine) }
    var queuedReaction by remember { mutableStateOf<PetBehaviorCue?>(null) }
    var behaviorVersion by remember { mutableIntStateOf(0) }
    var petOffset by remember(state.companionStyle) { mutableStateOf(Offset.Zero) }
    var lastTapAt by remember { mutableLongStateOf(0L) }

    fun react(interaction: PetInteraction) {
        queuedReaction = PetBehaviorEngine.reaction(
            style = state.companionStyle,
            interaction = interaction,
            locationLabel = state.environment.locationLabel,
        )
        behaviorVersion += 1
    }

    ShakeDetectorEffect { react(PetInteraction.Shake) }

    LaunchedEffect(treatTick) {
        if (treatTick > 0) react(PetInteraction.Treat)
    }

    LaunchedEffect(
        state.companionStyle,
        state.mood,
        state.environment.locationLabel,
        behaviorVersion,
    ) {
        queuedReaction?.let { cue ->
            action = cue.action
            message = cue.message
            delay(cue.durationMillis)
            queuedReaction = null
            action = PetAction.Idle
            message = state.statusLine
            delay(900)
        }

        while (true) {
            delay(Random.nextLong(2_400L, 5_200L))
            val next = PetBehaviorEngine.autonomousActions(
                style = state.companionStyle,
                mood = state.mood,
            ).random()
            action = next
            message = PetBehaviorEngine.autonomousMessage(
                style = state.companionStyle,
                action = next,
                locationLabel = state.environment.locationLabel,
            )
            delay(PetBehaviorEngine.durationFor(next))
            action = PetAction.Idle
            message = state.statusLine
        }
    }

    val density = LocalDensity.current
    val maxHorizontal = with(density) { 58.dp.toPx() }
    val maxVertical = with(density) { 38.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFDDF5F2), Color(0xFFF8FCFB), Color(0xFFFFF3E7)),
                ),
                RoundedCornerShape(28.dp),
            ),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, StageTeal.copy(alpha = 0.13f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = action.label,
                    color = StageTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = message,
                    color = StageText,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 58.dp)
                .size(286.dp)
                .offset { IntOffset(petOffset.x.roundToInt(), petOffset.y.roundToInt()) }
                .pointerInput(state.companionStyle) {
                    val dragThreshold = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startedAt = SystemClock.uptimeMillis()
                        val start = down.position
                        var dragged = false
                        var pressed = true

                        while (pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            pressed = change.pressed
                            val delta = change.positionChange()
                            val distance = (change.position - start).getDistance()
                            if (!dragged && distance > dragThreshold) {
                                dragged = true
                                action = PetAction.Dragged
                                message = "Buddy is following your hand."
                            }
                            if (dragged && delta != Offset.Zero) {
                                petOffset = Offset(
                                    x = (petOffset.x + delta.x).coerceIn(-maxHorizontal, maxHorizontal),
                                    y = (petOffset.y + delta.y).coerceIn(-maxVertical, maxVertical),
                                )
                                change.consume()
                            }
                        }

                        if (dragged) {
                            react(PetInteraction.Drag)
                        } else {
                            val now = SystemClock.uptimeMillis()
                            val heldFor = now - startedAt
                            val interaction = when {
                                heldFor >= 520L -> PetInteraction.LongPress
                                now - lastTapAt <= 330L -> PetInteraction.DoubleTap
                                start.y < size.height * 0.48f -> PetInteraction.TapHead
                                else -> PetInteraction.TapBody
                            }
                            lastTapAt = now
                            react(interaction)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            PetAvatar(
                state = state,
                action = action,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Text(
            text = "Tap head or body · hold to pet · drag Buddy · shake phone",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 13.dp),
            color = StageMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ShakeDetectorEffect(onShake: () -> Unit) {
    val context = LocalContext.current
    val latestOnShake by rememberUpdatedState(onShake)

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShake = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0] / SensorManager.GRAVITY_EARTH
                val y = event.values[1] / SensorManager.GRAVITY_EARTH
                val z = event.values[2] / SensorManager.GRAVITY_EARTH
                val gForce = sqrt(x * x + y * y + z * z)
                val now = SystemClock.elapsedRealtime()
                if (gForce > 2.35f && now - lastShake > 1_000L) {
                    lastShake = now
                    latestOnShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (accelerometer != null) {
            manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { manager.unregisterListener(listener) }
    }
}
