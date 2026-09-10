package com.group5.roammate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.R
import com.group5.roammate.ui.theme.RoamMateTheme


// Home page uniform color
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateCoral = Color(0xFFFF6F61)
private val RoamMateLightCoral = Color(0xFFFFEEE9)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)
private val RoamMateBlueText = Color(0xFF2E5F9E)
private val RoamMateLightBlue = Color(0xFFEAF4FF)


// home page data model
data class HomeTripStop(
    val time: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val status: HomeTripStopStatus,
)

// pet card
// 夏桀 替换imageRes/name/description/moodLabel
data class HomePetStatus(
    val name: String,
    val description: String,
    val moodLabel: String,
    val imageRes: Int,
)

// stop status: Done, Current, Next
enum class HomeTripStopStatus {
    Done,
    Current,
    Next,
}

// Home screen sub-component
// all onXxxClick are callback
@Composable
fun HomeScreen(
    userName: String,
    modifier: Modifier = Modifier,
    todayTripStops: List<HomeTripStop> = emptyList(),
    showLeaveNowReminder: Boolean = false,
    petStatus: HomePetStatus,
    onNavigateReminderClick: () -> Unit,
    onSmartSuggestionClick: () -> Unit,
    onPetCardClick: () -> Unit,
    onPlanMyTripClick: () -> Unit,
    onExploreClick: () -> Unit,
    onTripClick: () -> Unit,
    onTabClick: (RoamMateMainTab) -> Unit,
) {
    // Scaffold = Page skeleton；bottomBar 5 tab
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            RoamMateBottomNavigation(
                selectedTab = RoamMateMainTab.Home,
                onTabClick = onTabClick,
            )
        },
    ) { innerPadding ->
        // content list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())   // content can be scrolled
                .padding(horizontal = 28.dp),
        ) {
            Spacer(modifier = Modifier.height(26.dp))

            // header
            HomeHeader()

            Spacer(modifier = Modifier.height(18.dp))

            // reminder: only for overtime
            if (showLeaveNowReminder) {
                LeaveNowReminderCard(onNavigateClick = onNavigateReminderClick)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // smart suggestion for weather: click to adjust
            SmartSuggestionCard(onClick = onSmartSuggestionClick)

            Spacer(modifier = Modifier.height(14.dp))

            // pet card: click to Companions
            PetStatusCard(
                petStatus = petStatus,
                onClick = onPetCardClick,
            )

            Spacer(modifier = Modifier.height(22.dp))

            // — “Today's trip”
            Text(
                text = "Today's trip",
                color = RoamMateTeal,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // — today's trip card
            if (todayTripStops.isNotEmpty()) {
                TodayTripCard(
                    stops = todayTripStops,
                    onClick = onTripClick,
                )
            } else {
                NoTripTodayCard()
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Plan My Trip / Explore —
            QuickActionButtons(
                onPlanMyTripClick = onPlanMyTripClick,
                onExploreClick = onExploreClick,
            )

            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

// Home header
@Composable
private fun HomeHeader() {
    Column {
        Text(
            text = "Home",
            color = RoamMateTeal,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Text(
            text = "Good morning",
            color = RoamMateText,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// Leave now reminder
@Composable
private fun LeaveNowReminderCard(
    onNavigateClick: () -> Unit,   //  “Navigate” to External map
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = RoamMateLightCoral,
        border = BorderStroke(1.dp, RoamMateCoral.copy(alpha = 0.26f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // leave now logo
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(RoamMateCoral, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // leave now context
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Leave now",
                    color = RoamMateCoral,
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                Text(
                    text = "Melbourne Museum at 10:00 · you're 3 min behind",
                    color = RoamMateText,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Navigate click
            Text(
                text = "Navigate >",
                modifier = Modifier.clickable(onClick = onNavigateClick),
                color = RoamMateTeal,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// Smart Suggestion Card for weather
@Composable
private fun SmartSuggestionCard(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = RoamMateLightBlue,
        border = BorderStroke(1.dp, RoamMateBlueText.copy(alpha = 0.18f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Rain",
                color = RoamMateBlueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Smart suggestion · Rain 2-4 PM, tap to adjust your plan",
                modifier = Modifier.weight(1f),
                color = RoamMateBlueText,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Text(
                text = ">",
                color = RoamMateBlueText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

//  Pet Status Card
@Composable
private fun PetStatusCard(
    petStatus: HomePetStatus,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = RoamMateLightTeal.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.12f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // pet logo, 等夏桀
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = petStatus.imageRes),
                    contentDescription = petStatus.name,
                    modifier = Modifier.size(46.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // name + state
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = petStatus.name,
                    color = RoamMateText,
                    fontSize = 23.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                Text(
                    text = petStatus.description,
                    color = RoamMateMutedText,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // pet state
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFFFF4D7),
            ) {
                Text(
                    text = petStatus.moodLabel,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color(0xFF9A6B18),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = ">",
                color = RoamMateTeal,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// Today Trip Card
@Composable
private fun TodayTripCard(
    stops: List<HomeTripStop>,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            // Draw a dividing line for each station;
            // The last line is not drawn
            stops.forEachIndexed { index, stop ->
                HomeTripRow(stop = stop)

                if (index != stops.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(RoamMateFieldBorder.copy(alpha = 0.65f)),
                    )
                }
            }
        }
    }
}

// No Trip Today Card
@Composable
private fun NoTripTodayCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = RoamMateLightTeal.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // No trips today
            Text(
                text = "No trips today",
                color = RoamMateTeal,
                fontSize = 23.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // subtitle for no trips today
            Text(
                text = "Take it easy, or start planning a new trip below  🌿",
                color = RoamMateMutedText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Home Trip Row
// Status: left=time, middle=place only, right= ✓ / now / next
@Composable
private fun HomeTripRow(
    stop: HomeTripStop,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // time
        Text(
            text = stop.time,
            modifier = Modifier.width(68.dp),
            color = if (stop.status == HomeTripStopStatus.Current) RoamMateTeal else RoamMateMutedText,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        // stop logo
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(RoamMateLightTeal, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stop.icon,
                fontSize = 18.sp,
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // place name
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.title,
                modifier = Modifier.align(Alignment.CenterStart),
                color = if (stop.status == HomeTripStopStatus.Done) RoamMateMutedText else RoamMateText,
                fontSize = 19.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textDecoration = if (stop.status == HomeTripStopStatus.Done) {
                    TextDecoration.LineThrough    // Done= Gray + strikethrough
                } else {
                    TextDecoration.None
                },
            )
        }

        // past shows ✓, current shows "now", next shows "next"
        when (stop.status) {
            HomeTripStopStatus.Done -> {
                Text(
                    text = "✓",
                    color = Color(0xFF6FCF97),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            HomeTripStopStatus.Current -> {
                TripStatusBadge(
                    label = "now",
                    backgroundColor = RoamMateLightCoral,
                    textColor = RoamMateCoral,
                )
            }

            HomeTripStopStatus.Next -> {
                TripStatusBadge(
                    label = "next",
                    backgroundColor = RoamMateLightTeal,
                    textColor = RoamMateTeal,
                )
            }
        }
    }
}

// small status label used on the right side of each trip stop
@Composable
private fun TripStatusBadge(
    label: String,
    backgroundColor: Color,
    textColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// Quick Action Buttons
@Composable
private fun QuickActionButtons(
    onPlanMyTripClick: () -> Unit,
    onExploreClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(
            onClick = onPlanMyTripClick,
            modifier = Modifier
                .weight(1f)
                .height(62.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoamMateTeal,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = "Plan My Trip",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Button(
            onClick = onExploreClick,
            modifier = Modifier
                .weight(1f)
                .height(62.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoamMateCoral,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = "Explore",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// sample Home TripStops：预览用的假数据（三个站点：已完成 / 当前 / 下一步）
private fun sampleHomeTripStops(): List<HomeTripStop> = listOf(
    HomeTripStop(
        time = "09:00",
        title = "Federation Square",
        subtitle = "",
        icon = "A",
        status = HomeTripStopStatus.Done,
    ),
    HomeTripStop(
        time = "10:00",
        title = "Melbourne Museum",
        subtitle = "Now",
        icon = "M",
        status = HomeTripStopStatus.Current,
    ),
    HomeTripStop(
        time = "12:00",
        title = "Lunch nearby",
        subtitle = "Next",
        icon = "L",
        status = HomeTripStopStatus.Next,
    ),
)

// 预览用的宠物假数据。
// TODO: 之后真正宠物系统完成后，预览也可以换成真实 pet 资源。
private fun samplePetStatus(): HomePetStatus = HomePetStatus(
    name = "Buddy",
    description = "Rainy day · been walking a while",
    moodLabel = "Tired",
    imageRes = R.drawable.roammate_wombat,
)

// ===== 12. Preview：Android Studio 预览（不进正式 App）=====
// 预览一：有今日行程 + 显示超时提醒
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreviewWithTrip() {
    RoamMateTheme(dynamicColor = false) {
        HomeScreen(
            userName = "Yufei",
            todayTripStops = sampleHomeTripStops(),
            showLeaveNowReminder = true,
            petStatus = samplePetStatus(),
            onNavigateReminderClick = {},
            onSmartSuggestionClick = {},
            onPetCardClick = {},
            onPlanMyTripClick = {},
            onExploreClick = {},
            onTripClick = {},
            onTabClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// 预览二：没有今日行程（会显示 NoTripTodayCard 空状态）+ 不显示超时提醒
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreviewWithoutTrip() {
    RoamMateTheme(dynamicColor = false) {
        HomeScreen(
            userName = "Yufei",
            todayTripStops = emptyList(),
            showLeaveNowReminder = false,
            petStatus = samplePetStatus(),
            onNavigateReminderClick = {},
            onSmartSuggestionClick = {},
            onPetCardClick = {},
            onPlanMyTripClick = {},
            onExploreClick = {},
            onTripClick = {},
            onTabClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
