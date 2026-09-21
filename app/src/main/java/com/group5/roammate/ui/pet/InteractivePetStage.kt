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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.group5.roammate.pet.PetTripContext
import com.group5.roammate.pet.PetUiState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val StageTeal = Color(0xFF008B8F)
private val StageText = Color(0xFF17212B)

/**
 * The Pet tab's always-live companion. Mood selects one durable loop; direct input briefly
 * interrupts it and then returns Buddy to that same loop. Micro-animation (breathing, blinking,
 * tail and ear motion) lives inside every loop instead of randomly changing Buddy's emotion.
 */
@Composable
fun InteractivePetStage(
    state: PetUiState,
    treatTick: Int,
    onPreviousOutfit: () -> Unit,
    onNextOutfit: () -> Unit,
    tripContext: PetTripContext = PetTripContext(),
    modifier: Modifier = Modifier,
) {
    var action by remember(state.companionStyle) {
        mutableStateOf(PetBehaviorEngine.restingAction(state.mood))
    }
    var message by remember(state.companionStyle) { mutableStateOf(state.statusLine) }
    var queuedReaction by remember { mutableStateOf<PetBehaviorCue?>(null) }
    var behaviorVersion by remember { mutableIntStateOf(0) }
    var petOffset by remember(state.companionStyle) { mutableStateOf(Offset.Zero) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var shakeCount by remember { mutableIntStateOf(0) }
    val latestPreviousOutfit by rememberUpdatedState(onPreviousOutfit)
    val latestNextOutfit by rememberUpdatedState(onNextOutfit)

    fun showCue(cue: PetBehaviorCue) {
        queuedReaction = cue
        behaviorVersion += 1
    }

    fun react(interaction: PetInteraction) = showCue(PetBehaviorEngine.reaction(interaction))

    ShakeDetectorEffect {
        shakeCount += 1
        showCue(PetBehaviorEngine.weatherAdvice(state.weather, tripContext, shakeCount))
    }

    LaunchedEffect(treatTick) {
        if (treatTick > 0) react(PetInteraction.Treat)
    }

    LaunchedEffect(
        state.companionStyle,
        state.outfit,
        state.mood,
        state.statusLine,
        behaviorVersion,
    ) {
        val cue = queuedReaction
        if (cue != null) {
            action = cue.action
            message = cue.message
            delay(cue.durationMillis)
            queuedReaction = null
        }
        action = PetBehaviorEngine.restingAction(state.mood)
        message = state.statusLine
    }

    val density = LocalDensity.current
    val maxVertical = with(density) { 38.dp.toPx() }
    val outfitSwipeThreshold = with(density) { 48.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color(0xFFEAF7F5),
                RoundedCornerShape(28.dp),
            ),
    ) {
        AnimatedWeatherBackdrop(
            weather = state.weather,
            modifier = Modifier.fillMaxSize(),
        )

        val avatarSize = minOf(
            286.dp,
            (maxWidth - 32.dp).coerceAtLeast(0.dp),
            (maxHeight - 118.dp).coerceAtLeast(0.dp),
        )

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
                .padding(top = 48.dp)
                .fillMaxWidth()
                .height(avatarSize)
                .pointerInput(state.companionStyle, state.outfit) {
                    val dragThreshold = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startedAt = SystemClock.uptimeMillis()
                        val start = down.position
                        var gestureMode = GestureMode.Undecided
                        var totalDelta = Offset.Zero
                        var pressed = true

                        while (pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            pressed = change.pressed
                            val delta = change.positionChange()
                            totalDelta += delta
                            if (gestureMode == GestureMode.Undecided &&
                                totalDelta.getDistance() > dragThreshold
                            ) {
                                gestureMode = if (
                                    abs(totalDelta.x) > abs(totalDelta.y) * 1.2f
                                ) {
                                    GestureMode.OutfitSwipe
                                } else {
                                    action = PetAction.Dragged
                                    message = "Buddy is following your hand."
                                    GestureMode.VerticalDrag
                                }
                            }

                            if (gestureMode == GestureMode.VerticalDrag && delta != Offset.Zero) {
                                petOffset = Offset(
                                    x = 0f,
                                    y = (petOffset.y + delta.y).coerceIn(-maxVertical, maxVertical),
                                )
                                change.consume()
                            } else if (gestureMode == GestureMode.OutfitSwipe) {
                                change.consume()
                            }
                        }

                        when (gestureMode) {
                            GestureMode.OutfitSwipe -> {
                                when {
                                    totalDelta.x <= -outfitSwipeThreshold -> latestNextOutfit()
                                    totalDelta.x >= outfitSwipeThreshold -> latestPreviousOutfit()
                                    else -> message = state.statusLine
                                }
                            }

                            GestureMode.VerticalDrag -> react(PetInteraction.Drag)
                            GestureMode.Undecided -> {
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
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            PetAvatar(
                state = state,
                action = action,
                modifier = Modifier
                    .size(avatarSize)
                    .offset { IntOffset(petOffset.x.roundToInt(), petOffset.y.roundToInt()) },
            )
        }
    }
}

private enum class GestureMode {
    Undecided,
    VerticalDrag,
    OutfitSwipe,
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
