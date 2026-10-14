package com.group5.roammate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.localdatebase.cloud.CloudResultCallback
import com.example.localdatebase.cloud.CloudUserSession
import com.example.localdatebase.cloud.FirebaseCloudUserRepository
import com.example.localdatebase.cloud.UserPreferences
import com.example.localdatebase.planner.FirebasePlannerRepository
import com.example.localdatebase.planner.PlannerResultCallback
import com.example.localdatebase.planner.SavedTripSummary as CloudSavedTripSummary
import com.group5.roammate.backend.POISearchService
import com.group5.roammate.backend.PlaceRepository

import com.group5.roammate.ui.pet.rememberPetEnvironment
import com.group5.roammate.pet.PetPreferences
import com.group5.roammate.pet.PetStateEngine
import com.group5.roammate.pet.PetTripContext
import com.group5.roammate.pet.PetWeatherSnapshot
import com.group5.roammate.sensor.GeocodingRepository
import com.group5.roammate.ui.screens.AdjustChangeTone
import com.group5.roammate.ui.screens.AdjustItineraryPlan
import com.group5.roammate.ui.screens.AdjustItineraryScreen
import com.group5.roammate.ui.screens.AdjustItineraryStop
import com.group5.roammate.ui.screens.AdjustRemovedStop
import com.group5.roammate.ui.screens.AddStopPlace
import com.group5.roammate.ui.screens.AddStopScreen
import com.group5.roammate.ui.screens.AttractionDetail
import com.group5.roammate.ui.screens.AttractionDetailScreen
import com.group5.roammate.ui.screens.AttractionOpeningHours
import com.group5.roammate.ui.screens.CreateAccountScreen
import com.group5.roammate.ui.screens.EditProfileScreen
import com.group5.roammate.ui.screens.EditItineraryScreen
import com.group5.roammate.ui.screens.ExplorePlace
import com.group5.roammate.ui.screens.ExplorePlaceCategory
import com.group5.roammate.ui.screens.ExploreScreen
import com.group5.roammate.ui.screens.HomeLeaveNowReminder
import com.group5.roammate.ui.screens.HomePetStatus
import com.group5.roammate.ui.screens.HomeScreen
import com.group5.roammate.ui.screens.HomeSmartSuggestion
import com.group5.roammate.ui.screens.HomeTripStop
import com.group5.roammate.ui.screens.HomeTripStopStatus
import com.group5.roammate.ui.screens.InterestsSaveTarget
import com.group5.roammate.ui.screens.InterestsScreen
import com.group5.roammate.ui.screens.LoginScreen
import com.group5.roammate.ui.screens.PlanMyTripScreen
import com.group5.roammate.ui.screens.PlanMyTripRequest
import com.group5.roammate.ui.screens.ProfileScreen
import com.group5.roammate.ui.screens.RoamMateMainTab
import com.group5.roammate.ui.screens.SavedTrip
import com.group5.roammate.ui.screens.SavedTripsScreen
import com.group5.roammate.ui.screens.TripScreen
import com.group5.roammate.ui.screens.TripStopStatus
import com.group5.roammate.ui.screens.TripDayPlan
import com.group5.roammate.ui.screens.TripSummary
import com.group5.roammate.ui.screens.TripTimelineStop
import com.group5.roammate.ui.screens.TripWeatherSummary
import com.group5.roammate.ui.sensor.SensorTestScreen
import com.group5.roammate.ui.pet.CompanionsScreen
import com.group5.roammate.ui.pet.PetCameraScreen
import com.group5.roammate.ui.theme.RoamMateTheme

import com.group5.roammate.weather.data.WeatherService
import com.group5.roammate.weather.monitor.CurrentWeatherMonitor
import com.group5.roammate.weather.monitor.WeatherForecastMonitor
import com.group5.roammate.weather.context.WeatherContextDetector
import com.group5.roammate.weather.context.WeatherContextEvent
import com.group5.roammate.weather.data.WeatherRepository
import com.group5.roammate.weather.model.DailyWeatherForecast
import com.group5.roammate.poi.data.geoapify.GeoapifyRepository
import com.group5.roammate.poi.data.geoapify.PoiDatabaseStore

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.group5.roammate.backend.DatabasePOI
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

// for demo only
import kotlinx.coroutines.delay
import com.group5.roammate.weather.model.CurrentWeather
import com.group5.roammate.weather.model.HourlyWeather
import com.group5.roammate.weather.model.WeatherStatus
import java.time.LocalDateTime


// [NEW] Sensor module
import com.group5.roammate.sensor.RoamMateApp
import com.group5.roammate.sensor.SensorRepository
import com.group5.roammate.sensor.SensorService

/*
UI Integration Map
Full contract: ui/RoamMate_UI_API_Guide.md
Search "function:" in this file to find each teammate's integration point.

Yuxiang: Firebase auth, user profile, saved trips.
Alex: sensor GPS, shake events, fuzzy search trigger.
Leyan: attraction list, attraction detail, indoor/outdoor, website, hours. weather summary and weather availability.
Zewen: itinerary generation, edit/reorder/add stop, weather adjustment.
Xiajie: pet state and pet screen.
Yufei: UI wiring, navigation, loading/error display.
Keep each owner in a small function below, then connect the screen callbacks to those functions.
*/






class MainActivity : ComponentActivity() {
    // POI Database
    private val geoapifyPoiRepository by lazy {
        GeoapifyRepository(
            poiDatabaseStore = PoiDatabaseStore(applicationContext)
        )
    }

// ==================== Leyan Weather ====================

    private val weatherRepository by lazy {
        WeatherRepository()
    }

    private val weatherContextDetector by lazy {
        WeatherContextDetector()
    }

    private val weatherService by lazy {
        WeatherService(
            weatherRepository = weatherRepository
        )
    }

    private val currentWeatherMonitor by lazy {
        CurrentWeatherMonitor(
            weatherService = weatherService,
            getCurrentLocation = {
                sensorRepository
                    .getCurrentLocation()
                    ?.let {
                        it.latitude to it.longitude
                    }
            }
        )
    }

    private val weatherForecastMonitor by lazy {
        WeatherForecastMonitor(
            weatherRepository = weatherRepository,
            contextDetector = weatherContextDetector,
            getCurrentLocation = {
                sensorRepository
                    .getCurrentLocation()
                    ?.let { location ->
                        location.latitude to location.longitude
                    }
            }
        )
    }

    private var currentWeatherMonitorStarted = false

    private val currentWeatherSummaryState =
        mutableStateOf(
            TripWeatherSummary(
                temperature = "—",
                condition = "Weather unavailable",
            )
        )

    private val weatherSuggestionState =
        mutableStateOf<HomeSmartSuggestion?>(null)

    private var latestPlannedWeather:
            List<DailyWeatherForecast> = emptyList()

