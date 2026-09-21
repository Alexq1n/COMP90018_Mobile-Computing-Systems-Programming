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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group5.roammate.ui.theme.RoamMateTheme

// Add a stop page uniform color
private val RoamMateTeal = Color(0xFF008B8F)
private val RoamMateLightTeal = Color(0xFFE6F5F3)
private val RoamMateText = Color(0xFF17212B)
private val RoamMateMutedText = Color(0xFF8A949E)
private val RoamMateFieldBorder = Color(0xFFE3E8EF)
private val RoamMateSearchBackground = Color(0xFFF1F4F5)
private val RoamMateGreenBackground = Color(0xFFE5F8EC)
private val RoamMateGreen = Color(0xFF22A866)

// data: one searchable place result
data class AddStopPlace(
    val name: String,
    val distanceText: String,
    val environmentType: String,
)

@Composable
fun AddStopScreen(
    currentCity: String,
    places: List<AddStopPlace>,
    onBackClick: () -> Unit,
    onSearchConfirmClick: (String) -> Unit,
    onAddPlaceClick: (AddStopPlace) -> Unit,
    isSearching: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var confirmedSearchText by rememberSaveable { mutableStateOf("") }

    val filteredPlaces = if (confirmedSearchText.isBlank()) {
        places
    } else {
        places.filter { place ->
            place.name.contains(confirmedSearchText, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0.dp),
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
                text = "Add a stop",
                color = RoamMateTeal,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(20.dp))

            AddStopSearchField(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onClearClick = {
                    searchText = ""
                    confirmedSearchText = ""
                },
                onSearchConfirmClick = {
                    val query = searchText.trim()
                    confirmedSearchText = query

                    // Only this confirm click sends the query to the future search logic.
                    // Typing in the field should not call Alex's search.
                    if (query.isNotBlank() && !isSearching) {
                        onSearchConfirmClick(query)
                    }
                },
                isSearching = isSearching,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (confirmedSearchText.isBlank()) {
                    "Popular in $currentCity"
                } else {
                    "Search results"
                },
                color = RoamMateText,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // TODO: 之后这里接 Alex 的搜索结果，Yan 的景点/室内室外数据，Sitao 的距离定位数据。
            if (isSearching) {
                SearchLoadingCard()
            } else if (filteredPlaces.isEmpty()) {
                NotFoundCard(searchText = confirmedSearchText)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    filteredPlaces.forEach { place ->
                        AddStopResultCard(
                            place = place,
                            onAddClick = { onAddPlaceClick(place) },
                        )
                    }
                }
            }

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

// ---- search field ----
@Composable
private fun AddStopSearchField(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearClick: () -> Unit,
    onSearchConfirmClick: () -> Unit,
    isSearching: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                if (!isSearching) {
                    onSearchTextChange(it)
                }
            },
            modifier = Modifier
                .weight(1f)
                .height(62.dp),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            leadingIcon = {
                Text(
                    text = "⌕",
                    color = RoamMateMutedText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            trailingIcon = {
                if (searchText.isNotBlank()) {
                    Text(
                        text = "×",
                        modifier = Modifier.clickable(
                            enabled = !isSearching,
                            onClick = onClearClick,
                        ),
                        color = RoamMateMutedText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            placeholder = {
                Text(
                    text = "Search places to add",
                    color = RoamMateMutedText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = RoamMateText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = RoamMateSearchBackground,
                unfocusedContainerColor = RoamMateSearchBackground,
                cursorColor = RoamMateTeal,
            ),
        )

        SearchConfirmButton(
            enabled = searchText.isNotBlank() && !isSearching,
            isLoading = isSearching,
            onClick = onSearchConfirmClick,
        )
    }
}

// ---- search confirm button ----
@Composable
private fun SearchConfirmButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(
                color = if (enabled || isLoading) RoamMateTeal else RoamMateFieldBorder,
                shape = CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            // loading spinner
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            // search icon
            Text(
                text = "⌕",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---- one result row ----
@Composable
private fun AddStopResultCard(
    place: AddStopPlace,
    onAddClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RoamMateFieldBorder),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaceInitialBadge(placeName = place.name)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    color = RoamMateText,
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${place.distanceText} · ${place.environmentType}",
                    color = RoamMateMutedText,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            AddPlaceButton(onClick = onAddClick)
        }
    }
}

// ---- small place badge ----
@Composable
private fun PlaceInitialBadge(
    placeName: String,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(RoamMateLightTeal, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = placeName.firstOrNull()?.uppercase() ?: "",
            color = RoamMateTeal,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// ---- plus button ----
@Composable
private fun AddPlaceButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(RoamMateGreenBackground, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            color = RoamMateGreen,
            fontSize = 25.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

// ---- search loading ----
@Composable
private fun SearchLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = RoamMateLightTeal.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // loading spinner
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = RoamMateTeal,
                strokeWidth = 2.dp,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // loading text
            Text(
                text = "Searching places",
                color = RoamMateTeal,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ---- no result ----
@Composable
private fun NotFoundCard(
    searchText: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = RoamMateLightTeal.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, RoamMateTeal.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Not found",
                color = RoamMateTeal,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "No place matched \"$searchText\"",
                color = RoamMateMutedText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddStopScreenPreview() {
    RoamMateTheme(dynamicColor = false) {
        AddStopScreen(
            currentCity = "Melbourne",
            places = listOf(
                AddStopPlace("Melbourne Museum", "1.8 km", "Indoor"),
                AddStopPlace("National Gallery of Victoria", "1.2 km", "Indoor"),
                AddStopPlace("Royal Botanic Gardens", "2.4 km", "Outdoor"),
            ),
            onBackClick = {},
            onSearchConfirmClick = {},
            onAddPlaceClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddStopLoadingPreview() {
    RoamMateTheme(dynamicColor = false) {
        AddStopScreen(
            currentCity = "Melbourne",
            places = listOf(
                AddStopPlace("Melbourne Museum", "1.8 km", "Indoor"),
                AddStopPlace("National Gallery of Victoria", "1.2 km", "Indoor"),
                AddStopPlace("Royal Botanic Gardens", "2.4 km", "Outdoor"),
            ),
            onBackClick = {},
            onSearchConfirmClick = {},
            onAddPlaceClick = {},
            isSearching = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
