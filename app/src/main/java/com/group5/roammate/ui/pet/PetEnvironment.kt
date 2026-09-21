package com.group5.roammate.ui.pet

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.group5.roammate.pet.OpenMeteoPetWeatherRepository
import com.group5.roammate.pet.PetWeatherCondition
import com.group5.roammate.pet.PetWeatherSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val CITY_LABEL = "Melbourne · selected city"
private const val REFRESH_INTERVAL_MS = 15 * 60_000L
private const val RETRY_INTERVAL_MS = 5 * 60_000L
private const val MAX_LOCATION_AGE_NANOS = 2 * 60_000_000_000L

internal data class PetWeatherTarget(
    val latitude: Double = -37.8136,
    val longitude: Double = 144.9631,
    val label: String = CITY_LABEL,
    val isDevice: Boolean = false,
)

/** Shared foreground weather state; requesting location is always an explicit user action. */
class PetEnvironment internal constructor(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("roammate_pet_environment", Context.MODE_PRIVATE)
    private var snapshot by mutableStateOf(unknownWeather(CITY_LABEL))
    internal var clock by mutableLongStateOf(System.currentTimeMillis())
    internal var target by mutableStateOf(PetWeatherTarget())
    internal var refreshVersion by mutableIntStateOf(0)
    internal var locationVersion by mutableIntStateOf(0)
    internal var locationRequested by mutableStateOf(preferences.getBoolean("device_location", false))
    internal var locationStatus by mutableStateOf<String?>(null)
    internal var weatherStatus by mutableStateOf<String?>("Loading weather")
    internal var onLocationRequest: () -> Unit = {}
    internal var lastDeviceFixNanos: Long = 0L
    private var lastRefreshTapMillis = 0L

    var isRefreshing by mutableStateOf(false)
        internal set

    /** Reading the minute clock invalidates consumers when the same cached reading expires. */
    val weather: PetWeatherSnapshot
        get() {
            clock
            return snapshot
        }
    val locationLabel: String get() = target.label
    val usingDeviceLocation: Boolean get() = target.isDevice
    val status: String?
        get() = listOfNotNull(locationStatus, weatherStatus).joinToString(" · ").ifBlank { null }

    fun requestLocationPermission() = onLocationRequest()

    fun refresh() {
        val now = SystemClock.elapsedRealtime()
        if (lastRefreshTapMillis > 0 && now - lastRefreshTapMillis < 5_000L) return
        lastRefreshTapMillis = now
        refreshVersion += 1
        // Re-check provider availability if the user just enabled location in Android settings.
        locationVersion += 1
    }

    fun useMelbourne() {
        locationRequested = false
        preferences.edit().putBoolean("device_location", false).apply()
        locationStatus = null
        selectTarget(PetWeatherTarget())
        refresh()
    }

    internal fun enableLocation() {
        locationRequested = true
        preferences.edit().putBoolean("device_location", true).apply()
        locationStatus = "Finding your location; using the selected city until a fresh fix arrives"
        locationVersion += 1
    }

    internal fun permissionDenied() {
        useMelbourne()
        locationStatus = "Location not enabled; using Melbourne"
    }

    internal fun selectTarget(next: PetWeatherTarget) {
        if (next == target) return
        target = next
        // Never briefly claim that the previous city's weather describes a new location.
        snapshot = unknownWeather(next.label)
        weatherStatus = "Loading weather"
    }

    internal fun acceptWeather(value: PetWeatherSnapshot) {
        snapshot = value
        clock = System.currentTimeMillis()
        weatherStatus = null
    }

    internal fun weatherFailed() {
        clock = System.currentTimeMillis()
        weatherStatus = if (snapshot.observedAtMillis > 0) {
            "Could not refresh; showing the last reading"
        } else {
            "Weather unavailable; check your connection and retry"
        }
    }
}

private fun unknownWeather(label: String) = PetWeatherSnapshot(
    condition = PetWeatherCondition.Unknown,
    label = "Weather unavailable",
    temperatureC = Double.NaN,
    windSpeedKmh = Double.NaN,
    locationLabel = label,
    source = "Unavailable",
)

