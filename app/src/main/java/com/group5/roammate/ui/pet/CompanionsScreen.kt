package com.group5.roammate.ui.pet

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.pet.PetProfile
import com.group5.roammate.pet.PetStateEngine
import com.group5.roammate.pet.PetTripContext
import com.group5.roammate.pet.PetUiState
import com.group5.roammate.pet.PetWardrobeChoice
import com.group5.roammate.ui.screens.RoamMateBottomNavigation
import com.group5.roammate.ui.screens.RoamMateMainTab
import com.group5.roammate.ui.theme.RoamMateTheme

private val PetTeal = Color(0xFF008B8F)
private val PetLightTeal = Color(0xFFE6F5F3)
private val PetCoral = Color(0xFFFF6F61)
private val PetText = Color(0xFF17212B)
private val PetMuted = Color(0xFF76838B)
private val PetBorder = Color(0xFFDDE7E8)

/**
 * A fixed-height pet experience: one koala, one live interaction stage, one compact wardrobe.
 * Nothing in this content scrolls vertically, so the bottom navigation and all primary actions
 * remain reachable on the same screen.
 */
@Composable
fun CompanionsScreen(
    state: PetUiState,
    onWardrobeSelected: (PetWardrobeChoice) -> Unit,
    onOpenCamera: () -> Unit,
    onTabClick: (RoamMateMainTab) -> Unit,
    tripContext: PetTripContext = PetTripContext(),
    environment: PetEnvironment? = null,
    modifier: Modifier = Modifier,
) {
    var treatTick by remember { mutableIntStateOf(0) }
    var showEnvironment by remember { androidx.compose.runtime.mutableStateOf(false) }
    val motion = rememberPetMotion()
    if (showEnvironment) {
        PetEnvironmentDialog(state, environment, motion) { showEnvironment = false }
    }
    val previousOutfit = {
        onWardrobeSelected(PetStateEngine.wardrobeAfter(state.wardrobeChoice, -1))
    }
    val nextOutfit = {
        onWardrobeSelected(PetStateEngine.wardrobeAfter(state.wardrobeChoice, 1))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            RoamMateBottomNavigation(
                selectedTab = RoamMateMainTab.Pet,
                onTabClick = onTabClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            PetHeader(state = state, isMoving = motion.state.isMoving, onWeatherClick = { showEnvironment = true })

            Spacer(Modifier.height(12.dp))

            InteractivePetStage(
                state = state,
                treatTick = treatTick,
                onPreviousOutfit = previousOutfit,
                onNextOutfit = nextOutfit,
                tripContext = tripContext,
                isMoving = motion.state.isMoving,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(Modifier.height(10.dp))

            OutfitSelector(
                state = state,
                onPrevious = previousOutfit,
                onNext = nextOutfit,
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PetQuickActionButton(
                    label = "Treat",
                    containerColor = Color.White,
                    contentColor = PetTeal,
                    modifier = Modifier.weight(1f),
                    onClick = { treatTick += 1 },
                )
                PetQuickActionButton(
                    label = "Photo",
                    containerColor = PetCoral,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCamera,
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PetHeader(state: PetUiState, isMoving: Boolean, onWeatherClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        ) {
            Text(
                text = "Buddy",
                color = PetTeal,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = if (isMoving) "Walking together" else "Your little travel companion",
                color = PetMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Surface(
            modifier = Modifier.clickable(onClickLabel = "Weather and motion settings", onClick = onWeatherClick),
            shape = RoundedCornerShape(18.dp),
            color = PetLightTeal,
        ) {
            Text(
                text = if (state.weather.isCurrent) {
                    "${state.weather.label} · ${state.weather.temperatureC.toInt()}°"
                } else "Weather —",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = PetTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun OutfitSelector(
    state: PetUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF7FAFA),
        border = BorderStroke(1.dp, PetBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutfitArrow(label = "‹", contentDescription = "Previous outfit", onClick = onPrevious)

            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                PetAvatar(
                    state = state.copy(outfit = state.wardrobeChoice.manualOutfit ?: state.outfit),
                    modifier = Modifier.size(38.dp), animateIdle = false,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.wardrobeChoice.label,
                        color = PetText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = state.wardrobeNote,
                        color = PetMuted, fontSize = 10.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        PetWardrobeChoice.entries.forEach { choice ->
                            Box(
                                modifier = Modifier
                                    .size(if (choice == state.wardrobeChoice) 7.dp else 5.dp)
                                    .background(
                                        color = if (choice == state.wardrobeChoice) PetTeal else PetBorder,
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }

            }
            OutfitArrow(label = "›", contentDescription = "Next outfit", onClick = onNext)
        }
    }
}

@Composable
private fun OutfitArrow(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = PetLightTeal,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = PetTeal,
                fontSize = 28.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PetQuickActionButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        border = if (containerColor == Color.White) BorderStroke(1.dp, PetBorder) else null,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CompanionsScreenPreview() {
    val profile = PetProfile()
    RoamMateTheme {
        CompanionsScreen(
            state = PetStateEngine.buildUiState(profile),
            onWardrobeSelected = {},
            onOpenCamera = {},
            onTabClick = {},
        )
    }
}

@Composable
private fun PetEnvironmentDialog(
    state: PetUiState,
    environment: PetEnvironment?,
    motion: PetMotionController,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buddy's surroundings") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.weather.locationLabel, fontWeight = FontWeight.Bold)
                Text(if (state.weather.isCurrent) {
                    "${state.weather.label}, ${state.weather.temperatureC.toInt()}°C · wind ${state.weather.windSpeedKmh.toInt()} km/h"
                } else "No fresh weather available. Buddy won't guess the conditions.")
                environment?.status?.let { Text(it, fontSize = 12.sp) }
                if (environment != null) {
                    TextButton(onClick = environment::requestLocationPermission) { Text("Use my location") }
                    TextButton(onClick = environment::useMelbourne) { Text("Use Melbourne") }
                    TextButton(onClick = environment::refresh, enabled = !environment.isRefreshing) {
                        Text(if (environment.isRefreshing) "Updating…" else "Refresh weather")
                    }
                }
                Text("Weather protection comes first", fontWeight = FontWeight.Bold)
                Text("Buddy wears fitted rain, winter, wind or sun gear when needed. Your chosen fashion is saved and returns in mild weather.")
                Text("Motion: ${motion.state.source.label}")
                if (motion.state.needsActivityPermission) {
                    TextButton(onClick = motion.requestActivityPermission) { Text("Enable step detection") }
                }
                Text("Without a step sensor, movement is estimated from the phone. A desk or emulator may not produce walking events.", fontSize = 12.sp)
                TextButton(onClick = { uriHandler.openUri("https://open-meteo.com/") }) {
                    Text("Weather data: Open-Meteo · CC BY 4.0")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
