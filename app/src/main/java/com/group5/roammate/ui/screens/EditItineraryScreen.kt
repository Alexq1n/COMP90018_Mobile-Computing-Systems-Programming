package com.group5.roammate.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.ui.theme.RoamMateTheme

// Edit itinerary page uniform color
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateCoral = Color(0xFFFF6F61)
private val RoamMateLightCoral = Color(0xFFFFEEE9)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)

@Composable
fun EditItineraryScreen(
    stops: List<TripTimelineStop>,
    onBackClick: () -> Unit,
    onRemoveStopClick: (TripTimelineStop) -> Unit,
    onAddStopClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            SaveItineraryButton(onClick = onSaveClick)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            BackCircleButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Edit itinerary",
                color = RoamMateTeal,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Remove a stop, or add a new one",
                color = RoamMateMutedText,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // TODO: 之后这里接 Zewen 的真实行程站点列表。
            if (stops.isEmpty()) {
                EmptyEditItineraryCard()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    stops.forEach { stop ->
                        EditItineraryStopCard(
                            stop = stop,
                            onRemoveClick = { onRemoveStopClick(stop) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AddStopButton(onClick = onAddStopClick)

            Spacer(modifier = Modifier.height(34.dp))
        }
    }
}

// ---- back button ----
@Composable
private fun BackCircleButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(RoamMateLightTeal, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "<",
            color = RoamMateTeal,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ---- one editable stop row ----
@Composable
private fun EditItineraryStopCard(
    stop: TripTimelineStop,
    onRemoveClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stop.time,
                modifier = Modifier.width(74.dp),
                color = RoamMateTeal,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            // Only show place name here; no place icon on this page.
            Text(
                text = stop.title,
                modifier = Modifier.weight(1f),
                color = RoamMateText,
                fontSize = 20.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.width(14.dp))

            RemoveStopButton(onClick = onRemoveClick)
        }
    }
}

// ---- remove button ----
@Composable
private fun RemoveStopButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(RoamMateLightCoral, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "-",
            color = RoamMateCoral,
            fontSize = 24.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

// ---- add stop ----
@Composable
private fun AddStopButton(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateTeal),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "+ Add a stop",
                color = RoamMateTeal,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ---- empty state ----
@Composable
private fun EmptyEditItineraryCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = RoamMateLightTeal.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.12f)),
    ) {
        Text(
            text = "No editable stops",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            color = RoamMateTeal,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

// ---- save button ----
@Composable
private fun SaveItineraryButton(
    onClick: () -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 12.dp)
                .height(62.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoamMateTeal,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = "Save",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditItineraryScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        EditItineraryScreen(
            stops = listOf(
                TripTimelineStop("10:00", "Melbourne Museum", TripStopStatus.Current),
                TripTimelineStop("12:00", "Lunch nearby", TripStopStatus.Upcoming),
                TripTimelineStop("14:00", "Royal Botanic Gardens", TripStopStatus.Upcoming),
            ),
            onBackClick = {},
            onRemoveStopClick = {},
            onAddStopClick = {},
            onSaveClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
