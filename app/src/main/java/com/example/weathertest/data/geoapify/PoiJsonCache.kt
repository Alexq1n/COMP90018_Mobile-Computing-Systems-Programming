package com.example.weathertest.data.geoapify

import android.content.Context
import com.example.weathertest.model.POI
import kotlinx.serialization.json.Json

class PoiJsonCache(private val context: Context) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun save(pois: List<POI>, fileName: String = "pois.json") {
        context.openFileOutput(fileName, Context.MODE_PRIVATE)
            .bufferedWriter()
            .use { it.write(json.encodeToString(pois)) }
    }

    fun load(fileName: String = "pois.json"): List<POI> {
        return try {
            val text = context.openFileInput(fileName)
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString<List<POI>>(text)
        } catch (_: Exception) {
            emptyList()
        }
    }
}