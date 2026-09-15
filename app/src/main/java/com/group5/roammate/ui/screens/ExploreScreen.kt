package com.group5.roammate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.ui.theme.RoamMateTheme

// on-screen -> code:
//   "Explore" + "Near <area>"        -> ExploreScreen (top texts)
//   filter chips (Indoor/Outdoor..)  -> ExploreFilterRow > ExploreFilterChip
//   map + dots + locate button       -> NearbyMapPreview
//   bottom sheet + list              -> NearbyResultsPanel
//   one place card (tap = open)      -> NearbyPlaceCard
//   "No nearby places"               -> EmptyNearbyCard
//   bottom tabs                      -> RoamMateBottomNavigation

// colors
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)
private val RoamMateMapBackground = Color(0xFFEAF6F3)   // map fill
private val RoamMateMapLine = Color(0xFFFFFFFF)         // map grid lines
private val RoamMateGpsBlue = Color(0xFF168BFF)         // "you are here" dot

// filter tabs (only changes place type; all still nearby)
enum class ExplorePlaceCategory(
    val label: String,
) {
    Indoor("Indoor"),
    Outdoor("Outdoor"),
    Cafes("Cafes"),
    Food("Food"),
}

// data: one nearby place (name, distance, map x/y 0..1, tag)
data class ExplorePlace(
    val name: String,
    val distanceText: String,
    val category: ExplorePlaceCategory,
    val environmentLabel: String,
    val tagText: String,
    val mapX: Float,
    val mapY: Float,
)

