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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.R
import com.group5.roammate.ui.theme.RoamMateTheme

// The uniform color used on the Profile page.
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateCoral = Color(0xFFFF6F61)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)
private val RoamMateNavGrey = Color(0xFF9AA6A8)

@Composable
fun ProfileScreen(
    userName: String,
    onTravelPreferencesClick: () -> Unit,
    onSavedTripsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onTabClick: (RoamMateMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            RoamMateBottomNavigation(
                selectedTab = RoamMateMainTab.Profile,
                onTabClick = onTabClick,
            )
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

            Text(
                text = "Profile",
                color = RoamMateTeal,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            ProfileHeader(
                userName = userName,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // TODO: 这里之后跳转到共用的 Interests 页面，并保存为用户默认长期偏好。
            ProfileActionRow(
                title = "Travel preferences",
                onClick = onTravelPreferencesClick,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // TODO: 这里之后跳转到 Saved Trips 页面，行程数据由 Yuxiang/Firebase 提供。
            ProfileActionRow(
                title = "Saved trips",
                onClick = onSavedTripsClick,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onEditProfileClick,
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
                    text = "Edit profile",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onLogoutClick,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Log out",
                    color = RoamMateCoral,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // All user avatars should uniformly use the RoamMate mascot
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = RoamMateLightTeal,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.roammate_wombat),
                contentDescription = "RoamMate mascot avatar",
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = userName,
            color = RoamMateText,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun ProfileActionRow(
    title: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = RoamMateText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = ">",
                color = RoamMateTeal,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

enum class RoamMateMainTab(
    val label: String,
    val iconRes: Int,
) {
    Home("Home", R.drawable.nav_home),
    Trip("Trip", R.drawable.nav_trip),
    Explore("Explore", R.drawable.nav_explore),
    Pet("Pet", R.drawable.nav_pet),
    Profile("Profile", R.drawable.nav_profile),
}

@Composable
private fun RoamMateBottomNavigation(
    selectedTab: RoamMateMainTab,
    onTabClick: (RoamMateMainTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(86.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoamMateMainTab.entries.forEach { tab ->
                BottomNavigationItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabClick(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationItem(
    tab: RoamMateMainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColor = if (selected) RoamMateTeal else RoamMateNavGrey

    Column(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The currently selected tab will display a light cyan background color.
        // All bottom navigation ICONS are fixed at 24.dp
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    color = if (selected) RoamMateLightTeal else Color.Transparent,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = tab.iconRes),
                contentDescription = tab.label,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(itemColor),
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = tab.label,
            modifier = Modifier.fillMaxWidth(),
            color = itemColor,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        ProfileScreen(
            userName = "Yufei",
            onTravelPreferencesClick = {},
            onSavedTripsClick = {},
            onEditProfileClick = {},
            onLogoutClick = {},
            onTabClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
