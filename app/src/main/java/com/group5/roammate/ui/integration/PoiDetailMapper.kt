package com.group5.roammate.ui.integration

import com.example.localdatebase.database.PoiData
import com.group5.roammate.poi.model.POI
import com.group5.roammate.ui.screens.AttractionDetail
import com.group5.roammate.ui.screens.AttractionOpeningHours

internal fun POI.toAttractionDetail(): AttractionDetail = detail(
    name, environment.name, operatingHours.openTime, operatingHours.closeTime, photoUrl, website,
)

internal fun PoiData.toAttractionDetail(): AttractionDetail = detail(
    name, environment, openTime, closeTime, null, null,
)

internal fun unavailableAttractionDetail(name: String): AttractionDetail =
    detail(name, "Unavailable", "", "", null, null)

private fun detail(
    name: String,
    environment: String,
    openTime: String,
    closeTime: String,
    photo: String?,
    website: String?,
): AttractionDetail = AttractionDetail(
    name = name,
    distanceText = "",
    environmentLabel = environment.lowercase().replaceFirstChar { it.uppercase() },
    weatherTag = null,
    todayHours = if (openTime.isNotBlank() && closeTime.isNotBlank()) {
        AttractionOpeningHours("Today", "$openTime - $closeTime")
    } else null,
    weeklyHours = emptyList(),
    websiteUrl = website.webUrlOrNull(),
    photoUrl = photo.webUrlOrNull(),
    imageSymbol = name.firstOrNull()?.uppercase() ?: "",
)

private fun String?.webUrlOrNull(): String? = this?.trim()?.takeIf {
    it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
}
