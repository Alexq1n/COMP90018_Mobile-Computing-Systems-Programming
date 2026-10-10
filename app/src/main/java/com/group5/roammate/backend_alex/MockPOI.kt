package com.group5.roammate.backend_alex

import android.content.Context
import com.group5.roammate.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MockPOI(
    private val context: Context
) : PlaceRepository {

    override fun getPlaces(): List<Place> {

        val json = context.resources
            .openRawResource(R.raw.pois_enriched)
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<Place>>() {}.type

        return Gson().fromJson(json, type)
    }
}