    private val planDateFormatter =
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy",
            Locale.ENGLISH
        )

    private val cloudUserRepository by lazy {
        FirebaseCloudUserRepository()
    }

    private val plannerRepository by lazy {
        FirebasePlannerRepository()
    }

    private val geocodingRepository by lazy {
        GeocodingRepository(applicationContext)
    }

    // Use the ONE repository created by RoamMateApp.
    private val sensorRepository: SensorRepository
        get() = (application as RoamMateApp).sensorRepository

    // Bound service for Step Counter.
    private var sensorService: SensorService? = null
    private var bindingRequested = false

    private val sensorConnection = object : ServiceConnection {

        override fun onServiceConnected(
            name: ComponentName,
            binder: IBinder
        ) {
            sensorService =
                (binder as SensorService.LocalBinder).getService()

            // Binding is for accessing the service.
            // The Foreground Service starts Step Counter itself.
            android.util.Log.d(
                "RoamMateSensor",
                "SensorService connected"
            )
        }

        override fun onServiceDisconnected(name: ComponentName) {
            sensorService = null
        }
    }

    // Request Android 10+ step recognition permission.
    private val activityPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (
                granted &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            ) {
                startBackgroundStepTracking()
                sensorRepository.startStepDetection()
            }
        }

    // Request GPS permissions.
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.values.any { it }) {
                sensorRepository.startLocationTracking()
            }
        }

    private fun hasActivityRecognitionPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < 29 ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    // Start persistent background step tracking.
    private fun startBackgroundStepTracking() {
        if (!hasActivityRecognitionPermission()) return

        val intent = Intent(this, SensorService::class.java).apply {
            action = SensorService.ACTION_START
        }

        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (e: Exception) {
            android.util.Log.e(
                "RoamMateSensor",
                "Unable to start background step tracking",
                e
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ==================== Fetch POIs and save to database ====================

        lifecycleScope.launch {
            try {
                val pois = geoapifyPoiRepository.fetchAllCategories(
                    city = "Melbourne",
                    latitude = -37.8136,
                    longitude = 144.9631,
                    enrichWithDetails = false
                )

                android.util.Log.d(
                    "POI_DATABASE",
                    "Fetched and saved ${pois.size} POIs"
                )

            } catch (e: Exception) {
                android.util.Log.e(
                    "POI_DATABASE",
                    "Failed to fetch/save POIs",
                    e
                )
            }
        }


        // Weather context-awareness wiring.
        // Current weather is still refreshed every 10 minutes. The same result
        // is also used as the baseline for sudden-rain detection.
        currentWeatherMonitor.onWeatherUpdated = { weather ->
            weatherContextDetector
                .detectCurrentWeatherChange(weather)
                ?.let(::uiHandleWeatherContextEvent)

            // Start forecast monitoring only after current weather exists, so
            // upcoming-rain detection has a valid current-weather context.
            weatherForecastMonitor.start()
        }

        weatherForecastMonitor.onContextEvent = { event ->
            uiHandleWeatherContextEvent(event)
        }

        // ==================== Weather Demo ====================
        startWeatherDemoScenario()

        // UI observes the latest current weather through WeatherService.
        weatherService.currentWeather.observe(this) { weather ->

            android.util.Log.d(
                "ROAMMATE_WEATHER",
                "Current weather received: " +
                        "status=${weather.status}, " +
                        "temperature=${weather.temperature}"
            )

            val temperatureText =
                weather.temperature?.let {
                    "${it.roundToInt()}°C"
                } ?: "—"

            val conditionText =
                weather.status
                    ?.name
                    ?.lowercase()
                    ?.replace("_", " ")
                    ?.replaceFirstChar {
                        it.uppercase()
                    }
                    ?: "Weather unavailable"

            currentWeatherSummaryState.value =
                TripWeatherSummary(
                    temperature = temperatureText,
                    condition = conditionText,
                )
        }

        // ==================== Search Test ====================

        val testResults = uiSearchPlacesByName(
            context = this,
            query = "museum",
            currentCity = "M2",
            userLatitude = -37.8136,
            userLongitude = 144.9631,
            limit = 10
        )

        android.util.Log.d(
            "SearchTest",
            "Total results: ${testResults.size}"
        )

        testResults.forEachIndexed { index, place ->
            android.util.Log.d(
                "SearchTest",
                "${index + 1}. ${place.name} | " +
                        "Environment: ${place.environmentType}"
            )
        }

// ==================== End Test ====================












        // Subscribe to the shared repository.
        // Collection automatically stops/restarts with Activity lifecycle.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // GPS: Latest location
                launch {
                    sensorRepository.locationFlow.collect { location ->

                        if (
                            location != null &&
                            !currentWeatherMonitorStarted
                        ) {
                            android.util.Log.d(
                                "ROAMMATE_WEATHER",
                                "GPS for weather: " +
                                        "lat=${location.latitude}, " +
                                        "lon=${location.longitude}"
                            )
                            currentWeatherMonitor.start()

                            currentWeatherMonitorStarted = true
                        }
                    }
                }

                // Step Counter: Cumulative steps since device reboot
                launch {
                    sensorRepository.stepCountFlow.collect { steps ->
                        android.util.Log.d(
                            "RoamMateSensor",
                            "Total steps: $steps"
                        )
                    }
                }

                // Step Detector: Individual step events
                launch {
                    sensorRepository.stepEvents.collect { timestamp ->
                        android.util.Log.d(
                            "RoamMateSensor",
                            "Step detected: $timestamp"
                        )
                    }
                }
            }
        }




        // Enable the page to extend to the status bar and the bottom navigation bar area.
        enableEdgeToEdge()

        // setContent is the entry point of Jetpack Compose
        // and all pages will start to display from here.
        setContent {
            // RoamMateTheme responsible for unifying colors, fonts and the overall visual style.
            RoamMateTheme(dynamicColor = false) {
                // A signed-out user must authenticate before entering the app.
                var currentScreen by rememberSaveable {
                    mutableStateOf(
                        if (cloudUserRepository.currentUser() == null) {
                            AuthScreen.Login
                        } else {
                            AuthScreen.Home
                        },
                    )
                }

                // Jie pet feature state: one koala, live weather gear and a persisted fashion choice.
                val petPreferences = remember { PetPreferences(applicationContext) }
                var petProfile by remember { mutableStateOf(petPreferences.loadProfile()) }
                val petEnvironment = rememberPetEnvironment()
                val petWeather = petEnvironment.weather
                val petState = PetStateEngine.buildUiState(petProfile, petWeather)

                // Firebase keeps the account session across app restarts.
                var userName by rememberSaveable {
                    mutableStateOf(uiLoadUserProfile())
                }
                var isCloudRequestRunning by rememberSaveable { mutableStateOf(false) }

                // Loading states
                // TODO(Yufei, function: uiLoadingState): Set true while backend / Firebase requests are running.
                var isGeneratingItinerary by rememberSaveable { mutableStateOf(false) }
                var isSearchingPlaces by rememberSaveable { mutableStateOf(false) }

                // Demonstration location for Yufei's Explore preview, not a measured GPS fix.
                // TODO(Alex, function: uiGetCurrentLocation): Update this and set isSample = false when GPS changes.
                val currentSensorLocation = remember {
                    mutableStateOf(
                        SensorLocation(
                            latitude = -37.8039,
                            longitude = 144.9717,
                            accuracyMeters = 12f,
                            areaName = "Carlton",
                        ),
                    )
                }

                // Sensor refresh state
                // function: uiObserveShakeEvents; owner: Alex; UI consumer: Yufei.
                // Alex emits shake events; Yufei maps them to the current page action below.
                var exploreShakeRefreshCount by rememberSaveable { mutableStateOf(0) }
                var addStopShakeRefreshCount by rememberSaveable { mutableStateOf(0) }
                var tripShakeProgressCount by rememberSaveable { mutableStateOf(0) }

                // Update existing UI location using GPS StateFlow.
                LaunchedEffect(sensorRepository) {

                    sensorRepository.locationFlow.collectLatest { location ->

                        if (location != null) {

                            // Get latest GPS + geocoding result
                            val result = uiGetCurrentLocation()

                            // Keep previous area name if geocoding fails
                            val previousArea = currentSensorLocation.value.areaName

                            val finalResult = if (
                                result.areaName == "Current location"
                            ) {
                                result.copy(
                                    areaName = previousArea
                                )
                            } else {
                                result
                            }

                            // Update UI state
                            currentSensorLocation.value = finalResult

                        }
                    }
                }


                // Sensor trigger
                // function: uiObserveShakeEvents; owner: Alex; UI consumer: Yufei.
                // Alex emits one shake event; this handler updates the visible UI.
                fun handleShakeTrigger() {
                    when (currentScreen) {
                        AuthScreen.Explore -> {
                            // Explore: shake refreshes nearby suggestions.
                            exploreShakeRefreshCount += 1
                            Toast.makeText(this, "Nearby places refreshed", Toast.LENGTH_SHORT).show()
                        }

                        AuthScreen.Trip -> {
                            // Trip: shake re-checks current progress.
                            tripShakeProgressCount += 1
                            Toast.makeText(this, "Trip progress updated", Toast.LENGTH_SHORT).show()
                        }

                        AuthScreen.PlanMyTrip,
                        AuthScreen.PlanAddStop,
                        AuthScreen.EditAddStop -> {
                            // Planning/search: shake can suggest one nearby place.
                            addStopShakeRefreshCount += 1
                            Toast.makeText(this, "Nearby stop suggestions refreshed", Toast.LENGTH_SHORT).show()
                        }

                        else -> {
                            // Other pages: no shake action.
                        }
                    }
                }


                // Receive shake events from SensorRepository.
                LaunchedEffect(sensorRepository) {
                    uiObserveShakeEvents {
                        handleShakeTrigger()
                    }
                }


                // Attraction detail 当前展示的景点。之后由 Explore/Trip 点击的真实景点数据替换。
                var selectedAttractionDetail by remember {
                    mutableStateOf(sampleAttractionDetail("Melbourne Museum"))
                }

                // 记录详情页从哪里进来，返回按钮才能回到正确页面。
                var attractionDetailReturnScreen by rememberSaveable {
                    mutableStateOf(AuthScreen.Explore)
                }

                // Profile 入口使用：长期默认兴趣偏好，由 Firebase 持久化。
                var profileDefaultInterests by rememberSaveable {
                    mutableStateOf(listOf("Museums", "Parks", "Food"))
                }

                LaunchedEffect(currentScreen) {
                    if (
                        cloudUserRepository.currentUser() != null &&
                        (currentScreen == AuthScreen.Home ||
                            currentScreen == AuthScreen.Profile ||
                            currentScreen == AuthScreen.ProfileInterests)
                    ) {
                        uiLoadProfileInterests(
                            onSuccess = { interests ->
                                if (interests.isNotEmpty()) profileDefaultInterests = interests
                            },
                            onError = { /* Keep the local defaults when no cloud document exists. */ },
                        )
                    }
                }

                // Plan My Trip 入口使用：只属于当前这次行程的兴趣。
                // TODO(Zewen, function: uiSaveTripOnlyInterests): pass one-trip interests into itinerary request only.
                var tripInterests by rememberSaveable {
                    mutableStateOf(listOf("Museums", "Parks", "Food"))
                }

                // Plan My Trip 入口添加的必去地点。
                // TODO(Zewen, function: uiGenerateItinerary): pass these as must-visit places.
                var planRequiredPlaces by remember {
                    mutableStateOf(emptyList<AddStopPlace>())
                }

                // Add stop list
                // function: uiSearchPlacesByName; owner: Alex
                // Used by Plan Add Stop and Edit Add Stop.
                // Shake refreshes this default suggestion list.
                var addStopResultPlaces by remember(
                    addStopShakeRefreshCount,
                    currentSensorLocation.value.latitude,
                    currentSensorLocation.value.longitude,
                ) {
                    mutableStateOf(
                        uiSearchPlacesByName(
                            context = this@MainActivity,
                            query = "",
                            currentCity = "Melbourne",
                            userLatitude = currentSensorLocation.value.latitude,
                            userLongitude = currentSensorLocation.value.longitude,
                            limit = 8,
                        ).rotateLeft(addStopShakeRefreshCount),
                    )
                }

                // Saved trips are loaded from the signed-in user's Firestore document.
                var savedTrips by remember { mutableStateOf(emptyList<SavedTrip>()) }

                LaunchedEffect(currentScreen) {
                    if (
                        currentScreen == AuthScreen.SavedTrips &&
                        cloudUserRepository.currentUser() != null
                    ) {
                        uiLoadSavedTrips(
                            onSuccess = { trips -> savedTrips = trips },
                            onError = ::showCloudError,
                        )
                    }
                }

                // TODO: 这组 Trip timeline 是临时演示假数据。
                // TODO(Zewen, function: uiGenerateItinerary): Replace with real itinerary returned by backend.
                var tripTimelineStops by remember {
                    mutableStateOf(
                        listOf(
                            TripTimelineStop(
                                time = "09:00",
                                title = "Federation Square",
                                status = TripStopStatus.Done,
                            ),
                            TripTimelineStop(
                                time = "10:00",
                                title = "Melbourne Museum",
                                status = TripStopStatus.Current,
                            ),
                            TripTimelineStop(
                                time = "12:00",
                                title = "Lunch nearby",
                                status = TripStopStatus.Upcoming,
                            ),
                            TripTimelineStop(
                                time = "13:30",
                                title = "Pidapipo Gelato",
                                status = TripStopStatus.Upcoming,
                                // TODO: 这是 hidden 小条的临时演示；之后由 Zewen 的 isFiller/hidden-gem 数据决定。
                                hiddenTag = "Hidden gem · nearby",
                            ),
                            TripTimelineStop(
                                time = "14:00",
                                title = "Royal Botanic Gardens",
                                status = TripStopStatus.Upcoming,
                            ),
                        ),
                    )
                }

                // Trip progress demo
                // TODO(Zewen, function: uiUpdateTripProgress): Replace with real progress logic.
                // input provider: Alex GPS location.
                LaunchedEffect(tripShakeProgressCount) {
                    if (tripShakeProgressCount > 0) {
                        tripTimelineStops = tripTimelineStops.advanceCurrentStopForDemo()
                    }
                }

                // Adjust itinerary 弹窗当前显示第几个候选方案。
                var adjustPlanIndex by rememberSaveable { mutableStateOf(0) }

                // Store weather adjustment plans (calculated on-demand)
                var adjustedItineraryPlans by remember {
                    mutableStateOf<List<AdjustItineraryPlan>>(emptyList())
                }

                when (currentScreen) {
                    AuthScreen.Login -> {
                        // After the user opens the app, they first see the Login page.
                        LoginScreen(
                            modifier = Modifier.fillMaxSize(),

                            // Enter Home only after Firebase accepts the credentials.
                            onLoginClick = { email, password ->
                                if (email.isBlank() || password.isBlank()) {
                                    Toast.makeText(
                                        this,
                                        "Please enter email and password",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else if (!isCloudRequestRunning) {
                                    isCloudRequestRunning = true
                                    uiLoginWithFirebase(
                                        email = email,
                                        password = password,
                                        onSuccess = { session ->
                                            isCloudRequestRunning = false
                                            userName = session.readableName()
                                            currentScreen = AuthScreen.Home
                                        },
                                        onError = { error ->
                                            isCloudRequestRunning = false
                                            // A failed login must leave no authenticated session.
                                            cloudUserRepository.signOut()
                                            showCloudError(error)
                                        },
                                    )
                                }
                            },

                            // When the user clicks "Create account", it switches to the registration page.
                            onCreateAccountClick = {
                                currentScreen = AuthScreen.CreateAccount
                            },
                        )
                    }

                    AuthScreen.CreateAccount -> {
                        CreateAccountScreen(
                            modifier = Modifier.fillMaxSize(),

                            // Click the "Back" button return to the Login page.
                            onBackClick = {
                                currentScreen = AuthScreen.Login
                            },

                            // Click "Login" to return to the Login page.
                            onLoginClick = {
                                currentScreen = AuthScreen.Login
                            },

                            onCreateAccountClick = { fullName, email, password ->
                                if (!isCloudRequestRunning) {
                                    isCloudRequestRunning = true
                                    uiCreateAccountWithFirebase(
                                        fullName = fullName,
                                        email = email,
                                        password = password,
                                        onSuccess = {
                                            isCloudRequestRunning = false
                                            cloudUserRepository.signOut()
                                            userName = ""
                                            Toast.makeText(
                                                this,
                                                "Account created. Please log in.",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            currentScreen = AuthScreen.Login
                                        },
                                        onError = { error ->
                                            isCloudRequestRunning = false
                                            // Account creation signs into Firebase before the
                                            // profile is written. If that write fails, return to
                                            // a fully signed-out state instead of bypassing Login.
                                            cloudUserRepository.signOut()
                                            showCloudError(error)
                                        },
                                    )
                                }
                            },

                            // An error prompt on the login page
                            onValidationError = { message ->
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }

                    AuthScreen.Home -> {
                        // Home page multi-owner integration.
                        // Each teammate owns one small function below, then UI wires the result here.
                        val homeUserName = uiLoadUserProfile()
                        val homeCurrentLocation = uiLoadCurrentLocation(currentSensorLocation.value)
                        val homeNextTripStop = uiLoadNextTripStop(tripTimelineStops)
                        val homeTodayTripStops = uiBuildHomeTodayTripStops(tripTimelineStops)
                        val homeLeaveNowReminder = uiBuildLeaveNowReminder(
                            nextTripStop = homeNextTripStop,
                            currentLocation = homeCurrentLocation,
                        )
                        val homeSmartSuggestion = uiBuildHomeSmartSuggestion(
                            weather = petWeather,
                            petStatusLine = petState.statusLine,
                        )

                        HomeScreen(
                            modifier = Modifier.fillMaxSize(),

                            // function: uiLoadUserProfile; owner: Yuxiang.
                            userName = homeUserName,

                            // function: uiBuildHomeTodayTripStops; owner: Yufei; status: UI done.
                            // input provider: Zewen itinerary.
                            todayTripStops = homeTodayTripStops,

                            // function: uiBuildLeaveNowReminder; owner: Yufei; status: UI done.
                            // input providers: Zewen next stop, Alex location.
                            leaveNowReminder = homeLeaveNowReminder,

                            // function: uiBuildHomeSmartSuggestion; owner: Yufei; status: UI done.
                            // input providers: LeYan weather, Xiajie pet status.
                            smartSuggestion = weatherSuggestionState.value,

                            // Jie pet module: Home, Pet and Camera share the same koala outfit.
                            petStatus = HomePetStatus(
                                name = "Buddy",
                                description = petState.statusLine,
                                moodLabel = petState.mood.label,
                                petState = petState,
                            ),

                            // function: uiOpenExternalMap; owner: Yufei; status: UI done.
                            onNavigateReminderClick = {
                                homeLeaveNowReminder?.let { reminder ->
                                    openExternalMap(reminder.navigationQuery)
                                }
                            },

                            // function: uiAdjustItineraryForWeather; owner: Zewen.
                            // UI entry is wired by Yufei.
                            onSmartSuggestionClick = {
                                // Calculate weather adjustment plans RIGHT BEFORE entering the screen
                                adjustedItineraryPlans = uiAdjustItineraryForWeather("Heavy rain expected")
                                adjustPlanIndex = 0 // Reset to first plan
                                currentScreen = AuthScreen.AdjustItinerary
                            },

                            onPetCardClick = {
                                currentScreen = AuthScreen.Pet
                            },

                            // 点击 Home 页底部按钮，进入 Plan My Trip 页面。
                            onPlanMyTripClick = {
                                currentScreen = AuthScreen.PlanMyTrip
                            },

                            onExploreClick = {
                                currentScreen = AuthScreen.Explore
                            },

                            // 点击 today's trip 卡片，进入 Trip 页面。
                            onTripClick = {
                                currentScreen = AuthScreen.Trip
                            },

                            // 底部导航先接好 Home/Trip/Profile，其余页面等对应 UI 做好后再跳转。
                            onTabClick = { tab ->
                                when (tab) {
                                    RoamMateMainTab.Home -> Unit
                                    RoamMateMainTab.Trip -> currentScreen = AuthScreen.Trip
                                    RoamMateMainTab.Profile -> currentScreen = AuthScreen.Profile
                                    RoamMateMainTab.Explore -> currentScreen = AuthScreen.Explore
                                    RoamMateMainTab.Pet -> currentScreen = AuthScreen.Pet
                                }
                            },
                        )
                    }

                    AuthScreen.AdjustItinerary -> {
                        // Ensure plans are loaded when entering this screen
                        if (adjustedItineraryPlans.isEmpty()) {
                            LaunchedEffect(Unit) {
                                adjustedItineraryPlans = uiAdjustItineraryForWeather("Heavy rain expected")
                            }
                        }

                        // Show loading state if plans not ready yet
                        if (adjustedItineraryPlans.isEmpty()) {
                            // Simple loading placeholder
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        } else {
                            val selectedPlan = adjustedItineraryPlans[
                                adjustPlanIndex.coerceIn(0, adjustedItineraryPlans.lastIndex)
                            ]

                            AdjustItineraryScreen(
                                modifier = Modifier.fillMaxSize(),
                                plan = selectedPlan,
                                canRegenerateAnother = adjustPlanIndex < adjustedItineraryPlans.lastIndex,

                                // Apply this plan:
                                // 把 Zewen 给出的候选方案转换成 Trip/Home 共用的 tripTimelineStops。
                                onApplyPlanClick = { plan ->
                                    // Update tripTimelineStops for current day display
                                    tripTimelineStops = plan.toTripTimelineStops()

                                    // Apply the selected plan to currentItinerary
                                    applyWeatherAdjustmentToItinerary(adjustPlanIndex)

                                    Toast.makeText(this, "New itinerary applied", Toast.LENGTH_SHORT).show()
                                    currentScreen = AuthScreen.Trip
                                },

                                // Regenerate another:
                                // Zewen 目前只做两个方案，所以这里只能从第一个切到第二个。
                                onRegenerateAnotherClick = {
                                    adjustPlanIndex = (adjustPlanIndex + 1)
                                        .coerceAtMost(adjustedItineraryPlans.lastIndex)
                                },

                                // 第二个方案没有第三个可生成，所以按钮变成 Back to previous plan。
                                onBackToPreviousPlanClick = {
                                    adjustPlanIndex = (adjustPlanIndex - 1).coerceAtLeast(0)
                                },

                                // 不采用新方案，保留原本 tripTimelineStops。
                                onKeepCurrentPlanClick = {
                                    currentScreen = AuthScreen.Trip
                                },
                            )
                        }
                    }

                    AuthScreen.Trip -> {
                        TripScreen(
                            modifier = Modifier.fillMaxSize(),
                            destinationTitle = "Today in Melbourne",

                            // LeYan: live current weather from WeatherRepository + CurrentWeatherMonitor + Alex GPS
                            weatherSummary = currentWeatherSummaryState.value,

                            // Call Zewen's backend to get trip summary
                            tripSummary = uiLoadTripSummary(),

                            // TODO(Zewen, function: uiUpdateTripProgress): Replace with real itinerary order and progress status.
                            // input provider: Alex GPS location.
                            stops = tripTimelineStops,

                            // TODO(Zewen, function: uiLoadTripDays): Connect full multi-day itinerary.
                            tripDays = uiLoadTripDays(tripTimelineStops),

                            // TODO(Leyan, function: uiLoadAttractionDetail): Open detail with real attraction id.
                            // storage provider: Yuxiang database if needed.
                            onStopClick = { stop ->
                                selectedAttractionDetail = sampleAttractionDetail(stop.title)
                                attractionDetailReturnScreen = AuthScreen.Trip
                                currentScreen = AuthScreen.AttractionDetail
                            },

                            // End trip: close the current active trip and return Home.
                            onEndTripClick = {
                                Toast.makeText(this, "Trip ended", Toast.LENGTH_SHORT).show()
                                currentScreen = AuthScreen.Home
                            },

                            // Start navigation 和 Home 提醒条共用外部地图逻辑。
                            onStartNavigationClick = {
                                openExternalMap("Melbourne Museum")
                            },

                            // Trip 当前页点 Trip 不动，其余已完成页面正常切换。
                            onTabClick = { tab ->
                                when (tab) {
                                    RoamMateMainTab.Home -> currentScreen = AuthScreen.Home
                                    RoamMateMainTab.Trip -> Unit
                                    RoamMateMainTab.Explore -> currentScreen = AuthScreen.Explore
                                    RoamMateMainTab.Profile -> currentScreen = AuthScreen.Profile
                                    RoamMateMainTab.Pet -> currentScreen = AuthScreen.Pet
                                }
                            },
                        )
                    }

                    AuthScreen.Explore -> {
                        // Nearby places
                        // function: uiExploreNearbyPlaces; owner: Alex
                        // input: GPS + selected category source
                        // output: nearby place cards for Explore
                        // Shake refreshes order now; later Alex can refresh real nearby data here.
                        val nearbyExplorePlaces = remember(
                            exploreShakeRefreshCount,
                            currentSensorLocation.value.latitude,
                            currentSensorLocation.value.longitude,
                        ) {
                            val location = currentSensorLocation.value
                            ExplorePlaceCategory.entries
                                .flatMap { category ->

                                    // Get top 10 places for each category
                                    val pool = uiExploreNearbyPlaces(
                                        context = this@MainActivity,
                                        category = category,
                                        userLatitude = location.latitude,
                                        userLongitude = location.longitude,
                                        limit = 10,
                                    )

                                    // Randomly select 5 indices
                                    val keepIndices = pool.indices
                                        .shuffled(
                                            kotlin.random.Random(
                                                exploreShakeRefreshCount + category.ordinal
                                            )
                                        )
                                        .take(5)
                                        .toSet()

                                    // Keep original ranking order
                                    pool.filterIndexed { index, _ ->
                                        index in keepIndices
                                    }
                                }
                                .distinctBy { place -> place.name }
                        }

                        ExploreScreen(
                            modifier = Modifier.fillMaxSize(),
                            currentArea = currentSensorLocation.value.let { location ->
                                if (location.isSample) "${location.areaName} · sample" else location.areaName
                            },
                            places = nearbyExplorePlaces,

                            // TODO(Leyan, function: uiLoadAttractionDetail): Open detail with real attraction id.
                            // storage provider: Yuxiang database if needed.
                            onPlaceClick = { place ->
                                selectedAttractionDetail = place.toAttractionDetail()
                                attractionDetailReturnScreen = AuthScreen.Explore
                                currentScreen = AuthScreen.AttractionDetail
                            },

                            // Explore 当前页点 Explore 不动，其余已完成页面正常切换。
                            onTabClick = { tab ->
                                when (tab) {
                                    RoamMateMainTab.Home -> currentScreen = AuthScreen.Home
                                    RoamMateMainTab.Trip -> currentScreen = AuthScreen.Trip
                                    RoamMateMainTab.Explore -> Unit
                                    RoamMateMainTab.Profile -> currentScreen = AuthScreen.Profile
                                    RoamMateMainTab.Pet -> currentScreen = AuthScreen.Pet
                                }
                            },
                        )
                    }

                    AuthScreen.AttractionDetail -> {
                        AttractionDetailScreen(
                            modifier = Modifier.fillMaxSize(),
                            attraction = selectedAttractionDetail,

                            // 返回到打开详情页的来源页面：Explore 或 Trip。
                            onBackClick = {
                                currentScreen = attractionDetailReturnScreen
                            },

                            // Navigate: 打开外部地图，和 Home/Trip 共用同一个入口。
                            onNavigateClick = { attraction ->
                                openExternalMap(attraction.name)
                            },

                            // Visit official website: 打开 Leyan 提供的景点官网。
                            onWebsiteClick = { attraction ->
                                openExternalWebsite(attraction.websiteUrl)
                            },
                        )
                    }

                    AuthScreen.EditItinerary -> {
                        EditItineraryScreen(
                            modifier = Modifier.fillMaxSize(),

                            // 已完成的站点不显示在编辑页；只编辑当前及后面的行程。
                            stops = tripTimelineStops.filter { stop ->
                                stop.status != TripStopStatus.Done
                            },

                            // 返回 Trip，不保存额外修改。
                            onBackClick = {
                                currentScreen = AuthScreen.Trip
                            },

                            // TODO(Zewen, function: uiUpdateItineraryAfterEdit): Send removed stop list for recalculation.
                            onRemoveStopClick = { removedStop ->
                                tripTimelineStops = rebalanceTripStopsAfterManualEdit(
                                    tripTimelineStops.filterNot { stop ->
                                        stop.time == removedStop.time && stop.title == removedStop.title
                                    },
                                )
                            },

                            // TODO(Zewen, function: uiUpdateItineraryAfterEdit): Add stop via search, then recalculate.
                            // input provider: Alex place search.
                            onAddStopClick = {
                                currentScreen = AuthScreen.EditAddStop
                            },

                            // TODO(Zewen, function: uiUpdateItineraryAfterEdit): Recalculate reordered stops.
                            // storage provider: Yuxiang saves final itinerary.
                            onSaveClick = { reorderedStops ->
                                val doneStops = tripTimelineStops.filter { stop ->
                                    stop.status == TripStopStatus.Done
                                }
                                tripTimelineStops = rebalanceTripStopsAfterManualEdit(
                                    doneStops + reorderedStops,
                                )
                                Toast.makeText(this, "Itinerary saved", Toast.LENGTH_SHORT).show()
                                currentScreen = AuthScreen.Trip
                            },
                        )
                    }

                    AuthScreen.PlanMyTrip -> {
                        // Log the current state of planRequiredPlaces
                        android.util.Log.d("RoamMate", "PlanMyTrip screen: planRequiredPlaces size = ${planRequiredPlaces.size}")
                        planRequiredPlaces.forEachIndexed { index, place ->
                            android.util.Log.d("RoamMate", "  [$index] ${place.name}")
                        }

                        PlanMyTripScreen(
                            modifier = Modifier.fillMaxSize(),

                            // 这里是本次行程兴趣，不是 Profile 的长期默认兴趣。
                            selectedInterests = tripInterests,
                            specificPlaces = planRequiredPlaces.map { place ->
                                place.name
                            },
                            onInterestsChanged = { updatedInterests ->
                                tripInterests = updatedInterests
                            },

                            // 返回 Home 页面。
                            onBackClick = {
                                currentScreen = AuthScreen.Home
                            },

                            // 从 Plan My Trip 进入 Interests，只保存本次行程兴趣。
                            onMoreInterestsClick = {
                                currentScreen = AuthScreen.PlanTripInterests
                            },

                            // 进入 Add a stop 页面，添加本次规划里“我一定要去”的地点。
                            onSearchPlacesClick = {
                                currentScreen = AuthScreen.PlanAddStop
                            },

                            // TODO(Zewen, function: uiGenerateItinerary): Send request to planning backend, then open Trip.
                            isGeneratingItinerary = isGeneratingItinerary,
                            onGenerateItineraryClick = { request ->

                                isGeneratingItinerary = true

                                Toast.makeText(
                                    this,
                                    "Generating itinerary for ${request.destination}",
                                    Toast.LENGTH_SHORT,
                                ).show()

                                android.util.Log.d(
                                    "PLANNED_WEATHER",
                                    "UI request: destination=${request.destination}, " +
                                            "startDate=${request.startDate}, " +
                                            "endDate=${request.endDate}"
                                )

                                uiLoadPlannedWeather(
                                    request = request
                                ) { forecasts ->

                                    if (forecasts == null) {

                                        android.util.Log.e(
                                            "PLANNED_WEATHER",
                                            "Failed to load planned weather"
                                        )

                                        Toast.makeText(
                                            this,
                                            "Planned weather request failed",
                                            Toast.LENGTH_SHORT,
                                        ).show()

                                        isGeneratingItinerary = false
                                        return@uiLoadPlannedWeather
                                    }

                                    android.util.Log.d(
                                        "PLANNED_WEATHER",
                                        "Forecast count = ${forecasts.size}"
                                    )

                                    forecasts.forEach { forecast ->
                                        android.util.Log.d(
                                            "PLANNED_WEATHER",
                                            "date=${forecast.date}, status=${forecast.status}"
                                        )
                                    }

                                    // Pass trip request + planned weather to Zewen
                                    tripTimelineStops =
                                        uiGenerateItinerary(
                                            request = request,
                                            weatherForecasts = forecasts,
                                        )

                                    isGeneratingItinerary = false

                                    Toast.makeText(
                                        this,
                                        "Itinerary generated successfully!",
                                        Toast.LENGTH_SHORT,
                                    ).show()

                                    currentScreen = AuthScreen.Trip
                                }
                            },
                        )
                    }

                    AuthScreen.PlanAddStop -> {
                        AddStopScreen(
                            modifier = Modifier.fillMaxSize(),
                            currentCity = "Melbourne",
                            places = addStopResultPlaces,
                            isSearching = isSearchingPlaces,

                            // 从 Plan My Trip 进来，返回 Plan My Trip。
                            onBackClick = {
                                currentScreen = AuthScreen.PlanMyTrip
                            },

                            // TODO(Alex, function: uiSearchPlacesByName): Send query only after confirm button; name match first.
                            // 输入框中间修改不传；只有按确认搜索按钮后才会走到这里。
                            onSearchConfirmClick = { query ->
                                val location = currentSensorLocation.value
                                isSearchingPlaces = true
                                addStopResultPlaces = uiSearchPlacesByName(
                                    context = this@MainActivity,
                                    query = query,
                                    currentCity = "Melbourne",
                                    userLatitude = location.latitude,
                                    userLongitude = location.longitude,
                                    limit = 8,
                                )
                                Toast.makeText(
                                    this,
                                    "Search results updated",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                // TODO(Alex, function: uiSearchPlacesByName): Set false after search results return.
                                // data provider: Leyan attraction names.
                                isSearchingPlaces = false
                            },

                            onClearSearchClick = {
                                val location = currentSensorLocation.value
                                addStopResultPlaces = uiSearchPlacesByName(
                                    context = this@MainActivity,
                                    query = "",
                                    currentCity = "Melbourne",
                                    userLatitude = location.latitude,
                                    userLongitude = location.longitude,
                                    limit = 8,
                                ).rotateLeft(addStopShakeRefreshCount)
                            },

                            // TODO(Zewen, function: uiGenerateItinerary): Add selected place into must-visit request.
                            onAddPlaceClick = { place ->
                                planRequiredPlaces = planRequiredPlaces.addUniquePlace(place)
                                Toast.makeText(
                                    this,
                                    "${place.name} added to this trip plan",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                currentScreen = AuthScreen.PlanMyTrip
                            },
                        )
                    }

                    AuthScreen.EditAddStop -> {
                        AddStopScreen(
                            modifier = Modifier.fillMaxSize(),
                            currentCity = "Melbourne",
                            places = addStopResultPlaces,
                            isSearching = isSearchingPlaces,

                            // 从 Edit itinerary 进来，返回 Edit itinerary。
                            onBackClick = {
                                currentScreen = AuthScreen.EditItinerary
                            },

                            // TODO(Alex, function: uiSearchPlacesByName): Send query only after confirm button; name match first.
                            // 输入框中间修改不传；只有按确认搜索按钮后才会走到这里。
                            onSearchConfirmClick = { query ->
                                val location = currentSensorLocation.value
                                isSearchingPlaces = true
                                addStopResultPlaces = uiSearchPlacesByName(
                                    context = this@MainActivity,
                                    query = query,
                                    currentCity = "Melbourne",
                                    userLatitude = location.latitude,
                                    userLongitude = location.longitude,
                                    limit = 8,
                                )
                                Toast.makeText(
                                    this,
                                    "Search results updated",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                // TODO(Alex, function: uiSearchPlacesByName): Set false after search results return.
                                // data provider: Leyan attraction names.
                                isSearchingPlaces = false
                            },

                            onClearSearchClick = {
                                val location = currentSensorLocation.value
                                addStopResultPlaces = uiSearchPlacesByName(
                                    context = this@MainActivity,
                                    query = "",
                                    currentCity = "Melbourne",
                                    userLatitude = location.latitude,
                                    userLongitude = location.longitude,
                                    limit = 8,
                                ).rotateLeft(addStopShakeRefreshCount)
                            },

                            // TODO(Zewen, function: uiUpdateItineraryAfterEdit): Send added place for insert and recalculation.
                            onAddPlaceClick = { place ->
                                tripTimelineStops = rebalanceTripStopsAfterManualEdit(
                                    tripTimelineStops.withAddedTemporaryStop(place),
                                )
                                Toast.makeText(
                                    this,
                                    "${place.name} added to itinerary",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                currentScreen = AuthScreen.EditItinerary
                            },
                        )
                    }

                    AuthScreen.PlanTripInterests -> {
                        InterestsScreen(
                            modifier = Modifier.fillMaxSize(),
                            saveTarget = InterestsSaveTarget.TripOnly,
                            initialSelectedInterests = tripInterests,

                            // 从 Plan 进来，返回就回到 Plan My Trip。
                            onBackClick = {
                                currentScreen = AuthScreen.PlanMyTrip
                            },

                            // Done 后只更新本次行程兴趣，再返回 Plan My Trip。
                            onDoneClick = { updatedInterests ->
                                tripInterests = uiSaveTripOnlyInterests(updatedInterests)
                                currentScreen = AuthScreen.PlanMyTrip
                            },
                        )
                    }

                    AuthScreen.Pet -> {
                        val nextStop = tripTimelineStops.firstOrNull {
                            it.status != TripStopStatus.Done
                        }
                        CompanionsScreen(
                            state = petState,
                            environment = petEnvironment,
                            onWardrobeSelected = { wardrobeChoice ->
                                val updatedProfile = petProfile.copy(
                                    wardrobeChoice = wardrobeChoice,
                                )
                                petProfile = updatedProfile
                                petPreferences.saveProfile(updatedProfile)
                            },
                            tripContext = PetTripContext(
                                nextStopName = nextStop?.title,
                                nextStopTime = nextStop?.time,
                                isDemo = true, // Change only when real itinerary data replaces sample stops.
                            ),
                            onOpenCamera = { currentScreen = AuthScreen.PetCamera },
                            onTabClick = { tab ->
                                when (tab) {
                                    RoamMateMainTab.Home -> currentScreen = AuthScreen.Home
                                    RoamMateMainTab.Trip -> currentScreen = AuthScreen.Trip
                                    RoamMateMainTab.Explore -> currentScreen = AuthScreen.Explore
                                    RoamMateMainTab.Pet -> Unit
                                    RoamMateMainTab.Profile -> currentScreen = AuthScreen.Profile
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    AuthScreen.PetCamera -> {
                        PetCameraScreen(
                            state = petState,
                            onBack = { currentScreen = AuthScreen.Pet },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    AuthScreen.Profile -> {
                        ProfileScreen(
                            modifier = Modifier.fillMaxSize(),

                            userName = userName,

                            // 从 Profile 进入 Interests，保存为用户长期默认偏好。
                            onTravelPreferencesClick = {
                                currentScreen = AuthScreen.ProfileInterests
                            },

                            // Open Saved trips page.
                            onSavedTripsClick = {
                                currentScreen = AuthScreen.SavedTrips
                            },

                            // Open Edit profile page.
                            onEditProfileClick = {
                                currentScreen = AuthScreen.EditProfile
                            },

                            onLogoutClick = {
                                cloudUserRepository.signOut()
                                userName = ""
                                savedTrips = emptyList()
                                currentScreen = AuthScreen.Login
                            },

                            // 底部导航先接好 Home/Trip/Profile，其余页面等对应 UI 做好后再跳转。
                            onTabClick = { tab ->
                                when (tab) {
                                    RoamMateMainTab.Profile -> Unit
                                    RoamMateMainTab.Home -> currentScreen = AuthScreen.Home
                                    RoamMateMainTab.Trip -> currentScreen = AuthScreen.Trip
                                    RoamMateMainTab.Explore -> currentScreen = AuthScreen.Explore
                                    RoamMateMainTab.Pet -> currentScreen = AuthScreen.Pet
                                }
                            },
                        )
                    }

                    AuthScreen.SavedTrips -> {
                        SavedTripsScreen(
                            modifier = Modifier.fillMaxSize(),
                            savedTrips = savedTrips,

                            // Back button.
                            onBackClick = {
                                currentScreen = AuthScreen.Profile
                            },

                            // Create button.
                            onCreateNewTripClick = {
                                currentScreen = AuthScreen.PlanMyTrip
                            },
                        )
                    }

                    AuthScreen.EditProfile -> {
                        EditProfileScreen(
                            modifier = Modifier.fillMaxSize(),
                            initialName = userName,

                            // Back button.
                            onBackClick = {
                                currentScreen = AuthScreen.Profile
                            },

                            onSaveChangesClick = { updatedName ->
                                if (!isCloudRequestRunning) {
                                    isCloudRequestRunning = true
                                    uiSaveUserProfile(
                                        updatedName = updatedName,
                                        onSuccess = { session ->
                                            isCloudRequestRunning = false
                                            userName = session.readableName()
                                            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                                            currentScreen = AuthScreen.Profile
                                        },
                                        onError = { error ->
                                            isCloudRequestRunning = false
                                            showCloudError(error)
                                        },
                                    )
                                }
                            },

                            // Error toast.
                            onValidationError = { message ->
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }

                    AuthScreen.ProfileInterests -> {
                        InterestsScreen(
                            modifier = Modifier.fillMaxSize(),
                            saveTarget = InterestsSaveTarget.ProfileDefault,
                            initialSelectedInterests = profileDefaultInterests,

                            // 从 Profile 进来，返回就回到 Profile。
                            onBackClick = {
                                currentScreen = AuthScreen.Profile
                            },

                            onDoneClick = { updatedInterests ->
                                if (!isCloudRequestRunning) {
                                    isCloudRequestRunning = true
                                    uiSaveProfileInterests(
                                        interests = updatedInterests,
                                        onSuccess = { savedInterests ->
                                            isCloudRequestRunning = false
                                            profileDefaultInterests = savedInterests
                                            currentScreen = AuthScreen.Profile
                                        },
                                        onError = { error ->
                                            isCloudRequestRunning = false
                                            showCloudError(error)
                                        },
                                    )
                                }
                            },
                        )
                    }

                    AuthScreen.SensorTest -> {
                        SensorTestScreen(
                            sensorRepository = sensorRepository,
                            onBack = {
                                currentScreen = AuthScreen.Login
                            },
                            searchPlacesByName = {query, latitude, longitude ->
                                uiSearchPlacesByName(
                                    context = this@MainActivity,
                                    query = query,
                                    currentCity = "Melbourne",
                                    userLatitude = latitude,
                                    userLongitude = longitude,
                                    limit = 10
                                )
                            }
                        )

                    }




                }
            }
        }
    }






    // Start sensors when Activity becomes visible.
    override fun onStart() {
        super.onStart()

        // Start persistent step tracking when permitted.
        if (hasActivityRecognitionPermission()) {
            startBackgroundStepTracking()
        }

        // Bind to SensorService.
        if (!bindingRequested) {
            bindingRequested = bindService(
                Intent(this, SensorService::class.java),
                sensorConnection,
                Context.BIND_AUTO_CREATE
            )
        }

        // Shake Detector.
        sensorRepository.startShakeDetection()
//        val shakeStarted = sensorRepository.startShakeDetection()
//        android.util.Log.d(
//            "RoamMateSensor",
//            "Shake start result: $shakeStarted"
//        )


        // Step Detector.
        if (hasActivityRecognitionPermission()) {
            sensorRepository.startStepDetection()
//            android.util.Log.d(
//                "RoamMateSensor",
//                "Step Detector start requested"
//            )
        } else {
            activityPermissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        }

        // GPS
        if (hasLocationPermission()) {
            sensorRepository.startLocationTracking()
//            android.util.Log.d(
//                "RoamMateSensor",
//                "GPS start requested"
//            )
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }




    }

    override fun onStop() {
        android.util.Log.d(
            "RoamMateSensor",
            "MainActivity onStop: stopping foreground-only sensors"
        )

        currentWeatherMonitor.stop()
        weatherForecastMonitor.stop()
        currentWeatherMonitorStarted = false

        // Foreground-only sensors stop.
        sensorRepository.stopShakeDetection()
        sensorRepository.stopStepDetection()
        sensorRepository.stopLocationTracking()

        android.util.Log.d(
            "RoamMateSensor",
            "Shake, Step Detector and GPS stopped"
        )

        // Do NOT stop Step Counter.
        // The Foreground Service continues independently.

        if (bindingRequested) {
            unbindService(sensorConnection)
            bindingRequested = false
            sensorService = null
        }

        super.onStop()
    }






    // ==================== UI integration functions ====================
    // These functions are grouped by owner.
    // Team members can search "function: xxx" and edit only their own function.
    // Current returns are mock/sample data so the UI can still run before backend integration.


    // ---- Home page split example ----

    private fun uiLoadNextTripStop(
        stops: List<TripTimelineStop>,
    ): TripTimelineStop? {
        // function: uiLoadNextTripStop
        // owner: Zewen
        // input: full itinerary stops
        // output: next stop for Home reminder
        // TODO(Zewen): Replace with backend current/next stop selection.
        return stops.firstOrNull { stop ->
            stop.status == TripStopStatus.Current
        } ?: stops.firstOrNull { stop ->
            stop.status == TripStopStatus.Upcoming
        }
    }

    private fun uiLoadCurrentLocation(
        latestLocation: SensorLocation,
    ): SensorLocation {
        // function: uiLoadCurrentLocation
        // owner: Alex
        // input: SensorRepository latest location
        // output: current location used by Home / Explore / Add stop
        // TODO(Alex): Pass real GPS location here from sensor module.
        return latestLocation
    }

    private fun uiBuildLeaveNowReminder(
        nextTripStop: TripTimelineStop?,
        currentLocation: SensorLocation,
    ): HomeLeaveNowReminder? {
        // function: uiBuildLeaveNowReminder
        // owner: Yufei
        // input: Zewen next stop + Alex current location
        // output: Home leave-now card data
        // TODO(Yufei): Replace demo delay with route-time comparison after backend/sensor is ready.
        if (nextTripStop == null) return null

        val demoDelayMinutes = if (currentLocation.isSample) 3 else 3
        return HomeLeaveNowReminder(
            placeName = nextTripStop.title,
            scheduledTime = nextTripStop.time,
            delayMinutes = demoDelayMinutes,
            navigationQuery = nextTripStop.title,
        )
    }

    private fun uiBuildHomeTodayTripStops(
        stops: List<TripTimelineStop>,
    ): List<HomeTripStop> {
        // function: uiBuildHomeTodayTripStops
        // owner: Yufei
        // input: Zewen itinerary stops
        // output: Home compact trip list
        return stops.toHomeTripStops()
    }

    private fun uiBuildHomeSmartSuggestion(
        weather: PetWeatherSnapshot,
        petStatusLine: String,
    ): HomeSmartSuggestion {
        // function: uiBuildHomeSmartSuggestion
        // owner: Yufei
        // input: LeYan weather + Xiajie pet status line
        // output: Home smart suggestion card data
        // status: UI card done
        // TODO(Zewen): Open Adjust itinerary only when backend says weather affects the plan.
        // input provider: LeYan weather.
        return HomeSmartSuggestion(
            label = if (weather.isCurrent) weather.label else "Weather",
            message = if (weather.isCurrent) {
                "${weather.locationLabel}: ${weather.temperatureC.toInt()}°C · $petStatusLine"
            } else {
                "Live weather is unavailable. Open Buddy to refresh."
            },
        )
    }

    // ---- Yuxiang: Firebase auth + user database ----

    private fun uiLoginWithFirebase(
        email: String,
        password: String,
        onSuccess: (CloudUserSession) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            cloudUserRepository.signIn(email, password, cloudCallback(onSuccess, onError))
        } catch (error: Exception) {
            onError(error)
        }
    }

    private fun uiCreateAccountWithFirebase(
        fullName: String,
        email: String,
        password: String,
        onSuccess: (CloudUserSession) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            cloudUserRepository.register(
                fullName,
                email,
                password,
                cloudCallback(onSuccess, onError),
            )
        } catch (error: Exception) {
            onError(error)
        }
    }

    private fun uiLoadUserProfile(): String =
        cloudUserRepository.currentUser()?.readableName().orEmpty()

    private fun uiSaveUserProfile(
        updatedName: String,
        onSuccess: (CloudUserSession) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            cloudUserRepository.updateDisplayName(
                updatedName,
                cloudCallback(onSuccess, onError),
            )
        } catch (error: Exception) {
            onError(error)
        }
    }

    private fun uiSaveProfileInterests(
        interests: List<String>,
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            cloudUserRepository.savePreferences(
                UserPreferences("system", "en-AU", interests, true),
                object : CloudResultCallback<Void> {
                    override fun onSuccess(value: Void?) = onSuccess(interests)
                    override fun onError(error: Exception) = onError(error)
                },
            )
        } catch (error: Exception) {
            onError(error)
        }
    }

    private fun uiLoadProfileInterests(
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            cloudUserRepository.loadPreferences(
                object : CloudResultCallback<UserPreferences> {
                    override fun onSuccess(value: UserPreferences) =
                        onSuccess(value.preferredCategories)

                    override fun onError(error: Exception) = onError(error)
                },
            )
        } catch (error: Exception) {
            onError(error)
        }
    }

    private fun <T> cloudCallback(
        onSuccess: (T) -> Unit,
        onError: (Exception) -> Unit,
    ) = object : CloudResultCallback<T> {
        override fun onSuccess(value: T) = onSuccess(value)
        override fun onError(error: Exception) = onError(error)
    }

    private fun CloudUserSession.readableName(): String =
        displayName.ifBlank { email.substringBefore('@').ifBlank { "Traveller" } }

    private fun showCloudError(error: Exception) {
        Toast.makeText(
            this,
            error.localizedMessage ?: "Cloud request failed",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun uiSaveTripOnlyInterests(interests: List<String>): List<String> {
        // function: uiSaveTripOnlyInterests
        // owner: Zewen
        // UI consumer: Yufei
        // input: current-trip interest labels
        // output: interest labels used only for this trip request
        // TODO(Zewen): Read these interests when generating the current itinerary.
        return interests
    }

    private fun uiLoadSavedTrips(
        onSuccess: (List<SavedTrip>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        // function: uiLoadSavedTrips
        // owner: Yuxiang
        // contributor: Zewen trip summary shape
        // output: saved trip cards
        try {
            plannerRepository.listTrips(
                object : PlannerResultCallback<List<CloudSavedTripSummary>> {
                    override fun onSuccess(value: List<CloudSavedTripSummary>) {
                        onSuccess(
                            value.map { trip ->
                                SavedTrip(
                                    destination = trip.destination,
                                    durationText = trip.durationText,
                                    dateText = trip.dateText,
                                )
                            },
                        )
                    }

                    override fun onError(error: Exception) = onError(error)
                },
            )
        } catch (error: Exception) {
            onError(error)
        }
    }

    // ---- Alex: sensor + fuzzy search trigger ----

    private suspend fun uiGetCurrentLocation(): SensorLocation {
        // function: uiGetCurrentLocation
        // owner: Alex
        // output: current latitude/longitude/area

        // Wait until GPS has a valid location
        val location = sensorRepository.locationFlow
            .filterNotNull()
            .first()

        // Convert GPS coordinates into area name
        val areaName = geocodingRepository.getAreaName(
            latitude = location.latitude,
            longitude = location.longitude
        )

        return SensorLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = 12f,
            areaName = areaName,
            isSample = false
        )
    }


    private suspend fun uiObserveShakeEvents(
        onShake: () -> Unit
    ) {
        // function: uiObserveShakeEvents
        // owner: Alex
        // UI consumer: Yufei
        // input: current screen
        // output: UI action label
        sensorRepository.shakeEvents.collect {
            onShake()
        }
    }

    private fun uiSearchPlacesByName(
        context: Context,
        query: String,
        currentCity: String?,
        userLatitude: Double?,
        userLongitude: Double?,
        limit: Int,
    ): List<AddStopPlace> {
        // function: uiSearchPlacesByName
        // owner: Alex
        // data source: Leyan POI source
        // storage: Yuxiang database if needed
        // input: query, city, GPS, limit
        // output: place name + environment type for AddStop UI
        val repository: PlaceRepository = DatabasePOI(context)
        val searchService = POISearchService(repository)

        return searchService.search(
            query = query,
            userLatitude = userLatitude,
            userLongitude = userLongitude,
            limit = limit,
        ).map { result ->
            AddStopPlace(
                name = result.place.name,
                environmentType = result.place.environment.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
            )
        }
    }

    // ---- Alex: nearby POI ranking ----

    private fun uiExploreNearbyPlaces(
        context: Context,
        category: ExplorePlaceCategory,
        userLatitude: Double?,
        userLongitude: Double?,
        limit: Int,
    ): List<ExplorePlace> {
        // function: uiExploreNearbyPlaces
        // owner: Alex
        // data source: Leyan POI source
        // sensor source: Alex GPS
        // input: category filter, GPS, limit
        // output: POI list mapped to ExplorePlace
        val repository = DatabasePOI(context)
        val searchService = POISearchService(repository)

        return searchService.search(
            query = "",
            userLatitude = userLatitude,
            userLongitude = userLongitude,
            category = category,
            limit = limit,
        ).map { result ->
            val place = result.place
            ExplorePlace(
                name = place.name,
                distanceText = result.distanceMeters?.let { distance ->
                    if (distance < 1000.0) {
                        "${distance.toInt()} m"
                    } else {
                        String.format(java.util.Locale.US, "%.1f km", distance / 1000.0)
                    }
                } ?: "",
                category = category,
                environmentLabel = place.environment.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
                tagText = result.tagText,
                mapX = 0.5f,
                mapY = 0.5f,
            )
        }
    }

    // ---- Leyan: attraction data ----

    private fun uiLoadAttractionDetail(attractionIdOrName: String): AttractionDetail {
        // function: uiLoadAttractionDetail
        // owner: Leyan
        // storage: Yuxiang database if needed
        // input: attraction id or attraction name
        // output: POI detail mapped to AttractionDetail
        // TODO(Leyan): Load POI photo, hours, indoor/outdoor, website from real database.
        return sampleAttractionDetail(attractionIdOrName)
    }

    // ---- Zewen: itinerary planning ----

    // Lazy initialization of TripPlannerEngine
    private val tripPlannerEngine by lazy {
        com.group5.roammate.logic.TripPlannerEngine(
            poiRepository = com.group5.roammate.logic.utils.MockPoiRepository(
                useJsonData = true,
                context = applicationContext
            )
        )
    }

    private fun uiGenerateItinerary(
        request: PlanMyTripRequest,
        weatherForecasts: List<DailyWeatherForecast>,
    ): List<TripTimelineStop> {
        // function: uiGenerateItinerary
        // owner: Zewen
        // input: destination/date/time/interests/transport/required places
        // output: generated itinerary stops

        try {
            // Step 1: Convert UI date format "12 Sep 2026" to backend format "2026-09-12"
            val backendDate = convertUIDateToBackendFormat(request.date)

            // Step 2: Convert UI interests to backend POICategory
            val userProfile = com.group5.roammate.logic.models.UserProfile(
                interests = convertInterestsToPOICategories(request.interests),
                budgetPreference = com.group5.roammate.logic.models.BudgetLevel.MEDIUM,
                transportMode = convertTransportMode(request.transportMode),
                transportPreference = 1.0f
            )

            // Step 3: Convert required places to pinned POIs
            android.util.Log.d("RoamMate", "Step 3: request.specificPlaces = ${request.specificPlaces}")
            val pinnedPOIs = convertToPinnedPOIs(
                placeNames = request.specificPlaces,
                destination = request.destination
            )
            android.util.Log.d("RoamMate", "Step 3: pinnedPOIs created = ${pinnedPOIs.size}")
            pinnedPOIs.forEach { pinned ->
                android.util.Log.d("RoamMate", "  - PinnedPOI: poiId=${pinned.poiId}, dayNumber=${pinned.dayNumber}")
            }

            // Step 4: Get starting location
            // TODO(Alex): Replace with real GPS location from currentSensorLocation
            val startingLocation = com.group5.roammate.logic.models.Coordinates(
                latitude = -37.8136,
                longitude = 144.9631
            )

            // Step 5: Calculate number of days from date range
            val numberOfDays = calculateNumberOfDays(request.startDate, request.endDate)

            // Step 6: Call backend to generate itinerary
            val itinerary = tripPlannerEngine.generateInitialTrip(
                tripId = "trip_${System.currentTimeMillis()}",
                destination = request.destination,
                startDate = backendDate, // Use converted date format
                numberOfDays = numberOfDays,
                userProfile = userProfile,
                startingLocation = startingLocation,
                dailyStartTime = request.startTime,
                dailyEndTime = "18:00",
                weatherStatus = com.group5.roammate.logic.models.WeatherStatus.SUNNY,
                includeFillers = true,
                pinnedPOIs = pinnedPOIs
            )

            // Step 7: Save itinerary for later use (trip summary, adjustments)
            currentItinerary = itinerary

            // Step 8: Convert backend Itinerary to UI TripTimelineStop list
            return convertItineraryToTimelineStops(itinerary)

        } catch (e: Exception) {
            android.util.Log.e("RoamMate", "Error generating itinerary", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            // Return fallback data on error
            return listOf(
                TripTimelineStop("09:00", "Federation Square", TripStopStatus.Current),
                TripTimelineStop("10:00", "Melbourne Museum", TripStopStatus.Upcoming),
            )
        }
    }

    /**
     * Convert UI date format "12 Sep 2026" or "12 Sep 2026 - 13 Sep 2026" to backend format "2026-09-12"
     */
    private fun convertUIDateToBackendFormat(uiDate: String): String {
        return try {
            // Handle date range: take the first date only
            val firstDate = if (uiDate.contains(" - ")) {
                uiDate.split(" - ")[0].trim()
            } else {
                uiDate.trim()
            }

            // Parse "12 Sep 2026" format
            val parts = firstDate.split(" ")
            if (parts.size != 3) {
                android.util.Log.w("RoamMate", "Invalid date format: $uiDate, using default")
                return "2026-09-12" // Default fallback
            }

            val day = parts[0].toInt()
            val monthStr = parts[1]
            val year = parts[2].toInt()

            // Convert month name to number
            val month = when (monthStr.lowercase()) {
                "jan" -> 1
                "feb" -> 2
                "mar" -> 3
                "apr" -> 4
                "may" -> 5
                "jun" -> 6
                "jul" -> 7
                "aug" -> 8
                "sep" -> 9
                "oct" -> 10
                "nov" -> 11
                "dec" -> 12
                else -> {
                    android.util.Log.w("RoamMate", "Unknown month: $monthStr, using 9")
                    9
                }
            }

            // Format as "yyyy-MM-dd"
            val result = String.format("%04d-%02d-%02d", year, month, day)
            android.util.Log.d("RoamMate", "Converted date: $uiDate -> $result")
            result
        } catch (e: Exception) {
            android.util.Log.e("RoamMate", "Error converting date: $uiDate", e)
            "2026-09-12" // Default fallback
        }
    }

    /**
     * Calculate number of days between startDate and endDate
     * UI date format: "13 Oct 2026"
     * Returns the number of days (inclusive), minimum 1 day
     */
    private fun calculateNumberOfDays(startDate: String, endDate: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

            val start = dateFormat.parse(startDate.trim())
            val end = dateFormat.parse(endDate.trim())

            if (start == null || end == null) {
                android.util.Log.w("RoamMate", "Failed to parse dates: startDate=$startDate, endDate=$endDate")
                return 1
            }

            val diffInMillis = end.time - start.time
            val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

            // Add 1 because we want inclusive count (e.g., Oct 13 to Oct 16 = 4 days)
            val numberOfDays = diffInDays + 1

            android.util.Log.d("RoamMate", "Calculated days: $startDate to $endDate = $numberOfDays days")

            // Return at least 1 day
            return maxOf(1, numberOfDays)
        } catch (e: Exception) {
            android.util.Log.e("RoamMate", "Error calculating number of days", e)
            return 1 // Default to 1 day on error
        }
    }

    /**
     * Convert UI interest strings to backend POICategory enums
     */
    private fun convertInterestsToPOICategories(interests: List<String>): List<com.group5.roammate.logic.models.POICategory> {
        return interests.mapNotNull { interest ->
            when (interest.lowercase()) {
                "museums" -> com.group5.roammate.logic.models.POICategory.MUSEUM
                "parks" -> com.group5.roammate.logic.models.POICategory.PARK
                "food", "restaurants" -> com.group5.roammate.logic.models.POICategory.RESTAURANT
                "shopping" -> com.group5.roammate.logic.models.POICategory.SHOPPING
                "entertainment" -> com.group5.roammate.logic.models.POICategory.ENTERTAINMENT
                "historical" -> com.group5.roammate.logic.models.POICategory.HISTORICAL
                "nature" -> com.group5.roammate.logic.models.POICategory.NATURE
                "beach", "beaches" -> com.group5.roammate.logic.models.POICategory.BEACH
                "sports" -> com.group5.roammate.logic.models.POICategory.SPORTS
                "cultural" -> com.group5.roammate.logic.models.POICategory.CULTURAL
                "nightlife" -> com.group5.roammate.logic.models.POICategory.NIGHTLIFE
                "cafes", "coffee" -> com.group5.roammate.logic.models.POICategory.CAFE
                "landmarks" -> com.group5.roammate.logic.models.POICategory.LANDMARK
                "scenic spots", "viewpoints" -> com.group5.roammate.logic.models.POICategory.SCENIC_SPOT
                else -> null // Ignore unknown interests
            }
        }
    }

    /**
     * Convert UI transport mode string to backend TransportMode enum
     */
    private fun convertTransportMode(transportMode: String): com.group5.roammate.logic.models.TransportMode {
        return when (transportMode.lowercase()) {
            "walking", "walk" -> com.group5.roammate.logic.models.TransportMode.WALKING
            "public transit", "transit", "public" -> com.group5.roammate.logic.models.TransportMode.PUBLIC_TRANSIT
            "driving", "car" -> com.group5.roammate.logic.models.TransportMode.DRIVING
            "cycling", "bike" -> com.group5.roammate.logic.models.TransportMode.CYCLING
            else -> com.group5.roammate.logic.models.TransportMode.WALKING // Default to walking
        }
    }

    /**
     * Convert place names to backend PinnedPOI objects
     */
    private fun convertToPinnedPOIs(
        placeNames: List<String>,
        destination: String
    ): List<com.group5.roammate.logic.models.PinnedPOI> {
        if (placeNames.isEmpty()) return emptyList()

        // Get all POIs for the destination (try with includeFillers = true to get more POIs)
        val allPOIs = tripPlannerEngine.getPOIsForDestination(
            destination = destination,
            includeFillers = true
        )

        android.util.Log.d("RoamMate", "convertToPinnedPOIs: destination='$destination', total POIs=${allPOIs.size}")
        android.util.Log.d("RoamMate", "Looking for places: ${placeNames.joinToString(", ")}")

        // If no POIs found for exact destination match, try getting ALL POIs from repository
        val poisToSearch = if (allPOIs.isEmpty()) {
            android.util.Log.w("RoamMate", "No POIs found for destination '$destination', trying all cities...")
            // Try common city name variations
            val cityVariations = listOf(destination, "Melbourne", "Melbourne CBD", "M2")
            cityVariations.flatMap { city ->
                tripPlannerEngine.getPOIsForDestination(city, includeFillers = true)
            }.distinctBy { it.id }
        } else {
            allPOIs
        }

        android.util.Log.d("RoamMate", "Total POIs to search: ${poisToSearch.size}")

        // Find matching POIs by name
        return placeNames.mapNotNull { placeName ->
            android.util.Log.d("RoamMate", "Searching for: $placeName")

            val matchingPOI = poisToSearch.find { poi ->
                poi.name.equals(placeName, ignoreCase = true) ||
                poi.name.contains(placeName, ignoreCase = true)
            }

            if (matchingPOI != null) {
                android.util.Log.d("RoamMate", "✓ Found POI: ${matchingPOI.name} (id=${matchingPOI.id})")
                com.group5.roammate.logic.models.PinnedPOI(
                    poiId = matchingPOI.id,
                    dayNumber = 1, // Pin to first day by default
                    preferredStartTime = null // No specific time preference
                )
            } else {
                android.util.Log.w("RoamMate", "✗ Could not find POI: $placeName (searched ${poisToSearch.size} POIs)")
                null
            }
        }
    }

    /**
     * Convert backend Itinerary to UI TripTimelineStop list
     */
    private fun convertItineraryToTimelineStops(
        itinerary: com.group5.roammate.logic.models.Itinerary
    ): List<TripTimelineStop> {
        val allStops = mutableListOf<TripTimelineStop>()

        itinerary.days.forEach { day ->
            day.pois.forEach { itineraryPOI ->
                allStops.add(
                    TripTimelineStop(
                        time = itineraryPOI.startTime,
                        title = itineraryPOI.poi.name,
                        status = TripStopStatus.Upcoming, // Default status
                        hiddenTag = if (itineraryPOI.isFiller) "Hidden gem · nearby" else null
                    )
                )
            }
        }

        // Set first stop as current
        if (allStops.isNotEmpty()) {
            allStops[0] = allStops[0].copy(status = TripStopStatus.Current)
        }

        return allStops
    }

    // Store the latest generated itinerary for summary generation
    private var currentItinerary: com.group5.roammate.logic.models.Itinerary? = null

    // Store weather adjustment options for applying to currentItinerary
    private var weatherAdjustmentOptions: com.group5.roammate.logic.engine.RainyDayOptions? = null

    private fun uiLoadTripSummary(): TripSummary {
        // function: uiLoadTripSummary
        // owner: Zewen
        // output: short trip summary for Trip screen

        val itinerary = currentItinerary
        if (itinerary == null) {
            // Fallback if no itinerary generated yet
            return TripSummary(
                title = "3 days in Melbourne",
                description = "Day 1: Melbourne Museum, State Library and ACMI. Day 2: gardens and markets. Day 3: beach and riverside walk.",
            )
        }

        // Generate summary from real itinerary
        val dayDescriptions = itinerary.days.mapIndexed { index, day ->
            val topPOIs = day.pois.take(3).joinToString(", ") { it.poi.name }
            "Day ${index + 1}: $topPOIs"
        }

        val cityName = itinerary.days.firstOrNull()?.pois?.firstOrNull()?.poi?.city ?: "Melbourne"

        return TripSummary(
            title = "${itinerary.days.size} days in $cityName",
            description = dayDescriptions.joinToString(". "),
        )
    }

    private fun uiLoadTripDays(
        currentDayStops: List<TripTimelineStop>,
    ): List<TripDayPlan> {
        // function: uiLoadTripDays
        // owner: Zewen
        // output: full multi-day itinerary for Trip screen

        val itinerary = currentItinerary
        if (itinerary == null || itinerary.days.isEmpty()) {
            // Fallback to single day if no itinerary generated yet
            return listOf(
                TripDayPlan(
                    dayTitle = "Day 1",
                    dateText = "Today",
                    stops = currentDayStops,
                )
            )
        }

        // Convert each DayItinerary to TripDayPlan
        return itinerary.days.mapIndexed { index, dayItinerary ->
            // Convert POIs to TripTimelineStop list
            val dayStops = dayItinerary.pois.map { itineraryPOI ->
                TripTimelineStop(
                    time = itineraryPOI.startTime,
                    title = itineraryPOI.poi.name,
                    status = if (index == 0 && dayItinerary.pois.firstOrNull() == itineraryPOI) {
                        TripStopStatus.Current // First stop of first day is current
                    } else {
                        TripStopStatus.Upcoming
                    },
                    hiddenTag = if (itineraryPOI.isFiller) "Hidden gem · nearby" else null
                )
            }

            // Format date text
            val dateText = when (index) {
                0 -> "Today"
                1 -> "Tomorrow"
                itinerary.days.size - 1 -> "Final day"
                else -> formatDayDate(dayItinerary.date)
            }

            TripDayPlan(
                dayTitle = "Day ${dayItinerary.dayNumber}",
                dateText = dateText,
                stops = dayStops,
            )
        }
    }

    /**
     * Format backend date "yyyy-MM-dd" to readable format like "Oct 15"
     */
    private fun formatDayDate(backendDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("MMM d", Locale.ENGLISH)
            val date = inputFormat.parse(backendDate)
            if (date != null) {
                outputFormat.format(date)
            } else {
                backendDate
            }
        } catch (e: Exception) {
            android.util.Log.w("RoamMate", "Error formatting date: $backendDate", e)
            backendDate
        }
    }

    private fun uiUpdateItineraryAfterEdit(
        editedStops: List<TripTimelineStop>,
    ): List<TripTimelineStop> {
        // function: uiUpdateItineraryAfterEdit
        // owner: Zewen
        // storage provider: Yuxiang saves final itinerary
        // input: added/removed/reordered stops
        // output: recalculated itinerary

        try {
            // If no backend itinerary exists, use frontend fallback
            val itinerary = currentItinerary
            if (itinerary == null || itinerary.days.isEmpty()) {
                return rebalanceTripStopsAfterManualEdit(editedStops)
            }

            // TODO(Zewen): For now, use frontend rebalancing
            // Full backend integration would:
            // 1. Convert editedStops back to ItineraryPOI list
            // 2. Call ItineraryEditService.reoptimizeDay()
            // 3. Convert result back to TripTimelineStop list

            return rebalanceTripStopsAfterManualEdit(editedStops)

        } catch (e: Exception) {
            android.util.Log.e("RoamMate", "Error updating itinerary after edit", e)
            return rebalanceTripStopsAfterManualEdit(editedStops)
        }
    }

    private fun uiAdjustItineraryForWeather(
        reason: String,
    ): List<AdjustItineraryPlan> {
        // function: uiAdjustItineraryForWeather
        // owner: Zewen
        // input provider: LeYan weather forecast
        // input: weather reason
        // output: alternative itinerary plans

        android.util.Log.d("WeatherAdjust", "START weather adjustment algorithm")
        android.util.Log.d("WeatherAdjust", "Reason: $reason")

        try {
            val itinerary = currentItinerary
            if (itinerary == null || itinerary.days.isEmpty()) {
                // No itinerary to adjust, return sample data
                android.util.Log.w("WeatherAdjust", "FALLBACK currentItinerary is null or empty")
                android.util.Log.w("WeatherAdjust", "itinerary null: ${itinerary == null}")
                if (itinerary != null) {
                    android.util.Log.w("WeatherAdjust", "days empty: ${itinerary.days.isEmpty()}")
                }
                return sampleAdjustedItineraryPlans()
            }

            android.util.Log.d("WeatherAdjust", "SUCCESS Found valid itinerary, executing real algorithm")
            android.util.Log.d("WeatherAdjust", "Days: ${itinerary.days.size}")
            android.util.Log.d("WeatherAdjust", "POIs in day 1: ${itinerary.days.firstOrNull()?.pois?.size ?: 0}")

            // Get current day (assume day 0 for now)
            val currentDay = itinerary.days.firstOrNull() ?: return sampleAdjustedItineraryPlans()

            // Get user profile from stored itinerary context
            val userProfile = com.group5.roammate.logic.models.UserProfile(
                interests = listOf(com.group5.roammate.logic.models.POICategory.MUSEUM),
                budgetPreference = com.group5.roammate.logic.models.BudgetLevel.MEDIUM,
                transportMode = com.group5.roammate.logic.models.TransportMode.WALKING
            )

            // Create context payload for rainy weather
            val contextPayload = com.group5.roammate.logic.models.ContextPayload(
                currentLocation = com.group5.roammate.logic.models.Coordinates(-37.8136, 144.9631),
                currentTime = "${currentDay.date}T${currentDay.pois.firstOrNull()?.startTime ?: "09:00"}:00",
                weatherStatus = com.group5.roammate.logic.models.WeatherStatus.RAINY,
                currentWeather = com.group5.roammate.logic.models.WeatherStatus.RAINY,
                itineraryProgress = com.group5.roammate.logic.models.ItineraryProgress(
                    completedPOIs = emptyList(),
                    delayMinutes = 0
                )
            )

            // Initialize weather replanning service
            val weatherService = com.group5.roammate.logic.engine.WeatherReplanningService(
                userProfile = userProfile,
                travelDurationMatrix = null
            )

            // Get all POIs for destination
            val destination = currentDay.pois.firstOrNull()?.poi?.city ?: "Melbourne"
            val allPOIs = tripPlannerEngine.getPOIsForDestination(
                destination = destination,
                includeFillers = false
            )

            // Calculate available time (assume 9 hours = 540 minutes)
            val availableTimeMinutes = 540

            // Get rainy day options
            android.util.Log.d("WeatherAdjust", "Calling WeatherReplanningService.handleRainyDayScenario")
            android.util.Log.d("WeatherAdjust", "Available POIs: ${allPOIs.size}")
            android.util.Log.d("WeatherAdjust", "Indoor POIs: ${allPOIs.count { it.environment == com.group5.roammate.logic.models.Environment.INDOOR }}")

            val rainyOptions = weatherService.handleRainyDayScenario(
                currentItinerary = currentDay,
                contextPayload = contextPayload,
                allPOIs = allPOIs,
                startLocation = com.group5.roammate.logic.models.Coordinates(-37.8136, 144.9631),
                availableTimeMinutes = availableTimeMinutes
            )

            android.util.Log.d("WeatherAdjust", "Backend returned options:")
            android.util.Log.d("WeatherAdjust", "  Experience First: ${rainyOptions.experienceFirst.pois.size} POIs")
            android.util.Log.d("WeatherAdjust", "  Efficiency First: ${rainyOptions.efficiencyFirst.pois.size} POIs")
            android.util.Log.d("WeatherAdjust", "  Trigger reason: ${rainyOptions.triggerReason}")

            // Save weather adjustment options for later application
            weatherAdjustmentOptions = rainyOptions

            // Convert backend options to UI format
            val result = convertRainyOptionsToAdjustmentPlans(rainyOptions, currentDay)
            android.util.Log.d("WeatherAdjust", "COMPLETE Converted to ${result.size} UI plans")
            return result

        } catch (e: Exception) {
            android.util.Log.e("RoamMate", "Error adjusting itinerary for weather", e)
            return sampleAdjustedItineraryPlans()
        }
    }

    /**
     * Convert backend RainyDayOptions to UI AdjustItineraryPlan list
     */
    private fun convertRainyOptionsToAdjustmentPlans(
        rainyOptions: com.group5.roammate.logic.engine.RainyDayOptions,
        originalDay: com.group5.roammate.logic.models.DayItinerary
    ): List<AdjustItineraryPlan> {
        // Option 1: Experience First (maximizes total EV)
        val experienceFirstPlan = AdjustItineraryPlan(
            reasonLabel = "rain",
            reasonDescription = rainyOptions.triggerReason,
            stops = rainyOptions.experienceFirst.pois.map { itineraryPOI ->
                val wasInOriginal = originalDay.pois.any { it.poi.id == itineraryPOI.poi.id }
                AdjustItineraryStop(
                    time = itineraryPOI.startTime,
                    title = itineraryPOI.poi.name,
                    changeLabel = if (wasInOriginal) "Kept" else "New · indoor",
                    changeTone = if (wasInOriginal) AdjustChangeTone.Neutral else AdjustChangeTone.Positive
                )
            },
            removedStop = findRemovedOutdoorStop(originalDay, rainyOptions.experienceFirst)
        )

        // Option 2: Efficiency First (maximizes EV/Cost ratio)
        val efficiencyFirstPlan = AdjustItineraryPlan(
            reasonLabel = "rain",
            reasonDescription = rainyOptions.triggerReason,
            stops = rainyOptions.efficiencyFirst.pois.map { itineraryPOI ->
                val wasInOriginal = originalDay.pois.any { it.poi.id == itineraryPOI.poi.id }
                AdjustItineraryStop(
                    time = itineraryPOI.startTime,
                    title = itineraryPOI.poi.name,
                    changeLabel = if (wasInOriginal) "Kept" else "New · indoor",
                    changeTone = if (wasInOriginal) AdjustChangeTone.Neutral else AdjustChangeTone.Positive
                )
            },
            removedStop = findRemovedOutdoorStop(originalDay, rainyOptions.efficiencyFirst)
        )

        return listOf(experienceFirstPlan, efficiencyFirstPlan)
    }

    /**
     * Find the first outdoor POI that was removed
     */
    private fun findRemovedOutdoorStop(
        originalDay: com.group5.roammate.logic.models.DayItinerary,
        newDay: com.group5.roammate.logic.models.DayItinerary
    ): AdjustRemovedStop? {
        val newPoiIds = newDay.pois.map { it.poi.id }.toSet()

        val removedOutdoorPOI = originalDay.pois.find { itineraryPOI ->
            itineraryPOI.poi.id !in newPoiIds &&
            itineraryPOI.poi.environment == com.group5.roammate.logic.models.Environment.OUTDOOR
        }

        return if (removedOutdoorPOI != null) {
            AdjustRemovedStop(
                title = removedOutdoorPOI.poi.name,
                reason = "outdoor · rain"
            )
        } else {
            null
        }
    }

    /**
     * Apply weather adjustment plan to currentItinerary
     * @param planIndex 0 = Experience First, 1 = Efficiency First
     */
    private fun applyWeatherAdjustmentToItinerary(planIndex: Int) {
        val options = weatherAdjustmentOptions
        val itinerary = currentItinerary

        if (options == null || itinerary == null || itinerary.days.isEmpty()) {
            android.util.Log.w("WeatherAdjust", "Cannot apply adjustment: options or itinerary is null")
            return
        }

        // Select the chosen plan
        val selectedDayItinerary = when (planIndex) {
            0 -> options.experienceFirst
            1 -> options.efficiencyFirst
            else -> {
                android.util.Log.w("WeatherAdjust", "Invalid plan index: $planIndex")
                return
            }
        }

        // Update Day 1 with the selected plan
        val updatedDays = itinerary.days.toMutableList()
        updatedDays[0] = selectedDayItinerary

        // Create updated itinerary
        currentItinerary = itinerary.copy(
            days = updatedDays,
            totalExpectedValue = updatedDays.sumOf { day ->
                day.pois.sumOf { it.expectedValue }
            }
        )

        android.util.Log.d("WeatherAdjust", "Applied plan $planIndex to currentItinerary")
        android.util.Log.d("WeatherAdjust", "Updated Day 1 POIs: ${selectedDayItinerary.pois.size}")
    }

    // ---- LeYan: weather ----
    // demo onlu
    private fun startWeatherDemoScenario() {

        lifecycleScope.launch {

            android.util.Log.d(
                "WEATHER_DEMO",
                "Demo started. Fake forecast will arrive in 5 minutes."
            )

            // Wait 1 minutes after app launch.
            delay(60 * 1000L)

            val now = LocalDateTime.now()

            // Example:
            // If the fake forecast arrives at 1:05 PM,
            // rain will be predicted for 2:00 PM.
            val rainTime =
                now
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .plusHours(1)


            // Fake current weather:
            // it is still sunny NOW.
            val fakeCurrentWeather =
                CurrentWeather(
                    status = WeatherStatus.SUNNY,
                    temperature = 22.0,
                )


            // Fake forecast:
            // sunny now -> rain next hour.
            val fakeHourlyForecast =
                listOf(
                    HourlyWeather(
                        time = now,
                        status = WeatherStatus.SUNNY,
                        precipitationType = null,
                        precipitationTotal = 0.0,
                    ),

                    HourlyWeather(
                        time = rainTime,
                        status = WeatherStatus.RAINY,
                        precipitationType = "rain",
                        precipitationTotal = 2.0,
                    ),
                )


            android.util.Log.d(
                "WEATHER_DEMO",
                "Fake forecast received: rain expected at $rainTime"
            )


            // IMPORTANT:
            // The fake data goes through the REAL context detector.
            val event =
                weatherContextDetector.detectUpcomingRain(
                    current = fakeCurrentWeather,
                    hourly = fakeHourlyForecast,
                    hoursAhead = 3,
                    now = now,
                )


            if (event != null) {

                android.util.Log.d(
                    "WEATHER_DEMO",
                    "Weather context event generated: $event"
                )

                uiHandleWeatherContextEvent(event)
            }
        }
    }

    private fun uiHandleWeatherContextEvent(
        event: WeatherContextEvent,
    ) {
        when (event) {

            is WeatherContextEvent.SuddenRain -> {

                android.util.Log.d(
                    "WEATHER_CONTEXT",
                    "Sudden rain detected: " +
                            "${event.from} -> ${event.to}"
                )

                runOnUiThread {
                    weatherSuggestionState.value =
                        HomeSmartSuggestion(
                            label = "Rain",
                            message = "It has started raining. Tap to view your options."
                        )
                }
            }


            is WeatherContextEvent.UpcomingRain -> {

                android.util.Log.d(
                    "WEATHER_CONTEXT",
                    "Rain expected at ${event.expectedStartTime}"
                )

                val timeText =
                    event.expectedStartTime
                        .format(
                            DateTimeFormatter.ofPattern("h:mm a")
                        )

                runOnUiThread {
                    weatherSuggestionState.value =
                        HomeSmartSuggestion(
                            label = "Rain",
                            message =
                                "Rain is expected around $timeText. Tap to view your options."
                        )
                }
            }
        }
    }

    private fun uiLoadPlannedWeather(
        request: PlanMyTripRequest,
        onResult: (List<DailyWeatherForecast>?) -> Unit,
    ) {

        val startDate =
            try {
                LocalDate.parse(
                    request.startDate,
                    planDateFormatter
                )
            } catch (e: Exception) {
                onResult(null)
                return
            }

        val endDate =
            try {
                LocalDate.parse(
                    request.endDate,
                    planDateFormatter
                )
            } catch (e: Exception) {
                onResult(null)
                return
            }

        lifecycleScope.launch {

            val forecasts =
                weatherService.getPlannedWeather(
                    destination = request.destination,
                    startDate = startDate,
                    endDate = endDate
                )

            if (forecasts != null) {
                latestPlannedWeather = forecasts

                android.util.Log.d(
                    "PLANNED_WEATHER",
                    "Loaded ${forecasts.size} planned weather forecasts"
                )
            }

            onResult(forecasts)
        }
    }

    private fun uiIsWeatherAvailableForDate(
        dateText: String,
    ): Boolean {

        val date =
            try {
                LocalDate.parse(
                    dateText,
                    planDateFormatter
                )
            } catch (e: Exception) {
                return false
            }

        return latestPlannedWeather
            .firstOrNull {
                it.date == date
            }
            ?.status != null
    }

    // ---- Xiajie: pet ----

    private fun uiPetStatusCard(): String {
        // function: uiPetStatusCard
        // owner: Xiajie
        // UI consumer: Yufei
        // output: pet card source label
        // TODO(Xiajie): Home should read pet state from pet module.
        return "Pet module"
    }

    // ---- Yufei: UI navigation helpers ----

    private fun uiLoadingState(isLoading: Boolean): Boolean {
        // function: uiLoadingState
        // owner: Yufei
        // input: backend request running
        // output: show loading UI
        return isLoading
    }

    private fun List<TripTimelineStop>.advanceCurrentStopForDemo(): List<TripTimelineStop> {
        // Demo progress
        // TODO(Zewen): Replace with real progress logic; input provider is Alex GPS.
        val currentIndex = indexOfFirst { stop ->
            stop.status == TripStopStatus.Current
        }

        if (currentIndex == -1 || currentIndex >= lastIndex) return this

        val nextIndex = currentIndex + 1
        return mapIndexed { index, stop ->
            when {
                index < nextIndex -> stop.copy(status = TripStopStatus.Done)
                index == nextIndex -> stop.copy(status = TripStopStatus.Current)
                else -> stop.copy(status = TripStopStatus.Upcoming)
            }
        }
    }

    private fun <T> List<T>.rotateLeft(steps: Int): List<T> {
        // Demo reorder
        if (isEmpty()) return this

        val offset = ((steps % size) + size) % size
        return drop(offset) + take(offset)
    }

    private fun rebalanceTripStopsAfterManualEdit(
        stops: List<TripTimelineStop>,
    ): List<TripTimelineStop> {
        // TODO: 这是前端临时演示逻辑；之后删除/新增后应调用 Zewen 的后端重新生成行程。
        var hasCurrentStop = false

        return stops.map { stop ->
            when {
                stop.status == TripStopStatus.Done -> stop
                !hasCurrentStop -> {
                    hasCurrentStop = true
                    stop.copy(status = TripStopStatus.Current)
                }
                else -> stop.copy(status = TripStopStatus.Upcoming)
            }
        }
    }

    private fun List<TripTimelineStop>.toHomeTripStops(): List<HomeTripStop> {
        // Home 只显示当前附近的几个站点，避免把完整行程挤在首页。
        val currentStopIndex = indexOfFirst { stop ->
            stop.status == TripStopStatus.Current
        }
        val firstVisibleIndex = if (currentStopIndex <= 0) {
            0
        } else {
            currentStopIndex - 1
        }

        return drop(firstVisibleIndex)
            .take(3)
            .map { stop ->
                HomeTripStop(
                    time = stop.time,
                    title = stop.title,
                    subtitle = when (stop.status) {
                        TripStopStatus.Done -> ""
                        TripStopStatus.Current -> "Now"
                        TripStopStatus.Upcoming -> "Next"
                    },
                    icon = stop.title.firstOrNull()?.uppercase() ?: "",
                    status = when (stop.status) {
                        TripStopStatus.Done -> HomeTripStopStatus.Done
                        TripStopStatus.Current -> HomeTripStopStatus.Current
                        TripStopStatus.Upcoming -> HomeTripStopStatus.Next
                    },
                )
            }
    }

    private fun List<AddStopPlace>.addUniquePlace(
        place: AddStopPlace,
    ): List<AddStopPlace> {
        return if (any { existingPlace -> existingPlace.name == place.name }) {
            this
        } else {
            this + place
        }
    }

    private fun List<TripTimelineStop>.withAddedTemporaryStop(
        place: AddStopPlace,
    ): List<TripTimelineStop> {
        // TODO: 这是前端临时添加逻辑；之后应由 Zewen 决定新地点插入位置和时间。
        val alreadyExists = any { stop ->
            stop.title == place.name
        }
        if (alreadyExists) return this

        return this + TripTimelineStop(
            time = nextTemporaryStopTime(lastOrNull()?.time),
            title = place.name,
            status = TripStopStatus.Upcoming,
        )
    }

    private fun List<TripTimelineStop>.withAddedTemporaryAttraction(
        attraction: AttractionDetail,
    ): List<TripTimelineStop> {
        // TODO: 这是前端临时添加逻辑；之后由 Zewen 根据位置/时间/营业时间插入合适位置。
        val alreadyExists = any { stop ->
            stop.title == attraction.name
        }
        if (alreadyExists) return this

        return this + TripTimelineStop(
            time = nextTemporaryStopTime(lastOrNull()?.time),
            title = attraction.name,
            status = TripStopStatus.Upcoming,
        )
    }

    private fun nextTemporaryStopTime(
        lastStopTime: String?,
    ): String {
        val nextHour = lastStopTime
            ?.substringBefore(":")
            ?.toIntOrNull()
            ?.plus(2)
            ?: 10

        return "%02d:00".format(nextHour.coerceAtMost(22))
    }

    private fun melbournePopularAddStopPlaces(): List<AddStopPlace> {
        // TODO: 之后替换为 LeYan 的景点搜索/景点类型数据 + Alex 的距离定位数据。
        return listOf(
            AddStopPlace(
                name = "Melbourne Museum",
                environmentType = "Indoor",
            ),
            AddStopPlace(
                name = "National Gallery of Victoria",
                environmentType = "Indoor",
            ),
            AddStopPlace(
                name = "State Library Victoria",
                environmentType = "Indoor",
            ),
            AddStopPlace(
                name = "ACMI",
                environmentType = "Indoor",
            ),
            AddStopPlace(
                name = "Royal Botanic Gardens",
                environmentType = "Outdoor",
            ),
            AddStopPlace(
                name = "Queen Victoria Market",
                environmentType = "Outdoor",
            ),
        )
    }

    private fun sampleNearbyExplorePlaces(): List<ExplorePlace> {
        // TODO: 之后这里替换为真实附近景点列表。
        // Alex 提供当前 GPS，LeYan/Leyan 提供景点坐标/类型，再由前端或后端计算距离后显示。
        return listOf(
            ExplorePlace(
                name = "Melbourne Museum",
                distanceText = "0.8 km",
                category = ExplorePlaceCategory.Indoor,
                environmentLabel = "Indoor",
                tagText = "Good for rain",
                mapX = 0.26f,
                mapY = 0.34f,
            ),
            ExplorePlace(
                name = "State Library Victoria",
                distanceText = "1.1 km",
                category = ExplorePlaceCategory.Indoor,
                environmentLabel = "Indoor",
                tagText = "Popular nearby",
                mapX = 0.62f,
                mapY = 0.28f,
            ),
            ExplorePlace(
                name = "ACMI",
                distanceText = "1.0 km",
                category = ExplorePlaceCategory.Indoor,
                environmentLabel = "Indoor",
                tagText = "Good for rain",
                mapX = 0.58f,
                mapY = 0.68f,
            ),
            ExplorePlace(
                name = "Royal Botanic Gardens",
                distanceText = "2.4 km",
                category = ExplorePlaceCategory.Outdoor,
                environmentLabel = "Outdoor",
                tagText = "Best on sunny days",
                mapX = 0.72f,
                mapY = 0.64f,
            ),
            ExplorePlace(
                name = "Carlton Gardens",
                distanceText = "0.6 km",
                category = ExplorePlaceCategory.Outdoor,
                environmentLabel = "Outdoor",
                tagText = "Close to you",
                mapX = 0.38f,
                mapY = 0.26f,
            ),
            ExplorePlace(
                name = "Fitzroy Gardens",
                distanceText = "1.7 km",
                category = ExplorePlaceCategory.Outdoor,
                environmentLabel = "Outdoor",
                tagText = "Nice walking route",
                mapX = 0.76f,
                mapY = 0.40f,
            ),
            ExplorePlace(
                name = "Lune Croissanterie",
                distanceText = "0.9 km",
                category = ExplorePlaceCategory.Cafes,
                environmentLabel = "Indoor",
                tagText = "Popular cafe",
                mapX = 0.42f,
                mapY = 0.72f,
            ),
            ExplorePlace(
                name = "Market Lane Coffee",
                distanceText = "0.5 km",
                category = ExplorePlaceCategory.Cafes,
                environmentLabel = "Indoor",
                tagText = "Coffee nearby",
                mapX = 0.32f,
                mapY = 0.58f,
            ),
            ExplorePlace(
                name = "Operator25",
                distanceText = "1.2 km",
                category = ExplorePlaceCategory.Food,
                environmentLabel = "Indoor",
                tagText = "Brunch spot",
                mapX = 0.50f,
                mapY = 0.78f,
            ),
            ExplorePlace(
                name = "Queen Victoria Market",
                distanceText = "1.6 km",
                category = ExplorePlaceCategory.Food,
                environmentLabel = "Covered",
                tagText = "Local food",
                mapX = 0.66f,
                mapY = 0.58f,
            ),
        )
    }

    private fun ExplorePlace.toAttractionDetail(): AttractionDetail {
        // Explore 只拿到列表卡片需要的轻量数据；详情页其余字段之后从 Leyan 的景点详情接口补齐。
        return sampleAttractionDetail(name).copy(
            distanceText = distanceText,
            environmentLabel = environmentLabel,
            weatherTag = tagText,
        )
    }

    private fun sampleAttractionDetail(
        name: String,
    ): AttractionDetail {
        // TODO: 之后替换为真实景点详情：
        // Leyan 提供名称、图片、室内外、天气适配、营业时间、官网；
        // Alex sensor/GPS 提供用户当前位置，再计算 distanceText。
        val defaultWeeklyHours = listOf(
            AttractionOpeningHours("Mon", "10:00 am - 5:00 pm"),
            AttractionOpeningHours("Tue", "10:00 am - 5:00 pm"),
            AttractionOpeningHours("Wed", "10:00 am - 5:00 pm"),
            AttractionOpeningHours("Thu", "10:00 am - 5:00 pm"),
            AttractionOpeningHours("Fri", "10:00 am - 5:00 pm"),
            AttractionOpeningHours("Sat", "10:00 am - 5:00 pm"),
            AttractionOpeningHours("Sun", "10:00 am - 5:00 pm"),
        )

        return when (name) {
            "Melbourne Museum" -> AttractionDetail(
                name = "Melbourne Museum",
                distanceText = "0.8 km",
                environmentLabel = "Indoor",
                weatherTag = "Good for rain",
                todayHours = AttractionOpeningHours("Today", "10:00 am - 5:00 pm"),
                weeklyHours = defaultWeeklyHours,
                websiteUrl = "https://museumsvictoria.com.au/melbournemuseum/",
                photoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2b/Melbourne_Museum_exterior.jpg/1280px-Melbourne_Museum_exterior.jpg",
                imageSymbol = "M",
            )

            "State Library Victoria", "State Library" -> AttractionDetail(
                name = "State Library Victoria",
                distanceText = "1.1 km",
                environmentLabel = "Indoor",
                weatherTag = "Good for rain",
                todayHours = AttractionOpeningHours("Today", "10:00 am - 6:00 pm"),
                weeklyHours = defaultWeeklyHours.map { hours ->
                    hours.copy(timeRange = "10:00 am - 6:00 pm")
                },
                websiteUrl = "https://www.slv.vic.gov.au/",
                imageSymbol = "S",
            )

            "ACMI" -> AttractionDetail(
                name = "ACMI",
                distanceText = "1.0 km",
                environmentLabel = "Indoor",
                weatherTag = "Good for rain",
                todayHours = AttractionOpeningHours("Today", "10:00 am - 5:00 pm"),
                weeklyHours = defaultWeeklyHours,
                websiteUrl = "https://www.acmi.net.au/",
                imageSymbol = "A",
            )

            "Royal Botanic Gardens" -> AttractionDetail(
                name = "Royal Botanic Gardens",
                distanceText = "2.4 km",
                environmentLabel = "Outdoor",
                weatherTag = "Best on sunny days",
                todayHours = AttractionOpeningHours("Today", "7:30 am - 5:30 pm"),
                weeklyHours = defaultWeeklyHours.map { hours ->
                    hours.copy(timeRange = "7:30 am - 5:30 pm")
                },
                websiteUrl = "https://www.rbg.vic.gov.au/melbourne-gardens/",
                imageSymbol = "R",
            )

            "Pidapipo Gelato" -> AttractionDetail(
                name = "Pidapipo Gelato",
                distanceText = "0.3 km",
                environmentLabel = "Indoor",
                weatherTag = "Hidden gem",
                // Leyan 不一定每个地点都有营业时间；这里用 null 演示 UI fallback。
                todayHours = null,
                weeklyHours = emptyList(),
                websiteUrl = "https://pidapipo.com/",
                imageSymbol = "P",
            )

            else -> AttractionDetail(
                name = name,
                distanceText = "1.0 km",
                environmentLabel = "Indoor",
                weatherTag = null,
                todayHours = null,
                weeklyHours = emptyList(),
                websiteUrl = null,
                imageSymbol = name.firstOrNull()?.uppercase() ?: "A",
            )
        }
    }

    private fun sampleAdjustedItineraryPlans(): List<AdjustItineraryPlan> {
        // TODO: 之后这里由 Zewen 后端返回。
        // UI 只需要拿到两个 AdjustItineraryPlan：第一个方案和 regenerate 后的第二个方案。
        return listOf(
            AdjustItineraryPlan(
                reasonLabel = "rain",
                reasonDescription = "Heavy rain 2-4 PM - here's a rewritten plan for today:",
                stops = listOf(
                    AdjustItineraryStop(
                        time = "10:00",
                        title = "Melbourne Museum",
                        changeLabel = "Kept",
                        changeTone = AdjustChangeTone.Neutral,
                    ),
                    AdjustItineraryStop(
                        time = "12:30",
                        title = "State Library",
                        changeLabel = "Moved earlier",
                        changeTone = AdjustChangeTone.Positive,
                    ),
                    AdjustItineraryStop(
                        time = "14:00",
                        title = "ACMI",
                        changeLabel = "New · indoor",
                        changeTone = AdjustChangeTone.Positive,
                    ),
                    AdjustItineraryStop(
                        time = "16:30",
                        title = "Queen Victoria Market",
                        changeLabel = "New · covered",
                        changeTone = AdjustChangeTone.Positive,
                    ),
                ),
                removedStop = AdjustRemovedStop(
                    title = "Royal Botanic Gardens",
                    reason = "outdoor · rain",
                ),
            ),
            AdjustItineraryPlan(
                reasonLabel = "rain",
                reasonDescription = "Heavy rain 2-4 PM - here's another indoor-friendly option:",
                stops = listOf(
                    AdjustItineraryStop(
                        time = "10:00",
                        title = "Melbourne Museum",
                        changeLabel = "Kept",
                        changeTone = AdjustChangeTone.Neutral,
                    ),
                    AdjustItineraryStop(
                        time = "12:00",
                        title = "ACMI",
                        changeLabel = "Moved earlier",
                        changeTone = AdjustChangeTone.Positive,
                    ),
                    AdjustItineraryStop(
                        time = "13:30",
                        title = "State Library",
                        changeLabel = "New · indoor",
                        changeTone = AdjustChangeTone.Positive,
                    ),
                    AdjustItineraryStop(
                        time = "15:30",
                        title = "National Gallery of Victoria",
                        changeLabel = "New · indoor",
                        changeTone = AdjustChangeTone.Positive,
                    ),
                ),
                removedStop = AdjustRemovedStop(
                    title = "Royal Botanic Gardens",
                    reason = "outdoor · rain",
                ),
            ),
        )
    }

    private fun AdjustItineraryPlan.toTripTimelineStops(): List<TripTimelineStop> {
        // TODO: 之后如果 Zewen 直接返回 TripTimelineStop 或统一 itinerary model，这里可以删除。
        return stops.mapIndexed { index, stop ->
            TripTimelineStop(
                time = stop.time,
                title = stop.title,
                status = if (index == 0) {
                    TripStopStatus.Current
                } else {
                    TripStopStatus.Upcoming
                },
            )
        }
    }

    private fun openExternalMap(placeName: String) {
        // function: uiOpenExternalMap
        // owner: Yufei
        // 外部地图统一入口：Home 提醒条和 Trip 的 Start navigation 都调用这里。
        val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(placeName)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)

        runCatching {
            startActivity(mapIntent)
        }.onFailure {
            Toast.makeText(this, "No map app available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternalWebsite(websiteUrl: String?) {
        if (websiteUrl == null) {
            Toast.makeText(this, "Official website is not available", Toast.LENGTH_SHORT).show()
            return
        }

        val websiteIntent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))

        runCatching {
            startActivity(websiteIntent)
        }.onFailure {
            Toast.makeText(this, "No browser app available", Toast.LENGTH_SHORT).show()
        }
    }
}

// Location adapter for Alex; defaults are explicitly demonstration data.
private data class SensorLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val areaName: String,
    val isSample: Boolean = true,
)

private enum class AuthScreen {
    Login,
    CreateAccount,
    Home,
    Trip,
    Explore,
    AttractionDetail,
    AdjustItinerary,
    EditItinerary,
    PlanAddStop,
    EditAddStop,
    PlanMyTrip,
    PlanTripInterests,
    Pet,
    PetCamera,
    Profile,
    SavedTrips,
    EditProfile,
    ProfileInterests,
    SensorTest,
}
