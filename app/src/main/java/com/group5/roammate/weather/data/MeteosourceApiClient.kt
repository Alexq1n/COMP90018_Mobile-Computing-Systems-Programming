package com.group5.roammate.weather.data

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import android.os.Handler
import android.os.Looper

/**
 * This class only knows how to request a Meteosource section and return JSON.
 */
class MeteosourceApiClient(
    private val client: OkHttpClient = OkHttpClient()
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val apiKey = "vnbcxa1687l84gf6lxlwm2saghv1lam998r8zc4v"

    fun request(
        latitude: Double,
        longitude: Double,
        section: String,
        callback: (JSONObject?) -> Unit
    ) {
        val url =
            "https://www.meteosource.com/api/v1/free/point" +
                    "?lat=$latitude" +
                    "&lon=$longitude" +
                    "&sections=$section" +
                    "&timezone=auto" +
                    "&language=en" +
                    "&units=metric" +
                    "&key=$apiKey"

        client.newCall(
            Request.Builder()
                .url(url)
                .build()
        ).enqueue(object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {
                Log.e(
                    "WeatherAPI",
                    "$section request failed",
                    e
                )
                deliver(null, callback)
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(
                            "WeatherAPI",
                            "$section HTTP ${it.code}: ${it.body?.string()}"
                        )
                        deliver(null, callback)
                        return
                    }

                    val json = try {
                        JSONObject(it.body?.string().orEmpty())
                    } catch (e: Exception) {
                        Log.e(
                            "WeatherAPI",
                            "$section JSON parse error",
                            e
                        )
                        null
                    }

                    deliver(json, callback)
                }
            }
        })
    }
    private fun deliver(json: JSONObject?, callback: (JSONObject?) -> Unit) {
        mainHandler.post { callback(json) }
    }
}
