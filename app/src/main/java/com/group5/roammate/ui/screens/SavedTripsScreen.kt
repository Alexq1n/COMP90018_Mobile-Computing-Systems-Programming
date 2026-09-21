package com.group5.roammate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.ui.theme.RoamMateTheme

// code map:
//   back button       -> top left circle
//   trip cards        -> saved history
//   empty state       -> no saved data
//   create button     -> Plan My Trip

// colors
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)

// data
data class SavedTrip(
    val destination: String,
    val durationText: String,
    val dateText: String,
)

// screen
@Composable
fun SavedTripsScreen(
    savedTrips: List<SavedTrip>,
    onBackClick: () -> Unit,
    onCreateNewTripClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {

            // back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = RoamMateLightTeal,
                        shape = CircleShape,
                    ),
            ) {
                BackArrowIcon(
                    tint = RoamMateTeal,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // title
            Text(
                text = "Saved trips",
                color = RoamMateTeal,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // body
            if (savedTrips.isEmpty()) {
                EmptySavedTripsState(
                    modifier = Modifier.weight(1f),
                    onCreateNewTripClick = onCreateNewTripClick,
                )
            } else {
                SavedTripsList(
                    savedTrips = savedTrips,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.height(18.dp))

                // create button
                CreateNewTripButton(
                    buttonText = "Create new trip",
                    onClick = onCreateNewTripClick,
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

// list
@Composable
private fun SavedTripsList(
    savedTrips: List<SavedTrip>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        // subtitle
        Text(
            text = "Places you've been",
            color = RoamMateText,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            savedTrips.forEach { trip ->
                // trip card
                SavedTripCard(savedTrip = trip)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// card
@Composable
private fun SavedTripCard(
    savedTrip: SavedTrip,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            // destination
            Text(
                text = savedTrip.destination,
                color = RoamMateText,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // trip info
            Text(
                text = "${savedTrip.durationText} · ${savedTrip.dateText}",
                color = RoamMateMutedText,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// empty
@Composable
private fun EmptySavedTripsState(
    onCreateNewTripClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // map icon
        Box(
            modifier = Modifier
                .size(118.dp)
                .background(
                    color = RoamMateLightTeal,
                    shape = RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            MapIcon(
                tint = RoamMateTeal,
                modifier = Modifier.size(58.dp),
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // empty title
        Text(
            text = "No saved trips yet",
            color = RoamMateTeal,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // empty text
        Text(
            text = "Plan a day and save it here to reuse anytime.",
            color = RoamMateMutedText,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(34.dp))

        // plan button
        CreateNewTripButton(
            buttonText = "Plan a trip",
            onClick = onCreateNewTripClick,
        )
    }
}

// button
@Composable
private fun CreateNewTripButton(
    buttonText: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RoamMateTeal,
            contentColor = Color.White,
        ),
    ) {
        Text(
            text = buttonText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// back icon
@Composable
private fun BackArrowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawLine(
            color = tint,
            start = Offset(size.width * 0.62f, size.height * 0.22f),
            end = Offset(size.width * 0.36f, size.height * 0.50f),
            strokeWidth = 3.dp.toPx(),
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.36f, size.height * 0.50f),
            end = Offset(size.width * 0.62f, size.height * 0.78f),
            strokeWidth = 3.dp.toPx(),
        )
    }
}

// map icon
@Composable
private fun MapIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawMapIcon(tint)
    }
}

// icon draw
private fun DrawScope.drawMapIcon(
    tint: Color,
) {
    val path = Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.25f)
        lineTo(size.width * 0.38f, size.height * 0.16f)
        lineTo(size.width * 0.62f, size.height * 0.25f)
        lineTo(size.width * 0.82f, size.height * 0.16f)
        lineTo(size.width * 0.82f, size.height * 0.75f)
        lineTo(size.width * 0.62f, size.height * 0.84f)
        lineTo(size.width * 0.38f, size.height * 0.75f)
        lineTo(size.width * 0.18f, size.height * 0.84f)
        close()
    }

    drawPath(
        path = path,
        color = tint,
        style = Stroke(width = 4.dp.toPx()),
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.38f, size.height * 0.16f),
        end = Offset(size.width * 0.38f, size.height * 0.75f),
        strokeWidth = 4.dp.toPx(),
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.62f, size.height * 0.25f),
        end = Offset(size.width * 0.62f, size.height * 0.84f),
        strokeWidth = 4.dp.toPx(),
    )
}

// preview list
@Preview(showBackground = true)
@Composable
private fun SavedTripsScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        SavedTripsScreen(
            savedTrips = listOf(
                SavedTrip("Melbourne", "3 days", "May 2025"),
                SavedTrip("Great Ocean Road", "1 day", "Mar 2025"),
                SavedTrip("Sydney", "2 days", "Jan 2025"),
            ),
            onBackClick = {},
            onCreateNewTripClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// preview empty
@Preview(showBackground = true)
@Composable
private fun EmptySavedTripsScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        SavedTripsScreen(
            savedTrips = emptyList(),
            onBackClick = {},
            onCreateNewTripClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
