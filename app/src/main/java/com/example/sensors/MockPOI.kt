package com.example.sensors

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MockPOI(
    private val context: Context
) {

    fun getPlaces(): List<Place> {

        val json = context.resources
            .openRawResource(R.raw.pois)
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<Place>>() {}.type

        return Gson().fromJson(
            json,
            type
        )
    }
}