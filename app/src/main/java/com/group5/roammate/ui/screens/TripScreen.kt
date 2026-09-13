package com.group5.roammate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.ui.theme.RoamMateTheme

// Trip page uniform color
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateTimelineGrey = Color(0xFFC9D3D5)
private val RoamMateWeatherBackground = Color(0xFFFFF4D7)
private val RoamMateWeatherText = Color(0xFF8A661D)

// data: one stop in today's trip timeline
data class TripTimelineStop(
    val time: String,
    val title: String,
    val status: TripStopStatus,
)

// data: weather summary shown on Trip page
data class TripWeatherSummary(
    val temperature: String,
    val condition: String,
)

// stop status used by the timeline dot
enum class TripStopStatus {
    Done,
    Current,
    Upcoming,
}

@Composable
fun TripScreen(
    destinationTitle: String,
    weatherSummary: TripWeatherSummary,
    stops: List<TripTimelineStop>,
    onStopClick: (TripTimelineStop) -> Unit,
    onEditItineraryClick: () -> Unit,
    onStartNavigationClick: () -> Unit,
    onTabClick: (RoamMateMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            Column {
                TripActionBar(
                    onEditItineraryClick = onEditItineraryClick,
                    onStartNavigationClick = onStartNavigationClick,
                )

                RoamMateBottomNavigation(
                    selectedTab = RoamMateMainTab.Trip,
                    onTabClick = onTabClick,
                )
            }
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
            Spacer(modifier = Modifier.height(28.dp))

            TripHeader(
                destinationTitle = destinationTitle,
                weatherSummary = weatherSummary,
            )

            Spacer(modifier = Modifier.height(26.dp))

            // TODO: 之后这里显示 Zewen 生成的真实行程顺序，以及 Sitao 提供的当前进度。
            TripTimeline(
                stops = stops,
                onStopClick = onStopClick,
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ---- header ----
@Composable
private fun TripHeader(
    destinationTitle: String,
    weatherSummary: TripWeatherSummary,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Trip",
            color = RoamMateTeal,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = destinationTitle,
            color = RoamMateText,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeatherPill(weatherSummary = weatherSummary)
        }
    }
}

// ---- weather pill ----
@Composable
private fun WeatherPill(
    weatherSummary: TripWeatherSummary,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = RoamMateWeatherBackground,
    ) {
        Text(
            text = "${weatherSummary.temperature} · ${weatherSummary.condition}",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = RoamMateWeatherText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

// ---- timeline ----
@Composable
private fun TripTimeline(
    stops: List<TripTimelineStop>,
    onStopClick: (TripTimelineStop) -> Unit,
) {
    if (stops.isEmpty()) {
        EmptyTripTimeline()
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            stops.forEachIndexed { index, stop ->
                TripTimelineRow(
                    stop = stop,
                    isFirst = index == 0,
                    isLast = index == stops.lastIndex,
                    onClick = { onStopClick(stop) },
                )
            }
        }
    }
}

// ---- empty timeline ----
@Composable
private fun EmptyTripTimeline() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = RoamMateLightTeal.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.12f)),
    ) {
        Text(
            text = "No trip stops yet",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            color = RoamMateTeal,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

// ---- timeline row ----
@Composable
private fun TripTimelineRow(
    stop: TripTimelineStop,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clickable(
                // Keep the row clean; only the dot shows progress status.
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stop.time,
            modifier = Modifier.width(74.dp),
            color = if (stop.status == TripStopStatus.Current) RoamMateTeal else RoamMateMutedText,
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        TimelineDotColumn(
            status = stop.status,
            showTopLine = !isFirst,
            showBottomLine = !isLast,
        )

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = stop.title,
            modifier = Modifier.weight(1f),
            color = RoamMateText,
            fontSize = 23.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// ---- timeline dot + line ----
@Composable
private fun TimelineDotColumn(
    status: TripStopStatus,
    showTopLine: Boolean,
    showBottomLine: Boolean,
) {
    Column(
        modifier = Modifier
            .width(28.dp)
            .height(92.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TimelineLineSegment(visible = showTopLine, modifier = Modifier.weight(1f))

        TimelineDot(status = status)

        TimelineLineSegment(visible = showBottomLine, modifier = Modifier.weight(1f))
    }
}

// ---- vertical connector ----
@Composable
private fun TimelineLineSegment(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .width(2.dp)
                .background(if (visible) RoamMateTimelineGrey else Color.Transparent),
        )
    }
}

// ---- dot color: done grey, current teal, upcoming white ----
@Composable
private fun TimelineDot(
    status: TripStopStatus,
) {
    val fillColor = when (status) {
        TripStopStatus.Done -> RoamMateTimelineGrey
        TripStopStatus.Current -> RoamMateTeal
        TripStopStatus.Upcoming -> Color.White
    }

    val borderColor = when (status) {
        TripStopStatus.Done -> RoamMateTimelineGrey
        TripStopStatus.Current -> RoamMateTeal
        TripStopStatus.Upcoming -> RoamMateTimelineGrey
    }

    Surface(
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        color = fillColor,
        border = BorderStroke(2.dp, borderColor),
    ) {}
}

// ---- bottom action buttons ----
@Composable
private fun TripActionBar(
    onEditItineraryClick: () -> Unit,
    onStartNavigationClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedButton(
                onClick = onEditItineraryClick,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RoamMateTeal),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RoamMateTeal,
                ),
            ) {
                Text(
                    text = "Edit itinerary",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Button(
                onClick = onStartNavigationClick,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoamMateTeal,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Start navigation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        TripScreen(
            destinationTitle = "Today in Melbourne",
            weatherSummary = TripWeatherSummary(
                temperature = "18°C",
                condition = "Partly cloudy",
            ),
            stops = listOf(
                TripTimelineStop("09:00", "Federation Square", TripStopStatus.Done),
                TripTimelineStop("10:00", "Melbourne Museum", TripStopStatus.Current),
                TripTimelineStop("12:00", "Lunch nearby", TripStopStatus.Upcoming),
                TripTimelineStop("14:00", "Royal Botanic Gardens", TripStopStatus.Upcoming),
            ),
            onStopClick = {},
            onEditItineraryClick = {},
            onStartNavigationClick = {},
            onTabClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
