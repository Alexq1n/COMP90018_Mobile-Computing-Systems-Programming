package com.group5.roammate.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.ui.theme.RoamMateTheme
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// colors
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)
private val RoamMateHeroBackground = Color(0xFFCFECEF)

// 景点详情页需要的数据。距离后面由 sensor/GPS 计算；营业时间/官网由 Leyan 提供。
data class AttractionDetail(
    val name: String,
    val distanceText: String,
    val environmentLabel: String,
    val weatherTag: String?,
    val todayHours: AttractionOpeningHours?,
    val weeklyHours: List<AttractionOpeningHours>,
    val websiteUrl: String?,
    val photoUrl: String? = null,
    val imageSymbol: String = "M",
)

// Leyan 如果没有营业时间，就让 todayHours = null，UI 会显示 unavailable。
data class AttractionOpeningHours(
    val dayLabel: String,
    val timeRange: String,
)

@Composable
fun AttractionDetailScreen(
    attraction: AttractionDetail,
    onBackClick: () -> Unit,
    onAddToTripClick: (AttractionDetail) -> Unit,
    onNavigateClick: (AttractionDetail) -> Unit,
    onWebsiteClick: (AttractionDetail) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            AttractionDetailActionBar(
                onAddToTripClick = { onAddToTripClick(attraction) },
                onNavigateClick = { onNavigateClick(attraction) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            AttractionHero(
                attraction = attraction,
                onBackClick = onBackClick,
            )

            AttractionBody(
                attraction = attraction,
                onWebsiteClick = { onWebsiteClick(attraction) },
            )
        }
    }
}

