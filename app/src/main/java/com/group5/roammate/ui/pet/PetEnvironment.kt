package com.group5.roammate.ui.pet

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.group5.roammate.pet.PetSharedWeatherState
import com.group5.roammate.pet.PetWeatherSnapshot
import com.group5.roammate.sensor.SensorRepository
import com.group5.roammate.weather.data.WeatherService
import com.group5.roammate.weather.model.CurrentWeather
import kotlinx.coroutines.delay

/** Pet view of the existing shared weather feed; it owns no weather client or GPS listener. */
class PetEnvironment internal constructor() {
    private val readings = PetSharedWeatherState()
    private var snapshot by mutableStateOf(PetWeatherSnapshot.demo().copy(
        locationLabel = "Current location",
        windSpeedKmh = Double.NaN,
        daylightKnown = false,
    ))
    internal var clock by mutableLongStateOf(System.currentTimeMillis())
    internal var onLocationRequest: () -> Unit = {}
    internal var onRefreshRequest: () -> Unit = {}
    private var lastRefreshTapMillis = 0L

    var status by mutableStateOf<String?>("Waiting for shared current weather")
        internal set
    var isRefreshing by mutableStateOf(false)
        internal set

    val weather: PetWeatherSnapshot
        get() {
            clock // Recompose when a cached response expires, even if its fields did not change.
            return snapshot
        }

    fun requestLocationPermission() = onLocationRequest()

    fun refresh() {
        val now = SystemClock.elapsedRealtime()
        if (isRefreshing || (lastRefreshTapMillis > 0L && now - lastRefreshTapMillis < 5_000L)) return
        lastRefreshTapMillis = now
        onRefreshRequest()
    }

    internal fun acceptWeather(value: CurrentWeather) {
        snapshot = readings.accept(value, System.currentTimeMillis())
        clock = System.currentTimeMillis()
        status = if (snapshot.isCurrent) null else "Shared weather is incomplete or out of date"
    }

    internal fun weatherFailed() {
        clock = System.currentTimeMillis()
        status = if (snapshot.isCurrent) "Could not refresh; showing the last shared reading"
            else "Weather unavailable; check location and your connection"
    }
}

@Composable
fun rememberPetEnvironment(
    weatherService: WeatherService,
    sensorRepository: SensorRepository,
): PetEnvironment {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val environment = remember(weatherService, sensorRepository) { PetEnvironment() }
    fun hasPreciseLocation(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Alex's existing LocationSensor requires fine permission; do not start another provider.
        if (hasPreciseLocation()) {
            sensorRepository.startLocationTracking()
            environment.status = "Waiting for shared GPS and weather"
        } else {
            environment.status = "Precise location is needed by the shared weather feed"
        }
    }
    SideEffect {
        environment.onLocationRequest = {
            if (hasPreciseLocation()) {
                sensorRepository.startLocationTracking()
                environment.refresh()
            } else {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ))
            }
        }
        environment.onRefreshRequest = {
            val location = sensorRepository.getCurrentLocation()
            when {
                !hasPreciseLocation() -> environment.status = "Enable precise location to refresh weather"
                location == null -> environment.status = "Waiting for shared GPS; try again after a location arrives"
                else -> {
                    environment.isRefreshing = true
                    // This refresh updates the SAME LiveData that also supplies the Trip summary.
                    weatherService.getCurrentWeather(location.latitude, location.longitude) { weather ->
                        environment.isRefreshing = false
                        if (weather == null) environment.weatherFailed()
                    }
                }
            }
        }
    }
    DisposableEffect(environment, weatherService, lifecycleOwner) {
        val observer = Observer<CurrentWeather> { weather ->
            if (weather != null) environment.acceptWeather(weather)
        }
        weatherService.currentWeather.observe(lifecycleOwner, observer)
        onDispose {
            weatherService.currentWeather.removeObserver(observer)
            environment.onLocationRequest = {}
            environment.onRefreshRequest = {}
        }
    }
    LaunchedEffect(environment, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                environment.clock = System.currentTimeMillis()
                delay(60_000L)
            }
        }
    }
    return environment
}
