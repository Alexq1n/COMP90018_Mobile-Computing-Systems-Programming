package com.group5.roammate.sensor

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.util.Locale


class GeocodingRepository(
    context: Context
) {
    private val appContext = context.applicationContext

    suspend fun getAreaName(
        latitude: Double,
        longitude: Double
    ): String {

        if (!Geocoder.isPresent()) {
            return "Current location"
        }

        val geocoder = Geocoder(
            appContext,
            Locale.ENGLISH
        )

        return try {
            val address = if (Build.VERSION.SDK_INT >= 33) {

                suspendCancellableCoroutine<Address?> { continuation ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1
                    ) { addresses ->
                        if (continuation.isActive) {
                            continuation.resume(addresses.firstOrNull())
                        }
                    }
                }

            } else {

                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1
                    )?.firstOrNull()
                }
            }

            address?.subLocality
                ?: address?.locality
                ?: address?.adminArea
                ?: "Current location"

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            "Current location"
        }
    }
}