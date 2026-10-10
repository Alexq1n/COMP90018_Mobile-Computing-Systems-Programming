package com.example.weathertest.data.geoapify

import android.content.Context
import com.example.weathertest.model.POI
import kotlinx.serialization.json.Json
import android.util.Log

class PoiJsonCache(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = true
    }

    fun save(pois: List<POI>, fileName: String = "pois.json") {
        context.openFileOutput(fileName, Context.MODE_PRIVATE)
            .bufferedWriter()
            .use { it.write(json.encodeToString(pois)) }
        val file = context.getFileStreamPath(fileName)
        Log.d(
            "POI_CACHE",
            "Saved ${pois.size} POIs to ${file.absolutePath}"
        )
    }

    fun load(fileName: String = "pois.json"): List<POI> {
        return try {
            val text = context.openFileInput(fileName)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            json.decodeFromString<List<POI>>(text)
        } catch (_: Exception) {
            emptyList()
        }
    }
}