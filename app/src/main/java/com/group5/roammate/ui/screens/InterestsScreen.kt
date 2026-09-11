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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.ui.theme.RoamMateTheme

// buttons: Back (top-left) · InterestChip (each tag) · Done (bottom bar)

// colors
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)

// enum: save target (Trip = this trip / Profile = default)
enum class InterestsSaveTarget {
    TripOnly,
    ProfileDefault,
}

// data: one category
private data class InterestCategory(
    val title: String,
    val interests: List<String>,
)

// ---- screen ----
@Composable
fun InterestsScreen(
    saveTarget: InterestsSaveTarget,
    initialSelectedInterests: List<String>,
    onBackClick: () -> Unit,
    onDoneClick: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // state: selected tags
    var selectedInterests by rememberSaveable(saveTarget) {
        mutableStateOf(initialSelectedInterests)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        // bottom: Done button
        bottomBar = {
            DoneButton(
                selectedCount = selectedInterests.size,
                onClick = { onDoneClick(selectedInterests) },
            )
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
            Spacer(modifier = Modifier.height(24.dp))

            // back button
            BackCircleButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(28.dp))

            // title
            Text(
                text = "Interests",
                color = RoamMateTeal,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            // subtitle
            Text(
                text = "Select your interests · pick a few",
                color = RoamMateMutedText,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // loop categories
            interestCategories().forEach { category ->
                InterestCategorySection(
                    category = category,
                    selectedInterests = selectedInterests,
                    // toggle select
                    onInterestClick = { interest ->
                        selectedInterests = if (selectedInterests.contains(interest)) {
                            selectedInterests - interest
                        } else {
                            selectedInterests + interest
                        }
                    },
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ---- back button (top-left circle) ----
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
            fontWeight = FontWeight.Bold,
        )
    }
}

// ---- category block (title + chips) ----
@Composable
private fun InterestCategorySection(
    category: InterestCategory,
    selectedInterests: List<String>,
    onInterestClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // category title
        Text(
            text = category.title,
            color = RoamMateText,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(modifier = Modifier.height(10.dp))

        InterestChipsGrid(
            interests = category.interests,
            selectedInterests = selectedInterests,
            onInterestClick = onInterestClick,
        )
    }
}

// ---- chips grid (2 per row) ----
@Composable
private fun InterestChipsGrid(
    interests: List<String>,
    selectedInterests: List<String>,
    onInterestClick: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        interests.chunked(2).forEach { rowInterests ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowInterests.forEach { interest ->
                    InterestChip(
                        label = interest,
                        selected = selectedInterests.contains(interest),
                        onClick = { onInterestClick(interest) },
                    )
                }
            }
        }
    }
}

// ---- one chip (tap = select, teal = on) ----
@Composable
private fun InterestChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) RoamMateTeal else RoamMateLightTeal.copy(alpha = 0.78f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) RoamMateTeal else RoamMateFieldBorder,
        ),
    ) {
        // label (+ check if on)
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selected) "$label ✓" else label,
                color = if (selected) Color.White else RoamMateTeal,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ---- Done button (bottom bar, shows count) ----
@Composable
private fun DoneButton(
    selectedCount: Int,
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
                text = "Done · $selectedCount selected",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// data: categories + tags
private fun interestCategories(): List<InterestCategory> = listOf(
    InterestCategory(
        title = "Popular",
        interests = listOf("Museums", "Parks", "Food", "Cafes"),
    ),
    InterestCategory(
        title = "Culture",
        interests = listOf("Art", "History", "Theatre", "Photo spots"),
    ),
    InterestCategory(
        title = "Outdoors",
        interests = listOf("Beaches", "Hiking", "Gardens", "Wildlife"),
    ),
    InterestCategory(
        title = "Lifestyle",
        interests = listOf("Shopping", "Nightlife", "Family", "Wineries"),
    ),
)

// preview
@Preview(showBackground = true)
@Composable
private fun InterestsScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        InterestsScreen(
            saveTarget = InterestsSaveTarget.TripOnly,
            initialSelectedInterests = listOf("Museums", "Parks", "Food", "Wildlife"),
            onBackClick = {},
            onDoneClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}