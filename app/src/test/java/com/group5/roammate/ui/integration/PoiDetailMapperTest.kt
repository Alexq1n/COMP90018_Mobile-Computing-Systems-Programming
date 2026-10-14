package com.group5.roammate.ui.integration

import com.example.localdatebase.database.PoiData
import com.group5.roammate.poi.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoiDetailMapperTest {
    @Test
    fun enrichedSourceSuppliesActualHoursWebsiteAndPhotoWithoutInventingWeeklyHours() {
        val detail = poi().toAttractionDetail()
        assertEquals("Real museum", detail.name)
        assertEquals("10:30 - 16:45", detail.todayHours?.timeRange)
        assertEquals("https://museum.example/", detail.websiteUrl)
        assertEquals("https://museum.example/photo.jpg", detail.photoUrl)
        assertTrue(detail.weeklyHours.isEmpty())
    }

    @Test
    fun absentSourceMetadataStaysAbsent() {
        val detail = poi().copy(
            operatingHours = OperatingHours("", ""), website = null, photoUrl = null,
        ).toAttractionDetail()
        assertNull(detail.todayHours)
        assertNull(detail.websiteUrl)
        assertNull(detail.photoUrl)
    }

    @Test
    fun sqliteFallbackUsesActualSourceFieldsAndRejectsNonWebLinks() {
        val record = PoiData(
            "park-1", "Real park", 5.0, "PARK", "OUTDOOR", -37.81, 144.96,
            60, "07:00", "20:00", false, "Melbourne", "Park address", "Park description",
        )
        assertEquals("07:00 - 20:00", record.toAttractionDetail().todayHours?.timeRange)
        assertEquals("Outdoor", record.toAttractionDetail().environmentLabel)
        assertNull(poi().copy(website = "javascript:alert(1)").toAttractionDetail().websiteUrl)
    }

    @Test
    fun unknownAttractionDoesNotReceiveSampleHoursOrWebsite() {
        val detail = unavailableAttractionDetail("Unknown attraction")
        assertEquals("Unknown attraction", detail.name)
        assertNull(detail.todayHours)
        assertNull(detail.websiteUrl)
        assertTrue(detail.weeklyHours.isEmpty())
    }

    private fun poi() = POI(
        id = "museum-1", name = "Real museum", category = POICategory.MUSEUM,
        environment = Environment.INDOOR, coordinates = Coordinates(-37.81, 144.96),
        recommendedVisitDuration = 90, operatingHours = OperatingHours("10:30", "16:45"),
        isFiller = false, city = "Melbourne", address = null, description = null,
        website = "https://museum.example/", photoUrl = "https://museum.example/photo.jpg",
    )
}