@Composable
fun rememberPetEnvironment(): PetEnvironment {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val environment = remember(context.applicationContext) { PetEnvironment(context.applicationContext) }
    val repository = remember { OpenMeteoPetWeatherRepository() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true || hasLocationPermission(context)
        ) environment.enableLocation() else environment.permissionDenied()
    }
    SideEffect {
        environment.onLocationRequest = {
            if (hasLocationPermission(context)) {
                environment.enableLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                )
            }
        }
    }
    DisposableEffect(environment) {
        onDispose { environment.onLocationRequest = {} }
    }

    LaunchedEffect(environment, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch {
                while (true) {
                    environment.clock = System.currentTimeMillis()
                    delay(60_000L)
                }
            }
            snapshotFlow { environment.target to environment.refreshVersion }.collectLatest { (target, _) ->
                while (true) {
                    environment.isRefreshing = true
                    val result = try {
                        repository.fetchCurrent(target.latitude, target.longitude, target.label)
                    } finally {
                        environment.isRefreshing = false
                    }
                    result.onSuccess(environment::acceptWeather)
                        .onFailure { environment.weatherFailed() }
                    delay(if (result.isSuccess) REFRESH_INTERVAL_MS else RETRY_INTERVAL_MS)
                }
            }
        }
    }
    ObservePetLocation(environment)
    return environment
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Uses platform providers, so Google Play Services is not required. No background location. */
@SuppressLint("MissingPermission")
@Composable
private fun ObservePetLocation(environment: PetEnvironment) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember(context.applicationContext) {
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }
    DisposableEffect(manager, lifecycleOwner, environment.locationRequested, environment.locationVersion) {
        var listening = false
        fun accept(location: Location) {
            val age = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
            if (age !in 0..MAX_LOCATION_AGE_NANOS || !location.hasAccuracy() ||
                !location.accuracy.isFinite() || location.accuracy !in 0f..5_000f
            ) return
            if (!location.latitude.isFinite() || !location.longitude.isFinite()) return
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) return
            environment.lastDeviceFixNanos = location.elapsedRealtimeNanos
            environment.locationStatus = null
            val previous = environment.target
            val distance = FloatArray(1)
            Location.distanceBetween(previous.latitude, previous.longitude, location.latitude, location.longitude, distance)
            if (!previous.isDevice || distance[0] >= 1_000f) {
                environment.selectTarget(PetWeatherTarget(location.latitude, location.longitude, "Your location", true))
            }
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (listening && environment.locationRequested &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                ) accept(location)
            }
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) {
                environment.locationStatus = "Location provider paused; using the last weather location"
            }
            @Deprecated("Required for older Android devices")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        fun stop() {
            if (listening) runCatching { manager?.removeUpdates(listener) }
            listening = false
        }
        fun start() {
            if (!environment.locationRequested || manager == null) return
            if (!hasLocationPermission(context)) {
                environment.permissionDenied()
                return
            }
            stop()
            if (environment.target.isDevice &&
                SystemClock.elapsedRealtimeNanos() - environment.lastDeviceFixNanos > MAX_LOCATION_AGE_NANOS
            ) {
                environment.selectTarget(PetWeatherTarget())
                environment.locationStatus = "Waiting for a fresh location; using Melbourne"
            }
            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val providers = buildList {
                add(LocationManager.NETWORK_PROVIDER)
                if (fineGranted) add(LocationManager.GPS_PROVIDER)
            }.filter { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }
            if (providers.isEmpty()) {
                environment.locationStatus = "Enable Android Location to use your position; using the selected weather location"
                return
            }
            for (provider in providers) {
                runCatching {
                    manager.getLastKnownLocation(provider)?.let(::accept)
                    manager.requestLocationUpdates(provider, 60_000L, 500f, listener, Looper.getMainLooper())
                    listening = true
                }.onFailure {
                    environment.locationStatus = "Location unavailable; using the selected weather location"
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose {
            stop()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
