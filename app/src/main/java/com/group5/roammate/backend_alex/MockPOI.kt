package com.group5.roammate.backend_alex

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MockPOI(
    private val context: Context
) : PlaceRepository {

    override fun getPlaces(): List<Place> {
        return try {
            // Read from assets/pois.json (same as MockPoiRepository for itinerary generation)
            val json = context.assets
                .open("pois.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<Place>>() {}.type

            Gson().fromJson(json, type)
        } catch (e: Exception) {
            android.util.Log.e("MockPOI", "Failed to load from assets/pois.json: ${e.message}", e)
            // Return empty list if file not found or parse error
            emptyList()
        }
    }
}