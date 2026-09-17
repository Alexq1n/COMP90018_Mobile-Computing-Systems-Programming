package com.group5.roammate.ui.pet

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.pet.PetEnvironmentSnapshot
import com.group5.roammate.pet.PetProfile
import com.group5.roammate.pet.PetStateEngine
import com.group5.roammate.pet.PetUiState
import com.group5.roammate.ui.theme.RoamMateTheme
import kotlin.math.sqrt

private val PlayTeal = Color(0xFF008B8F)
private val PlayCoral = Color(0xFFFF6F61)
private val PlayText = Color(0xFF17212B)
private val PlayMuted = Color(0xFF76838B)

@Composable
fun PlayWithBuddyScreen(
    state: PetUiState,
    onClose: () -> Unit,
    onCycleGear: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var interactionTick by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf(state.travelTip) }
    var hearts by remember { mutableStateOf(false) }

    ShakeDetectorEffect {
        interactionTick += 1
        message = "Route check complete! Buddy is ready for ${state.environment.locationLabel}."
        hearts = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFDDF5F2), Color(0xFFF9FCFB), Color(0xFFE7D3B8)),
                ),
            )
            .statusBarsPadding(),
    ) {
        Surface(
            modifier = Modifier
                .padding(18.dp)
                .size(42.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("×", color = PlayText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 18.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.88f),
        ) {
            Text(
                text = "${state.environment.conditionLabel} · ${state.outfit.label}",
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                color = PlayTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(54.dp))
            Text(
                text = "Buddy",
                color = PlayText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = state.environment.locationLabel,
                color = PlayMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(28.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, PlayTeal.copy(alpha = 0.12f)),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    color = PlayText,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .size(340.dp)
                    .clickable {
                        interactionTick += 1
                        hearts = !hearts
                        message = "Buddy loved that! Tap Photo to save this travel moment."
                    },
                contentAlignment = Alignment.Center,
            ) {
                PetAvatar(
                    state = state,
                    modifier = Modifier.size(320.dp),
                    interactionTick = interactionTick,
                )
                if (hearts) {
                    Text(
                        text = "♥  ♥",
                        modifier = Modifier.align(Alignment.TopEnd),
                        color = PlayCoral,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = "Tap Buddy or shake your phone for a travel reaction",
                color = PlayMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlayActionButton(
                    label = "Cheer",
                    containerColor = Color.White,
                    contentColor = PlayTeal,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        interactionTick += 1
                        hearts = true
                        message = "Let's make the next stop a great one!"
                    },
                )
                PlayActionButton(
                    label = "Gear",
                    containerColor = Color.White,
                    contentColor = PlayTeal,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onCycleGear()
                        interactionTick += 1
                        message = "Trying another travel outfit."
                    },
                )
                PlayActionButton(
                    label = "Photo",
                    containerColor = PlayCoral,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCamera,
                )
            }

            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun PlayActionButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        border = if (containerColor == Color.White) BorderStroke(1.dp, PlayTeal.copy(alpha = 0.2f)) else null,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlayWithBuddyPreview() {
    val profile = PetProfile()
    RoamMateTheme {
        PlayWithBuddyScreen(
            state = PetStateEngine.buildUiState(PetEnvironmentSnapshot.demo(), profile),
            onClose = {},
            onCycleGear = {},
            onOpenCamera = {},
        )
    }
}
