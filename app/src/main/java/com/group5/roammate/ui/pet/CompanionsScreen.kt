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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.pet.PetOutfit
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

/**
 * A fixed-height pet experience: one koala, one live interaction stage, one compact wardrobe.
 * Nothing in this content scrolls vertically, so the bottom navigation and all primary actions
 * remain reachable on the same screen.
 */
@Composable
fun CompanionsScreen(
    state: PetUiState,
    onOutfitSelected: (PetOutfit) -> Unit,
    onOpenCamera: () -> Unit,
    onTabClick: (RoamMateMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var treatTick by remember { mutableIntStateOf(0) }
    val previousOutfit = {
        onOutfitSelected(PetStateEngine.outfitAfter(state.outfit, -1))
    }
    val nextOutfit = {
        onOutfitSelected(PetStateEngine.outfitAfter(state.outfit, 1))
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

            PetHeader(outfit = state.outfit)

            Spacer(Modifier.height(12.dp))

            InteractivePetStage(
                state = state,
                treatTick = treatTick,
                onPreviousOutfit = previousOutfit,
                onNextOutfit = nextOutfit,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(Modifier.height(10.dp))

            OutfitSelector(
                selectedOutfit = state.outfit,
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
                    label = "Outfit",
                    containerColor = Color.White,
                    contentColor = PetTeal,
                    modifier = Modifier.weight(1f),
                    onClick = nextOutfit,
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
private fun PetHeader(outfit: PetOutfit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Buddy",
                color = PetTeal,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Your pocket koala · ${outfit.label}",
                color = PetMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = PetLightTeal,
        ) {
            Text(
                text = "KOALA",
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
    selectedOutfit: PetOutfit,
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

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = selectedOutfit.label,
                    color = PetText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    PetOutfit.entries.forEach { outfit ->
                        Box(
                            modifier = Modifier
                                .size(if (outfit == selectedOutfit) 7.dp else 5.dp)
                                .background(
                                    color = if (outfit == selectedOutfit) PetTeal else PetBorder,
                                    shape = CircleShape,
                                ),
                        )
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
            onOutfitSelected = {},
            onOpenCamera = {},
            onTabClick = {},
        )
    }
}
