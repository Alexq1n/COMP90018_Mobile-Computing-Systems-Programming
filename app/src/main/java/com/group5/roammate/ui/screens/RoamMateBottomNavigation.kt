package com.group5.roammate.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.R
import com.group5.roammate.ui.theme.RoamMateTheme

// 底部导航统一使用的颜色，保证每个主页面的导航栏长得一样。
private val RoamMateNavTeal = Color(0xFF008B8F)
private val RoamMateNavLightTeal = Color(0xFFE6F5F3)
private val RoamMateNavGrey = Color(0xFF9AA6A8)

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
fun RoamMateBottomNavigation(
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
    val itemColor = if (selected) RoamMateNavTeal else RoamMateNavGrey

    Column(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 当前选中的 tab 会显示浅青色底色。
        // 所有底部导航图标都固定为 24.dp，避免图片原始大小不一致。
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    color = if (selected) RoamMateNavLightTeal else Color.Transparent,
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
fun RoamMateBottomNavigationPreview() {
    RoamMateTheme {
        RoamMateBottomNavigation(
            selectedTab = RoamMateMainTab.Home,
            onTabClick = {}
        )
    }
}
