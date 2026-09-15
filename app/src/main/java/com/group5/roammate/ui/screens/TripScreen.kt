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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.ui.theme.RoamMateTheme

// on-screen -> code:
//   "Trip" + destination + weather pill  -> TripHeader / WeatherPill
//   timeline (time · dot · title · tag)  -> TripTimeline > TripTimelineRow
//   dot (done/current/upcoming)          -> TimelineDot
//   "Hidden gem" tag                     -> HiddenStopTag
//   buttons: Edit itinerary / Start navigation -> TripActionBar
//   bottom tabs                          -> RoamMateBottomNavigation

// colors
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateTimelineGrey = Color(0xFFC9D3D5)
private val RoamMateWeatherBackground = Color(0xFFFFF4D7)
private val RoamMateWeatherText = Color(0xFF8A661D)
private val RoamMateHiddenBackground = Color(0xFFFFF7E7)
private val RoamMateHiddenText = Color(0xFF936B16)

// data: one stop in today's timeline
data class TripTimelineStop(
    val time: String,
    val title: String,
    val status: TripStopStatus,
    // Zewen 如果插入顺路小众点，就在这里传入小标签；普通站点保持 null。
    val hiddenTag: String? = null,
)

// data: weather summary
data class TripWeatherSummary(
    val temperature: String,
    val condition: String,
)

// stop status (dot style)
enum class TripStopStatus {
    Done,
    Current,
    Upcoming,
}

// ---- screen ----
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
        // bottom: action buttons + tabs
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
        // scroll column
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

            // header (title + weather)
            TripHeader(
                destinationTitle = destinationTitle,
                weatherSummary = weatherSummary,
            )

            Spacer(modifier = Modifier.height(26.dp))

            // TODO: 之后这里显示 Zewen 生成的真实行程顺序，以及 Sitao 提供的当前进度。
            // timeline
            TripTimeline(
                stops = stops,
                onStopClick = onStopClick,
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ---- header ("Trip" + destination + weather pill) ----
@Composable
private fun TripHeader(
    destinationTitle: String,
    weatherSummary: TripWeatherSummary,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // "Trip" title
        Text(
            text = "Trip",
            color = RoamMateTeal,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // destination line
        Text(
            text = destinationTitle,
            color = RoamMateText,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // weather pill (right-aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeatherPill(weatherSummary = weatherSummary)
        }
    }
}

// ---- weather pill (yellow, "18°C · Partly cloudy") ----
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

// ---- timeline (list of stops, or empty) ----
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

// ---- empty timeline (no stops) ----
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

// ---- one row (time · dot · title · optional tag) ; tap = open stop ----
@Composable
private fun TripTimelineRow(
    stop: TripTimelineStop,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    // taller row when a hidden tag is shown
    val rowHeight = if (stop.hiddenTag == null) {
        92.dp
    } else {
        112.dp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clickable(
                // no ripple; keep row clean
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // time (teal if current)
        Text(
            text = stop.time,
            modifier = Modifier.width(74.dp),
            color = if (stop.status == TripStopStatus.Current) RoamMateTeal else RoamMateMutedText,
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        // dot + connector line
        TimelineDotColumn(
            status = stop.status,
            showTopLine = !isFirst,
            showBottomLine = !isLast,
            rowHeight = rowHeight,
        )

        Spacer(modifier = Modifier.width(20.dp))

        // title + optional hidden tag
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.title,
                color = RoamMateText,
                fontSize = 23.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            stop.hiddenTag?.let { tagText ->
                Spacer(modifier = Modifier.height(8.dp))
                HiddenStopTag(text = tagText)
            }
        }
    }
}

// ---- hidden-gem tag (backend-inserted stop) ----
@Composable
private fun HiddenStopTag(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = RoamMateHiddenBackground,
        border = BorderStroke(1.dp, RoamMateHiddenText.copy(alpha = 0.14f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = RoamMateHiddenText,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

// ---- dot + line stack (top line · dot · bottom line) ----
@Composable
private fun TimelineDotColumn(
    status: TripStopStatus,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    rowHeight: Dp,
) {
    Column(
        modifier = Modifier
            .width(28.dp)
            .height(rowHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TimelineLineSegment(visible = showTopLine, modifier = Modifier.weight(1f))

        TimelineDot(status = status)

        TimelineLineSegment(visible = showBottomLine, modifier = Modifier.weight(1f))
    }
}

// ---- vertical connector line ----
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

// ---- dot (done = grey, current = teal, upcoming = white) ----
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

// ---- bottom buttons: Edit itinerary (outline) + Start navigation (teal) ----
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
            // button: Edit itinerary -> edit page
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

            // button: Start navigation -> external map
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

// preview
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
                TripTimelineStop(
                    time = "13:30",
                    title = "Pidapipo Gelato",
                    status = TripStopStatus.Upcoming,
                    hiddenTag = "Hidden gem · nearby",
                ),
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