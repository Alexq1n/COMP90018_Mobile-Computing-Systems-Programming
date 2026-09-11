package com.group5.roammate.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.group5.roammate.R
import com.group5.roammate.ui.theme.RoamMateTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// buttons: Back (top-left) · Destination/Date/Start time fields · Interest chips + "+ More"
//          · Transport cards · Search field · Generate (bottom) · dialog ×/Confirm/options

// colors
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateCoral = Color(0xFFFF6F61)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)

// data: request (packed when Generate tapped)
// TODO: 之后交给 Zewen 的规划算法 / Yuxiang 的保存逻辑。
data class PlanMyTripRequest(
    val destination: String,
    val date: String,
    val startTime: String,
    val interests: List<String>,
    val transportMode: String,
)

// data: interest option
private data class PlanInterestOption(
    val label: String,
)

// data: transport option
private data class TransportOption(
    val label: String,
    val iconRes: Int,
)

// data: calendar month
private data class PlanCalendarMonth(
    val title: String,
    val days: List<PlanCalendarDay?>,
)

// data: calendar day
private data class PlanCalendarDay(
    val dayNumber: String,
    val displayDate: String,
    val enabled: Boolean,
)

// ---- screen ----
@Composable
fun PlanMyTripScreen(
    selectedInterests: List<String>,
    onInterestsChanged: (List<String>) -> Unit,
    onBackClick: () -> Unit,
    onMoreInterestsClick: () -> Unit,
    onSearchPlacesClick: () -> Unit,
    onGenerateItineraryClick: (PlanMyTripRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    // state
    var selectedDestination by rememberSaveable { mutableStateOf("Melbourne") }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }
    var selectedTransport by rememberSaveable { mutableStateOf("Transit") }
    var selectedDate by rememberSaveable { mutableStateOf("12 Sep 2026") }
    var selectedStartTime by rememberSaveable { mutableStateOf("10:00") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showStartTimePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        // bottom: Generate button
        bottomBar = {
            GenerateItineraryButton(
                onClick = {
                    onGenerateItineraryClick(
                        PlanMyTripRequest(
                            destination = selectedDestination,
                            date = selectedDate,
                            startTime = selectedStartTime,
                            interests = selectedInterests,
                            transportMode = selectedTransport,
                        ),
                    )
                },
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
                text = "Plan My Trip",
                color = RoamMateTeal,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Destination (tap = city picker)
            SectionTitle(text = "Destination")
            DestinationField(
                destination = selectedDestination,
                onClick = { showCityPicker = true },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Date + Start time (tap = pickers)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionTitle(text = "Date")
                    InfoField(
                        icon = "□",
                        text = selectedDate,
                        onClick = { showDatePicker = true },
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    SectionTitle(text = "Start time")
                    InfoField(
                        icon = "◷",
                        text = selectedStartTime,
                        onClick = { showStartTimePicker = true },
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Interests
            SectionTitle(text = "Interests")
            InterestChipsRow(
                selectedInterests = selectedInterests,
                onInterestClick = { interest ->
                    val updatedInterests = if (selectedInterests.contains(interest)) {
                        selectedInterests - interest
                    } else {
                        selectedInterests + interest
                    }
                    onInterestsChanged(updatedInterests)
                },
                onMoreClick = onMoreInterestsClick,
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Getting around
            SectionTitle(text = "Getting around")
            TransportOptionsRow(
                selectedTransport = selectedTransport,
                onTransportClick = { selectedTransport = it },
            )

            Spacer(modifier = Modifier.height(22.dp))

            // search field
            SectionTitle(text = "Add specific places (optional)")
            SearchPlacesField(onClick = onSearchPlacesClick)

            Spacer(modifier = Modifier.height(34.dp))
        }
    }

    // dialogs: city / date / time
    if (showCityPicker) {
        AustralianCityPickerDialog(
            selectedCity = selectedDestination,
            onCitySelected = { city ->
                selectedDestination = city
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false },
        )
    }

    if (showDatePicker) {
        PlanDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showStartTimePicker) {
        StartTimePickerDialog(
            selectedTime = selectedStartTime,
            onTimeSelected = { time ->
                selectedStartTime = time
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
        )
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

// section title
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = RoamMateText,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.ExtraBold,
    )
}

// ---- destination field (tap = city picker) ----
@Composable
private fun DestinationField(
    destination: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "⌖",
                color = RoamMateMutedText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = destination,
                modifier = Modifier.weight(1f),
                color = RoamMateText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Text(
                text = "▾",
                color = RoamMateTeal,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ---- info field (Date / Start time; tap = picker) ----
@Composable
private fun InfoField(
    icon: String,
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = icon,
                color = RoamMateMutedText,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                modifier = Modifier.weight(1f),
                color = RoamMateText,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Text(
                text = "▾",
                color = RoamMateTeal,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ---- interests row (3 chips + "+ More") ----
@Composable
private fun InterestChipsRow(
    selectedInterests: List<String>,
    onInterestClick: (String) -> Unit,
    onMoreClick: () -> Unit,
) {
    val interests = listOf(
        PlanInterestOption("Museums"),
        PlanInterestOption("Parks"),
        PlanInterestOption("Food"),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        interests.forEach { option ->
            InterestChip(
                label = option.label,
                selected = selectedInterests.contains(option.label),
                onClick = { onInterestClick(option.label) },
            )
        }

        // "+ More" -> interests page
        InterestChip(
            label = "+ More",
            selected = false,
            onClick = onMoreClick,
        )
    }
}

// ---- one chip (tap select / More) ----
@Composable
private fun InterestChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(54.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) RoamMateTeal else RoamMateLightTeal.copy(alpha = 0.65f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) RoamMateTeal else RoamMateTeal.copy(alpha = 0.25f),
        ),
    ) {
        // text only, no leading icon
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else RoamMateTeal,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ---- transport row (Walk / Transit / Drive) ----
@Composable
private fun TransportOptionsRow(
    selectedTransport: String,
    onTransportClick: (String) -> Unit,
) {
    val options = listOf(
        TransportOption("Walk", R.drawable.transport_walk),
        TransportOption("Transit", R.drawable.transport_transit),
        TransportOption("Drive", R.drawable.transport_drive),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        options.forEach { option ->
            TransportCard(
                iconRes = option.iconRes,
                label = option.label,
                selected = selectedTransport == option.label,
                onClick = { onTransportClick(option.label) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ---- transport card (tap select) ----
@Composable
private fun TransportCard(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) RoamMateTeal else Color.White,
        border = BorderStroke(1.dp, if (selected) RoamMateTeal else RoamMateFieldBorder),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // transport icons from drawables
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "$label icon",
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(
                    if (selected) Color.White else RoamMateText,
                ),
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = label,
                color = if (selected) Color.White else RoamMateMutedText,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ---- search field (tap = search page) ----
@Composable
private fun SearchPlacesField(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "⌕",
                color = RoamMateMutedText,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Search places to add",
                color = RoamMateMutedText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ---- Generate button (bottom bar) ----
@Composable
private fun GenerateItineraryButton(
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
                text = "Generate itinerary",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ---- city picker dialog (bottom sheet) ----
@Composable
private fun AustralianCityPickerDialog(
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // state: search text
    var searchText by rememberSaveable { mutableStateOf("") }

    // popular cities
    val popularCities = listOf(
        "Sydney",
        "Melbourne",
        "Brisbane",
        "Perth",
        "Adelaide",
        "Canberra",
    )

    // Australia-only city list (swap for backend API later)
    val australianMajorCities = listOf(
        "Adelaide",
        "Albury",
        "Alice Springs",
        "Ballarat",
        "Bendigo",
        "Brisbane",
        "Broome",
        "Bundaberg",
        "Byron Bay",
        "Cairns",
        "Canberra",
        "Coffs Harbour",
        "Darwin",
        "Geelong",
        "Gold Coast",
        "Hobart",
        "Launceston",
        "Mackay",
        "Melbourne",
        "Mildura",
        "Newcastle",
        "Perth",
        "Port Macquarie",
        "Rockhampton",
        "Sunshine Coast",
        "Sydney",
        "Toowoomba",
        "Townsville",
        "Wagga Wagga",
        "Wollongong",
    )

    // filter by search
    val filteredCities = if (searchText.isBlank()) {
        australianMajorCities
    } else {
        australianMajorCities.filter { city ->
            city.contains(searchText.trim(), ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                ) {
                    // header (× close + title)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "×",
                            modifier = Modifier
                                .size(42.dp)
                                .clickable(onClick = onDismiss),
                            color = RoamMateMutedText,
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            textAlign = TextAlign.Center,
                        )

                        Text(
                            text = "Choose destination",
                            modifier = Modifier.weight(1f),
                            color = RoamMateText,
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.width(42.dp))
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // search box
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        leadingIcon = {
                            Text(
                                text = "⌕",
                                color = RoamMateMutedText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Search Australian city",
                                color = RoamMateMutedText,
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoamMateTeal,
                            unfocusedBorderColor = RoamMateFieldBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = RoamMateTeal,
                        ),
                    )

                    // popular (only when no search)
                    if (searchText.isBlank()) {
                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "Popular cities",
                            color = RoamMateText,
                            fontSize = 19.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        CityGrid(
                            cities = popularCities,
                            selectedCity = selectedCity,
                            onCitySelected = onCitySelected,
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "Australia major cities",
                        color = RoamMateText,
                        fontSize = 19.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // results (or empty)
                    if (filteredCities.isEmpty()) {
                        Text(
                            text = "No matching city",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            color = RoamMateMutedText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        CityGrid(
                            cities = filteredCities,
                            selectedCity = selectedCity,
                            onCitySelected = onCitySelected,
                        )
                    }
                }
            }
        }
    }
}

// ---- date picker dialog ----
@Composable
private fun PlanDatePickerDialog(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // date range: 12 Sep 2026 - 12 Sep 2031
    val monthOptions = rememberPlanCalendarMonths()
    var pendingDate by rememberSaveable { mutableStateOf(selectedDate) }

    BottomPickerDialog(
        title = "Choose date",
        subtitle = "12 Sep 2026 - 12 Sep 2031",
        onDismiss = onDismiss,
    ) {
        Text(
            text = "Timezone: Australia/Melbourne (GMT+10:00)",
            color = RoamMateMutedText,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // scrollable months
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 430.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(monthOptions) { month ->
                CalendarMonthView(
                    month = month,
                    selectedDate = pendingDate,
                    onDateClick = { pendingDate = it },
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // confirm button
        ConfirmPickerButton(
            text = "Confirm",
            onClick = { onDateSelected(pendingDate) },
        )
    }
}

// ---- time picker dialog ----
@Composable
private fun StartTimePickerDialog(
    selectedTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // 24h; left = hour, right = minute
    var pendingHour by rememberSaveable { mutableStateOf(parseHourFromTime(selectedTime)) }
    var pendingMinute by rememberSaveable { mutableStateOf(parseMinuteFromTime(selectedTime)) }
    val hourOptions = remember { (0..23).map { "%02d".format(it) } }
    val minuteOptions = remember { (0..59).map { "%02d".format(it) } }

    BottomPickerDialog(
        title = "Choose start time",
        subtitle = "24-hour time",
        onDismiss = onDismiss,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF7F8FA),
        ) {
            // two wheels: hour / minute
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TimeWheelColumn(
                    title = "Hour",
                    options = hourOptions,
                    selectedOption = pendingHour,
                    onOptionClick = { pendingHour = it },
                    modifier = Modifier.weight(1f),
                )

                TimeWheelColumn(
                    title = "Minute",
                    options = minuteOptions,
                    selectedOption = pendingMinute,
                    onOptionClick = { pendingMinute = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // live preview
        Text(
            text = "Selected: $pendingHour:$pendingMinute",
            modifier = Modifier.fillMaxWidth(),
            color = RoamMateTeal,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(18.dp))

        // confirm button
        ConfirmPickerButton(
            text = "Confirm",
            onClick = { onTimeSelected("$pendingHour:$pendingMinute") },
        )
    }
}

// ---- month view (grid) ----
@Composable
private fun CalendarMonthView(
    month: PlanCalendarMonth,
    selectedDate: String,
    onDateClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = month.title,
            color = RoamMateText,
            fontSize = 26.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(modifier = Modifier.height(14.dp))

        WeekdayHeader()

        Spacer(modifier = Modifier.height(8.dp))

        month.days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        selected = day?.displayDate == selectedDate,
                        onDateClick = onDateClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

// weekday header (Sun..Sat)
@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { weekday ->
            Text(
                text = weekday,
                modifier = Modifier.weight(1f),
                color = RoamMateMutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---- day cell (tap = pick date) ----
@Composable
private fun CalendarDayCell(
    day: PlanCalendarDay?,
    selected: Boolean,
    onDateClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (day != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (selected) RoamMateTeal else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clickable(
                        enabled = day.enabled,
                        onClick = { onDateClick(day.displayDate) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.dayNumber,
                    color = when {
                        selected -> Color.White
                        day.enabled -> RoamMateText
                        else -> RoamMateMutedText.copy(alpha = 0.35f)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---- time column (one wheel: hour or minute) ----
@Composable
private fun TimeWheelColumn(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = RoamMateMutedText,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.height(248.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options) { option ->
                TimeWheelOption(
                    label = option,
                    selected = option == selectedOption,
                    onClick = { onOptionClick(option) },
                )
            }
        }
    }
}

// ---- time option (tap = pick) ----
@Composable
private fun TimeWheelOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) RoamMateTeal else Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) Color.White else RoamMateMutedText,
                fontSize = if (selected) 24.sp else 21.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---- confirm button (dialogs) ----
@Composable
private fun ConfirmPickerButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RoamMateTeal,
            contentColor = Color.White,
        ),
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// ---- bottom sheet dialog (shell: header + content) ----
@Composable
private fun BottomPickerDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                ) {
                    PickerDialogHeader(
                        title = title,
                        subtitle = subtitle,
                        onDismiss = onDismiss,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    content()
                }
            }
        }
    }
}

// ---- dialog header (× close + title + subtitle) ----
@Composable
private fun PickerDialogHeader(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "×",
            modifier = Modifier
                .size(42.dp)
                .clickable(onClick = onDismiss),
            color = RoamMateMutedText,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            textAlign = TextAlign.Center,
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = RoamMateText,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = subtitle,
                color = RoamMateMutedText,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.width(42.dp))
    }
}

// ---- picker row (tap = pick, helper) ----
@Composable
private fun PickerOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) RoamMateLightTeal else Color(0xFFF7F8FA),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) RoamMateTeal.copy(alpha = 0.35f) else Color.Transparent,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = if (selected) RoamMateTeal else RoamMateText,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            if (selected) {
                Text(
                    text = "✓",
                    color = RoamMateTeal,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// ---- city grid (2 per row) ----
@Composable
private fun CityGrid(
    cities: List<String>,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        cities.chunked(2).forEach { rowCities ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowCities.forEach { city ->
                    CityOption(
                        city = city,
                        selected = city == selectedCity,
                        onClick = { onCitySelected(city) },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (rowCities.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ---- city option (tap = pick city) ----
@Composable
private fun CityOption(
    city: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .widthIn(min = 120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) RoamMateLightTeal else Color(0xFFF7F8FA),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) RoamMateTeal.copy(alpha = 0.35f) else Color.Transparent,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = city,
                color = if (selected) RoamMateTeal else RoamMateText,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// remember: calendar months
@Composable
private fun rememberPlanCalendarMonths(): List<PlanCalendarMonth> {
    return remember {
        generatePlanCalendarMonths()
    }
}

// remember: date options
@Composable
private fun rememberPlanDateOptions(): List<String> {
    return remember {
        generatePlanDateOptions()
    }
}

// remember: time options
@Composable
private fun rememberStartTimeOptions(): List<String> {
    return remember {
        generateStartTimeOptions()
    }
}

// build: date list (range)
private fun generatePlanDateOptions(): List<String> {
    val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    val startDate = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 12, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endDate = Calendar.getInstance().apply {
        set(2031, Calendar.SEPTEMBER, 12, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val dates = mutableListOf<String>()
    val currentDate = startDate.clone() as Calendar
    while (!currentDate.after(endDate)) {
        dates.add(displayFormat.format(currentDate.time))
        currentDate.add(Calendar.DATE, 1)
    }
    return dates
}

// build: time list (00:00..23:59)
private fun generateStartTimeOptions(): List<String> {
    val times = mutableListOf<String>()
    for (hour in 0..23) {
        for (minute in 0..59) {
            times.add("%02d:%02d".format(hour, minute))
        }
    }
    return times
}

// build: calendar months (grid data)
private fun generatePlanCalendarMonths(): List<PlanCalendarMonth> {
    val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)

    val startDate = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 12, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endDate = Calendar.getInstance().apply {
        set(2031, Calendar.SEPTEMBER, 12, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val currentMonth = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val finalMonth = Calendar.getInstance().apply {
        set(2031, Calendar.SEPTEMBER, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val months = mutableListOf<PlanCalendarMonth>()

    while (!currentMonth.after(finalMonth)) {
        val monthDays = mutableListOf<PlanCalendarDay?>()
        val blanksBeforeMonth = currentMonth.get(Calendar.DAY_OF_WEEK) - 1
        repeat(blanksBeforeMonth) {
            monthDays.add(null)
        }

        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (dayNumber in 1..daysInMonth) {
            val dayCalendar = currentMonth.clone() as Calendar
            dayCalendar.set(Calendar.DAY_OF_MONTH, dayNumber)

            val enabled = !dayCalendar.before(startDate) && !dayCalendar.after(endDate)
            monthDays.add(
                PlanCalendarDay(
                    dayNumber = dayNumber.toString(),
                    displayDate = displayFormat.format(dayCalendar.time),
                    enabled = enabled,
                ),
            )
        }

        while (monthDays.size % 7 != 0) {
            monthDays.add(null)
        }

        months.add(
            PlanCalendarMonth(
                title = monthFormat.format(currentMonth.time),
                days = monthDays,
            ),
        )

        currentMonth.add(Calendar.MONTH, 1)
    }

    return months
}

// parse: hour from "HH:mm"
private fun parseHourFromTime(time: String): String {
    return time.substringBefore(":").padStart(2, '0').takeLast(2)
}

// parse: minute from "HH:mm"
private fun parseMinuteFromTime(time: String): String {
    return time.substringAfter(":", "00").padStart(2, '0').takeLast(2)
}

// preview
@Preview(showBackground = true)
@Composable
private fun PlanMyTripScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        var previewInterests by rememberSaveable {
            mutableStateOf(listOf("Museums", "Parks", "Food"))
        }

        PlanMyTripScreen(
            selectedInterests = previewInterests,
            onInterestsChanged = { previewInterests = it },
            onBackClick = {},
            onMoreInterestsClick = {},
            onSearchPlacesClick = {},
            onGenerateItineraryClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}