// ---- top image area ----
@Composable
private fun AttractionHero(
    attraction: AttractionDetail,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(344.dp)
            .background(RoamMateHeroBackground)
            .statusBarsPadding(),
    ) {
        // photo
        AttractionHeroPhoto(
            photoUrl = attraction.photoUrl,
            fallbackText = attraction.imageSymbol,
            modifier = Modifier.fillMaxSize(),
        )

        // Draw controls after the photo so they remain visible and clickable.
        BackCircleButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.dp, top = 24.dp),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.90f),
        ) {
            Text(
                text = attraction.name,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = RoamMateText,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

// ---- hero photo (URL image or fallback) ----
@Composable
private fun AttractionHeroPhoto(
    photoUrl: String?,
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    var photoBitmap by remember(photoUrl) {
        mutableStateOf<Bitmap?>(null)
    }
    var hasPhotoError by remember(photoUrl) {
        mutableStateOf(false)
    }

    // load photo
    LaunchedEffect(photoUrl) {
        photoBitmap = null
        hasPhotoError = false

        if (photoUrl != null) {
            val loadedBitmap = runCatching {
                withContext(Dispatchers.IO) {
                    URL(photoUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            }.getOrNull()

            photoBitmap = loadedBitmap
            hasPhotoError = loadedBitmap == null
        }
    }

    val loadedPhoto = photoBitmap

    if (loadedPhoto != null) {
        Image(
            bitmap = loadedPhoto.asImageBitmap(),
            contentDescription = "Attraction photo",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        AttractionHeroPhotoFallback(
            fallbackText = fallbackText,
            isLoading = photoUrl != null && !hasPhotoError,
            modifier = modifier,
        )
    }
}

// ---- photo fallback ----
@Composable
private fun AttractionHeroPhotoFallback(
    fallbackText: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(RoamMateHeroBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = fallbackText,
                color = RoamMateText.copy(alpha = 0.62f),
                fontSize = 88.sp,
                lineHeight = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isLoading) "Loading photo" else "Photo unavailable",
                color = RoamMateMutedText,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ---- back button ----
@Composable
private fun BackCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(50.dp)
            .background(Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "<",
            color = RoamMateTeal,
            fontSize = 30.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

// ---- page body ----
@Composable
private fun AttractionBody(
    attraction: AttractionDetail,
    onWebsiteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Text(
            text = attraction.name,
            color = RoamMateText,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "⌖",
                color = RoamMateTeal,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                // TODO: 之后这里接 Alex/Sitao sensor/GPS 算出的当前位置距离。
                text = "${attraction.distanceText} away",
                color = RoamMateMutedText,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        AttractionTagRow(attraction = attraction)

        Spacer(modifier = Modifier.height(24.dp))

        DividerLine()

        AttractionOpeningHoursRow(attraction = attraction)

        DividerLine()

        OfficialWebsiteRow(
            websiteUrl = attraction.websiteUrl,
            onClick = onWebsiteClick,
        )

        Spacer(modifier = Modifier.height(36.dp))
    }
}

// ---- tags ----
@Composable
private fun AttractionTagRow(
    attraction: AttractionDetail,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SmallInfoChip(text = attraction.environmentLabel)

        attraction.weatherTag?.let { tag ->
            SmallInfoChip(text = tag)
        }
    }
}

@Composable
private fun SmallInfoChip(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = RoamMateLightTeal,
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.08f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = RoamMateTeal,
            fontSize = 15.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

// ---- opening hours ----
@Composable
private fun AttractionOpeningHoursRow(
    attraction: AttractionDetail,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val todayHours = attraction.todayHours

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = todayHours != null) {
                expanded = !expanded
            }
            .padding(vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoIcon(text = "○")

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = if (todayHours == null) {
                    "Opening hours unavailable"
                } else {
                    "Open today"
                },
                modifier = Modifier.weight(1f),
                color = if (todayHours == null) RoamMateMutedText else RoamMateText,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            if (todayHours != null) {
                Text(
                    text = "${todayHours.timeRange} ${if (expanded) "^" else "v"}",
                    color = RoamMateTeal,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.End,
                )
            }
        }

        if (expanded && attraction.weeklyHours.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                attraction.weeklyHours.forEach { hours ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = hours.dayLabel,
                            modifier = Modifier.weight(1f),
                            color = RoamMateMutedText,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Text(
                            text = hours.timeRange,
                            color = RoamMateText,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

// ---- website ----
@Composable
private fun OfficialWebsiteRow(
    websiteUrl: String?,
    onClick: () -> Unit,
) {
    val enabled = websiteUrl != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfoIcon(text = "◎")

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = if (enabled) {
                "Visit official website"
            } else {
                "Official website unavailable"
            },
            modifier = Modifier.weight(1f),
            color = if (enabled) RoamMateText else RoamMateMutedText,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        if (enabled) {
            Text(
                text = "↗",
                color = RoamMateTeal,
                fontSize = 24.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun InfoIcon(
    text: String,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(RoamMateLightTeal, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = RoamMateTeal,
            fontSize = 18.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RoamMateFieldBorder),
    )
}

// ---- bottom actions ----
@Composable
private fun AttractionDetailActionBar(
    onAddToTripClick: () -> Unit,
    onNavigateClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedButton(
                onClick = onAddToTripClick,
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
                    text = "Add to trip",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = onNavigateClick,
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
                    text = "Navigate",
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
private fun AttractionDetailScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        AttractionDetailScreen(
            attraction = AttractionDetail(
                name = "Melbourne Museum",
                distanceText = "0.8 km",
                environmentLabel = "Indoor",
                weatherTag = "Good for rain",
                todayHours = AttractionOpeningHours("Today", "10:00 am - 5:00 pm"),
                weeklyHours = listOf(
                    AttractionOpeningHours("Mon", "10:00 am - 5:00 pm"),
                    AttractionOpeningHours("Tue", "10:00 am - 5:00 pm"),
                    AttractionOpeningHours("Wed", "10:00 am - 5:00 pm"),
                ),
                websiteUrl = "https://museumsvictoria.com.au/melbournemuseum/",
                photoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2b/Melbourne_Museum_exterior.jpg/1280px-Melbourne_Museum_exterior.jpg",
                imageSymbol = "M",
            ),
            onBackClick = {},
            onAddToTripClick = {},
            onNavigateClick = {},
            onWebsiteClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
