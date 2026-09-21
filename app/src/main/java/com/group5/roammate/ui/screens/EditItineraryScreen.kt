package com.group5.roammate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.group5.roammate.ui.theme.RoamMateTheme
import kotlin.math.roundToInt

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
    onSaveClick: (List<TripTimelineStop>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // local order
    var editableStops by remember(stops) {
        mutableStateOf(stops)
    }

    // move row
    fun moveStop(fromIndex: Int, toIndex: Int) {
        val safeToIndex = toIndex.coerceIn(editableStops.indices)
        if (fromIndex == safeToIndex || fromIndex !in editableStops.indices) return

        editableStops = editableStops.toMutableList().apply {
            add(safeToIndex, removeAt(fromIndex))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            SaveItineraryButton(
                onClick = {
                    onSaveClick(editableStops)
                },
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
                text = "Remove, add, or drag to reorder",
                color = RoamMateMutedText,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // TODO: 之后这里接 Zewen 的真实行程站点列表。
            if (editableStops.isEmpty()) {
                EmptyEditItineraryCard()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    editableStops.forEachIndexed { index, stop ->
                        EditItineraryStopCard(
                            stop = stop,
                            onMoveUp = {
                                moveStop(index, index - 1)
                            },
                            onMoveDown = {
                                moveStop(index, index + 1)
                            },
                            onRemoveClick = {
                                editableStops = editableStops.filterNot { currentStop ->
                                    currentStop.sameStopKey(stop)
                                }
                                onRemoveStopClick(stop)
                            },
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
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    // drag state
    val dragThresholdPx = with(LocalDensity.current) { 54.dp.toPx() }
    var dragOffsetY by remember(stop.time, stop.title) {
        mutableStateOf(0f)
    }
    var isDragging by remember(stop.time, stop.title) {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .offset {
                IntOffset(0, dragOffsetY.roundToInt())
            }
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(stop.time, stop.title) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffsetY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        dragOffsetY += dragAmount.y

                        when {
                            dragOffsetY > dragThresholdPx -> {
                                onMoveDown()
                                dragOffsetY = 0f
                            }
                            dragOffsetY < -dragThresholdPx -> {
                                onMoveUp()
                                dragOffsetY = 0f
                            }
                        }
                    },
                )
            },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = if (isDragging) 8.dp else 2.dp,
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

            // drag handle
            Text(
                text = "≡",
                color = RoamMateMutedText,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.width(12.dp))

            RemoveStopButton(onClick = onRemoveClick)
        }
    }
}

// ---- same stop ----
private fun TripTimelineStop.sameStopKey(
    other: TripTimelineStop,
): Boolean {
    return time == other.time && title == other.title
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
            onSaveClick = { _ -> },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
