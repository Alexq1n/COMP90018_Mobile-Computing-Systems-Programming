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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.pet.CompanionStyle
import com.group5.roammate.pet.PetEnvironmentSnapshot
import com.group5.roammate.pet.PetOutfitMode
import com.group5.roammate.pet.PetProfile
import com.group5.roammate.pet.PetStateEngine
import com.group5.roammate.pet.PetUiState
import com.group5.roammate.ui.screens.RoamMateBottomNavigation
import com.group5.roammate.ui.screens.RoamMateMainTab
import com.group5.roammate.ui.theme.RoamMateTheme

private val PetTeal = Color(0xFF008B8F)
private val PetLightTeal = Color(0xFFE6F5F3)
private val PetCoral = Color(0xFFFF6F61)
private val PetText = Color(0xFF17212B)
private val PetMuted = Color(0xFF76838B)
private val PetBorder = Color(0xFFDDE7E8)

@Composable
fun CompanionsScreen(
    state: PetUiState,
    profile: PetProfile,
    isRefreshingWeather: Boolean,
    weatherError: String?,
    onStyleSelected: (CompanionStyle) -> Unit,
    onOutfitModeSelected: (PetOutfitMode) -> Unit,
    onRefreshWeather: () -> Unit,
    onCycleGear: () -> Unit,
    onOpenCamera: () -> Unit,
    onTabClick: (RoamMateMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unlockedStyles = PetStateEngine.unlockedStyles(profile.checkedInPlaces)
    var treatTick by remember { mutableIntStateOf(0) }

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
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Companions",
                        color = PetTeal,
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "Buddy travels with your context",
                        color = PetMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PetLightTeal,
                ) {
                    Text(
                        text = "${state.environment.conditionLabel}  ${state.environment.temperatureC.toInt()}°",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        color = PetTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            InteractivePetStage(
                state = state,
                treatTick = treatTick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
            )

            Spacer(Modifier.height(14.dp))

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
                    label = "Outfit",
                    containerColor = Color.White,
                    contentColor = PetTeal,
                    modifier = Modifier.weight(1f),
                    onClick = onCycleGear,
                )
                PetQuickActionButton(
                    label = "Photo",
                    containerColor = PetCoral,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCamera,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Buddy keeps moving even when you do nothing—watch for personality-specific actions.",
                modifier = Modifier.fillMaxWidth(),
                color = PetMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = PetLightTeal,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Buddy the ${state.companionStyle.displayName}",
                            color = PetText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = state.environment.locationLabel,
                            color = PetMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Text(
                        text = "${state.mood.label} · ${state.outfit.label}",
                        color = PetTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            SectionTitle(
                title = "Weather wardrobe",
                subtitle = "Auto changes Buddy's gear using live weather",
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(PetOutfitMode.entries) { mode ->
                    OutfitModeCard(
                        mode = mode,
                        selected = profile.outfitMode == mode,
                        onClick = { onOutfitModeSelected(mode) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            SectionTitle(
                title = "Buddy style",
                subtitle = "One companion at a time; new looks unlock through travel",
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(CompanionStyle.entries) { style ->
                    CompanionStyleCard(
                        style = style,
                        selected = profile.selectedStyle == style,
                        unlocked = style in unlockedStyles,
                        onClick = { if (style in unlockedStyles) onStyleSelected(style) },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF7FAFA),
                border = BorderStroke(1.dp, PetBorder),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = state.travelTip,
                        color = PetText,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (weatherError == null) {
                                "Weather: ${state.environment.source}"
                            } else {
                                "Live weather unavailable · showing saved data"
                            },
                            color = PetMuted,
                            fontSize = 12.sp,
                        )
                        TextButton(
                            onClick = onRefreshWeather,
                            enabled = !isRefreshingWeather,
                        ) {
                            Text(
                                text = if (isRefreshingWeather) "Refreshing…" else "Refresh",
                                color = PetTeal,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
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
        modifier = modifier.height(50.dp),
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

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            color = PetTeal,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = subtitle,
            color = PetMuted,
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun OutfitModeCard(
    mode: PetOutfitMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(88.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PetTeal else Color.White,
        border = BorderStroke(1.dp, if (selected) PetTeal else PetBorder),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        color = if (selected) Color.White.copy(alpha = 0.23f) else PetLightTeal,
                        shape = CircleShape,
                    ),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = mode.label,
                color = if (selected) Color.White else PetText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CompanionStyleCard(
    style: CompanionStyle,
    selected: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(126.dp)
            .height(146.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = unlocked, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = when {
            selected -> PetLightTeal
            unlocked -> Color.White
            else -> Color(0xFFF1F3F3)
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) PetTeal else PetBorder,
        ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(style.imageRes()),
                contentDescription = style.displayName,
                modifier = Modifier.size(82.dp),
                alpha = if (unlocked) 1f else 0.32f,
            )
            Text(
                text = style.displayName,
                color = if (unlocked) PetText else PetMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = if (unlocked) {
                    if (selected) "Selected" else "Unlocked"
                } else {
                    "Locked"
                },
                color = if (selected) PetTeal else PetMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun CompanionsScreenPreview() {
    val profile = PetProfile()
    RoamMateTheme {
        CompanionsScreen(
            state = PetStateEngine.buildUiState(PetEnvironmentSnapshot.demo(), profile),
            profile = profile,
            isRefreshingWeather = false,
            weatherError = null,
            onStyleSelected = {},
            onOutfitModeSelected = {},
            onRefreshWeather = {},
            onCycleGear = {},
            onOpenCamera = {},
            onTabClick = {},
        )
    }
}