// ---- screen ----
@Composable
fun ExploreScreen(
    currentArea: String,
    places: List<ExplorePlace>,
    onPlaceClick: (ExplorePlace) -> Unit,
    onTabClick: (RoamMateMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    // state: selected filter
    var selectedCategory by rememberSaveable {
        mutableStateOf(ExplorePlaceCategory.Indoor)
    }

    // keep only places in the selected filter
    val filteredPlaces = places.filter { place ->
        place.category == selectedCategory
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        // bottom tabs
        bottomBar = {
            RoamMateBottomNavigation(
                selectedTab = RoamMateMainTab.Explore,
                onTabClick = onTabClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .statusBarsPadding(),
        ) {
            Spacer(modifier = Modifier.height(26.dp))

            // "Explore" title
            Text(
                text = "Explore",
                modifier = Modifier.padding(horizontal = 28.dp),
                color = RoamMateTeal,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // "Near <area>" subtitle
            Text(
                text = "Near $currentArea",
                modifier = Modifier.padding(horizontal = 28.dp),
                color = RoamMateMutedText,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // filter chips
            ExploreFilterRow(
                selectedCategory = selectedCategory,
                onCategoryClick = { category ->
                    selectedCategory = category
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // map preview
            NearbyMapPreview(
                places = filteredPlaces,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // results list (bottom sheet)
            NearbyResultsPanel(
                selectedCategory = selectedCategory,
                places = filteredPlaces,
                onPlaceClick = onPlaceClick,
            )
        }
    }
}

// ---- filter row (horizontal scroll of chips) ----
@Composable
private fun ExploreFilterRow(
    selectedCategory: ExplorePlaceCategory,
    onCategoryClick: (ExplorePlaceCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // one chip per category
        ExplorePlaceCategory.entries.forEach { category ->
            ExploreFilterChip(
                text = category.label,
                selected = selectedCategory == category,
                onClick = { onCategoryClick(category) },
            )
        }
    }
}

// ---- one filter chip (tap = select; teal = on) ----
@Composable
private fun ExploreFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) RoamMateTeal else RoamMateLightTeal,
        border = if (selected) {
            null
        } else {
            BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.08f))
        },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = if (selected) Color.White else RoamMateTeal,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

// ---- map preview (grid + dots + locate button) ----
@Composable
private fun NearbyMapPreview(
    places: List<ExplorePlace>,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(292.dp)
            .padding(horizontal = 28.dp),
        shape = RoundedCornerShape(24.dp),
        color = RoamMateMapBackground,
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.08f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // draw map: grid lines + user dot + place dots
            Canvas(modifier = Modifier.fillMaxSize()) {
                val verticalStep = size.width / 5f
                val horizontalStep = size.height / 5f

                // grid lines (fake map)
                for (index in 1..4) {
                    drawLine(
                        color = RoamMateMapLine.copy(alpha = 0.75f),
                        start = Offset(verticalStep * index, 0f),
                        end = Offset(verticalStep * index, size.height),
                        strokeWidth = 2f,
                    )
                    drawLine(
                        color = RoamMateMapLine.copy(alpha = 0.75f),
                        start = Offset(0f, horizontalStep * index),
                        end = Offset(size.width, horizontalStep * index),
                        strokeWidth = 2f,
                    )
                }

                // user location (blue dot)
                // TODO: 这里之后用 Alex/Sitao 的 GPS 坐标作为当前定位点。
                val userLocation = Offset(size.width * 0.50f, size.height * 0.56f)
                drawCircle(
                    color = Color.White,
                    radius = 17f,
                    center = userLocation,
                )
                drawCircle(
                    color = RoamMateGpsBlue,
                    radius = 11f,
                    center = userLocation,
                )

                // place markers (teal dots, positioned by mapX/mapY)
                // TODO: 这里之后用 Yan/Leyan 景点数据里的坐标画 marker。
                places.forEach { place ->
                    drawCircle(
                        color = Color.White,
                        radius = 15f,
                        center = Offset(size.width * place.mapX, size.height * place.mapY),
                    )
                    drawCircle(
                        color = RoamMateTeal,
                        radius = 9f,
                        center = Offset(size.width * place.mapX, size.height * place.mapY),
                    )
                }
            }

            // locate button (bottom-right ⌖)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = "⌖",
                    modifier = Modifier
                        .size(42.dp)
                        .padding(top = 5.dp),
                    color = RoamMateText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---- results panel (rounded bottom sheet + scrolling list) ----
@Composable
private fun NearbyResultsPanel(
    selectedCategory: ExplorePlaceCategory,
    places: List<ExplorePlace>,
    onPlaceClick: (ExplorePlace) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
        ) {
            // header: grab handle + "Nearby <type>"
            item {
                Spacer(modifier = Modifier.height(10.dp))

                // grab handle (little grey bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .height(5.dp)
                            .background(RoamMateFieldBorder, RoundedCornerShape(4.dp)),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // section title
                Text(
                    text = "Nearby ${selectedCategory.label.lowercase()}",
                    color = RoamMateText,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            // list: empty card, or one card per place
            if (places.isEmpty()) {
                item {
                    EmptyNearbyCard()
                }
            } else {
                items(places) { place ->
                    NearbyPlaceCard(
                        place = place,
                        onClick = { onPlaceClick(place) },
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

// ---- one place card (icon · name · distance · tag · >) ; tap = open detail ----
@Composable
private fun NearbyPlaceCard(
    place: ExplorePlace,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // left icon (first letter)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(RoamMateLightTeal, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = place.name.firstOrNull()?.uppercase() ?: "",
                    color = RoamMateTeal,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // name + distance + tag
            Column(modifier = Modifier.weight(1f)) {
                // name
                Text(
                    text = place.name,
                    color = RoamMateText,
                    fontSize = 20.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // distance · environment
                Text(
                    text = "${place.distanceText} · ${place.environmentLabel}",
                    color = RoamMateMutedText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // tag pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RoamMateLightTeal,
                ) {
                    Text(
                        text = place.tagText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = RoamMateTeal,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // chevron
            Text(
                text = ">",
                color = RoamMateTeal,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ---- empty card (no results) ----
@Composable
private fun EmptyNearbyCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = RoamMateLightTeal.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.12f)),
    ) {
        Text(
            text = "No nearby places found",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
            color = RoamMateTeal,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

// preview
@Preview(showBackground = true)
@Composable
private fun ExploreScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        ExploreScreen(
            currentArea = "Carlton",
            places = listOf(
                ExplorePlace(
                    name = "Melbourne Museum",
                    distanceText = "0.8 km",
                    category = ExplorePlaceCategory.Indoor,
                    environmentLabel = "Indoor",
                    tagText = "Good for rain",
                    mapX = 0.26f,
                    mapY = 0.35f,
                ),
                ExplorePlace(
                    name = "State Library Victoria",
                    distanceText = "1.1 km",
                    category = ExplorePlaceCategory.Indoor,
                    environmentLabel = "Indoor",
                    tagText = "Popular nearby",
                    mapX = 0.62f,
                    mapY = 0.28f,
                ),
            ),
            onPlaceClick = {},
            onTabClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}