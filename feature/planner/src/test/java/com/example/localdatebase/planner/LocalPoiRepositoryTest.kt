package com.example.localdatebase.planner

import com.example.localdatebase.database.PoiData
import com.example.localdatebase.database.PoiDataStore
import com.roammate.logic.models.POICategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalPoiRepositoryTest {
    private val museum = PoiData(
        "museum-1", "Museum", 9.0, "MUSEUM", "INDOOR",
        -37.8136, 144.9631, 90, "09:00", "17:00",
        false, "Melbourne", "1 Test St", "Test museum"
    )
    private val filler = PoiData(
        "cafe-1", "Cafe", 6.0, "CAFE", "INDOOR",
        -37.8137, 144.9632, 30, "07:00", "18:00",
        true, "Melbourne", "2 Test St", "Test cafe"
    )
    private val repository = LocalPoiRepository(FakeStore(listOf(museum, filler)))

    @Test fun mapsAndFiltersLocalPois() {
        val results = repository.getPOIsByCity("Melbourne")
        assertEquals(1, results.size)
        assertEquals(POICategory.MUSEUM, results.single().category)
        assertNotNull(repository.getPOIById("museum-1"))
        assertEquals(1, repository.getFillerPOIs("Melbourne").size)
        assertEquals(2, repository.getPOIsWithinRadius(-37.8136, 144.9631, 100.0, "Melbourne").size)
    }

    @Test fun normalizesAfterMidnightClosingTimeForPlanner() {
        val lateVenue = PoiData(
            "late-1", "Late venue", 7.0, "NIGHTLIFE", "MIXED",
            -37.8136, 144.9631, 150, "17:00", "26:00",
            false, "Melbourne", "3 Test St", "Open after midnight"
        )
        val result = LocalPoiRepository(FakeStore(listOf(lateVenue))).getPOIById("late-1")

        assertEquals("17:00", result?.operatingHours?.openTime)
        assertEquals("23:59", result?.operatingHours?.closeTime)
    }

    private class FakeStore(private val values: List<PoiData>) : PoiDataStore {
        override fun upsertPois(pois: MutableList<PoiData>?) = Unit
        override fun getPoiById(id: String?): PoiData? = values.firstOrNull { it.id == id }
        override fun getPois(category: String?, city: String?, search: String?, limit: Int): MutableList<PoiData> =
            values.filter {
                (category.isNullOrBlank() || it.category.equals(category, true)) &&
                    (city.isNullOrBlank() || it.city.equals(city, true))
            }.take(limit).toMutableList()
        override fun countPois(): Int = values.size
    }
}
