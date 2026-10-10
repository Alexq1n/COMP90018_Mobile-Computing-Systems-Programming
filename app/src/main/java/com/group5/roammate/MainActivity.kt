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
import com.group5.roammate.ui.pet.rememberPetEnvironment
import com.group5.roammate.pet.PetPreferences
import com.group5.roammate.pet.PetStateEngine
import com.group5.roammate.pet.PetTripContext
import com.group5.roammate.pet.PetWeatherSnapshot
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
import com.group5.roammate.ui.screens.TripSummary
import com.group5.roammate.ui.screens.TripTimelineStop
import com.group5.roammate.ui.screens.TripWeatherSummary
import com.group5.roammate.ui.sensor.SensorTestScreen
import com.group5.roammate.ui.pet.CompanionsScreen
import com.group5.roammate.ui.pet.PetCameraScreen
import com.group5.roammate.ui.theme.RoamMateTheme

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
import kotlinx.coroutines.launch

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


        // Subscribe to the shared repository.
        // Collection automatically stops/restarts with Activity lifecycle.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // GPS: Latest location
                launch {
                    sensorRepository.locationFlow.collect { location ->
                        if (location != null) {
//                            android.util.Log.d(
//                                "RoamMateSensor",
//                                "GPS: ${location.latitude}, ${location.longitude}"
//                            )
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
                // This is a temporary page status.
                var currentScreen by rememberSaveable { mutableStateOf(AuthScreen.SensorTest) }

                // Jie pet feature state: one koala, live weather gear and a persisted fashion choice.
                val petPreferences = remember { PetPreferences(applicationContext) }
                var petProfile by remember { mutableStateOf(petPreferences.loadProfile()) }
                val petEnvironment = rememberPetEnvironment()
                val petWeather = petEnvironment.weather
                val petState = PetStateEngine.buildUiState(petProfile, petWeather)

                // User name
                // TODO(Yuxiang, function: uiLoadUserProfile): Replace with Firebase user profile.
                var userName by rememberSaveable { mutableStateOf("Yufei") }

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
                    sensorRepository.locationFlow.collect { location ->
                        if (location != null) {
                            currentSensorLocation.value = SensorLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracyMeters = 0f,
                                areaName = "Current location",
                                isSample = false
                            )
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
                    sensorRepository.shakeEvents.collect {
//                        android.util.Log.d(
//                            "RoamMateSensor",
//                            "Shake detected!"
//                        )
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

                // Profile 入口使用：长期默认兴趣偏好。
                // TODO(Yuxiang, function: uiSaveProfileInterests): connect Firebase default interests, permanent save.
                var profileDefaultInterests by rememberSaveable {
                    mutableStateOf(listOf("Museums", "Parks", "Food"))
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

                // Saved trips
                // TODO(Yuxiang, function: uiLoadSavedTrips): Load saved trips from Firebase.
                // input provider: Zewen trip summary shape.
                val savedTrips = remember {
                    sampleSavedTrips()
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
                // TODO(Zewen, function: uiAdjustItineraryForWeather): Backend currently provides two candidate plans.
                var adjustPlanIndex by rememberSaveable { mutableStateOf(0) }

                // TODO(Zewen, function: uiAdjustItineraryForWeather): Replace with weather-based candidate plans.
                // input provider: LeYan weather and attraction suitability.
                val adjustedItineraryPlans = remember {
                    sampleAdjustedItineraryPlans()
                }

                when (currentScreen) {
                    AuthScreen.Login -> {
                        // After the user opens the app, they first see the Login page.
                        LoginScreen(
                            modifier = Modifier.fillMaxSize(),

                            // This will run when the user clicks "Log in".
                            // TODO(Yuxiang, function: uiLoginWithFirebase): connect Firebase login.
                            onLoginClick = { email, password ->
                                if (email.isBlank() || password.isBlank()) {
                                    Toast.makeText(
                                        this,
                                        "Please enter email and password",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    // 登录成功后进入 Home 页面。
                                    // TODO(Yuxiang, function: uiLoginWithFirebase): Navigate only after Firebase login success.
                                    currentScreen = AuthScreen.Home
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

                            // Received the name, email and password
                            // TODO(Yuxiang, function: uiCreateAccountWithFirebase): connect Firebase register.
                            onCreateAccountClick = { fullName, _, _ ->
                                // 注册成功后进入 Home 页面。
                                // TODO(Yuxiang, function: uiCreateAccountWithFirebase): Navigate only after Firebase register success.
                                Toast.makeText(
                                    this,
                                    "Create account clicked for $fullName",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                currentScreen = AuthScreen.Home
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
                            smartSuggestion = homeSmartSuggestion,

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
                            onSmartSuggestionClick = { currentScreen = AuthScreen.Pet },

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
                                tripTimelineStops = plan.toTripTimelineStops()
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

                    AuthScreen.Trip -> {
                        TripScreen(
                            modifier = Modifier.fillMaxSize(),
                            destinationTitle = "Today in Melbourne",

                            // TODO(LeYan, function: uiLoadWeatherSummary): Connect real weather data.
                            weatherSummary = TripWeatherSummary(
                                temperature = if (petWeather.isCurrent) "${petWeather.temperatureC.toInt()}°C" else "—",
                                condition = if (petWeather.isCurrent) "${petWeather.label} · ${petWeather.locationLabel}" else "Weather unavailable",
                            ),

                            // TODO(Zewen, function: uiLoadTripSummary): Connect generated multi-day trip summary.
                            // 例如：3 days in Melbourne + Day 1 / Day 2 / Day 3 简介。
                            tripSummary = TripSummary(
                                title = "3 days in Melbourne",
                                description = "Day 1: Melbourne Museum, State Library and ACMI. Day 2: Royal Botanic Gardens and Queen Victoria Market. Day 3: Brighton Beach and Southbank.",
                            ),

                            // TODO(Zewen, function: uiUpdateTripProgress): Replace with real itinerary order and progress status.
                            // input provider: Alex GPS location.
                            stops = tripTimelineStops,

                            // TODO(Leyan, function: uiLoadAttractionDetail): Open detail with real attraction id.
                            // storage provider: Yuxiang database if needed.
                            onStopClick = { stop ->
                                selectedAttractionDetail = sampleAttractionDetail(stop.title)
                                attractionDetailReturnScreen = AuthScreen.Trip
                                currentScreen = AuthScreen.AttractionDetail
                            },

                            // TODO: 之后这里跳转 Edit itinerary 页面，手动增加/删除站点。
                            onEditItineraryClick = {
                                currentScreen = AuthScreen.EditItinerary
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
                        // TODO(Alex, function: uiExploreNearbyPlaces): Temporary Explore nearby mock data.
                        // data provider: Leyan attraction list; GPS provider: Alex sensor.
                        val nearbyExplorePlaces = remember(exploreShakeRefreshCount) {
                            sampleNearbyExplorePlaces().rotateLeft(exploreShakeRefreshCount)
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

                            // Add to trip: 先加入当前行程假数据，并回到 Trip。
                            // TODO(Zewen, function: uiAddAttractionToTrip): Send attraction id to backend for insert/reorder.
                            onAddToTripClick = { attraction ->
                                tripTimelineStops = rebalanceTripStopsAfterManualEdit(
                                    tripTimelineStops.withAddedTemporaryAttraction(attraction),
                                )
                                Toast.makeText(
                                    this,
                                    "${attraction.name} added to itinerary",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                currentScreen = AuthScreen.Trip
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
                                    "Generate itinerary for ${request.destination}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                // TODO(Zewen, function: uiGenerateItinerary): Set false after backend returns itinerary.
                                isGeneratingItinerary = false
                                currentScreen = AuthScreen.Trip
                            },
                        )
                    }

                    AuthScreen.PlanAddStop -> {
                        AddStopScreen(
                            modifier = Modifier.fillMaxSize(),
                            currentCity = "Melbourne",
                            places = remember(addStopShakeRefreshCount) {
                                melbournePopularAddStopPlaces().rotateLeft(addStopShakeRefreshCount)
                            },
                            isSearching = isSearchingPlaces,

                            // 从 Plan My Trip 进来，返回 Plan My Trip。
                            onBackClick = {
                                currentScreen = AuthScreen.PlanMyTrip
                            },

                            // TODO(Alex, function: uiSearchPlacesByName): Send query only after confirm button; name match first.
                            // 输入框中间修改不传；只有按确认搜索按钮后才会走到这里。
                            onSearchConfirmClick = { query ->
                                isSearchingPlaces = true
                                Toast.makeText(
                                    this,
                                    "Search: $query",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                // TODO(Alex, function: uiSearchPlacesByName): Set false after search results return.
                                // data provider: Leyan attraction names.
                                isSearchingPlaces = false
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
                            places = remember(addStopShakeRefreshCount) {
                                melbournePopularAddStopPlaces().rotateLeft(addStopShakeRefreshCount)
                            },
                            isSearching = isSearchingPlaces,

                            // 从 Edit itinerary 进来，返回 Edit itinerary。
                            onBackClick = {
                                currentScreen = AuthScreen.EditItinerary
                            },

                            // TODO(Alex, function: uiSearchPlacesByName): Send query only after confirm button; name match first.
                            // 输入框中间修改不传；只有按确认搜索按钮后才会走到这里。
                            onSearchConfirmClick = { query ->
                                isSearchingPlaces = true
                                Toast.makeText(
                                    this,
                                    "Search: $query",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                // TODO(Alex, function: uiSearchPlacesByName): Set false after search results return.
                                // data provider: Leyan attraction names.
                                isSearchingPlaces = false
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
                                tripInterests = updatedInterests
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

                            // TODO(Yuxiang, function: uiLoadUserProfile): Replace with Firebase display name.
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

                            // TODO(Yuxiang, function: uiLogout): connect Firebase logout.
                            onLogoutClick = {
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

                            // Save button.
                            // TODO(Yuxiang, function: uiSaveUserProfile): Send updatedName to Firebase user profile.
                            onSaveChangesClick = { updatedName ->
                                userName = updatedName
                                Toast.makeText(
                                    this,
                                    "Profile updated",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                currentScreen = AuthScreen.Profile
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

                            // Done 后更新长期默认兴趣；之后接 Yuxiang 的 Firebase 永久保存。
                            onDoneClick = { updatedInterests ->
                                profileDefaultInterests = updatedInterests
                                currentScreen = AuthScreen.Profile
                            },
                        )
                    }

                    AuthScreen.SensorTest -> {
                        SensorTestScreen(
                            sensorRepository = sensorRepository,
                            onBack = {
                                currentScreen = AuthScreen.Login
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
        val shakeStarted = sensorRepository.startShakeDetection()
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
    ): Boolean {
        // function: uiLoginWithFirebase
        // owner: Yuxiang
        // input: email, password
        // output: login success
        // TODO(Yuxiang): Replace with Firebase Auth signIn.
        return email.isNotBlank() && password.isNotBlank()
    }

    private fun uiCreateAccountWithFirebase(
        fullName: String,
        email: String,
        password: String,
    ): Boolean {
        // function: uiCreateAccountWithFirebase
        // owner: Yuxiang
        // input: fullName, email, password
        // output: register success
        // TODO(Yuxiang): Replace with Firebase Auth createUser + user profile save.
        return fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank()
    }

    private fun uiLoadUserProfile(): String {
        // function: uiLoadUserProfile
        // owner: Yuxiang
        // output: displayName
        // TODO(Yuxiang): Load user display name from Firebase.
        return "Yufei"
    }

    private fun uiSaveUserProfile(updatedName: String): String {
        // function: uiSaveUserProfile
        // owner: Yuxiang
        // input: updatedName
        // output: saved displayName
        // TODO(Yuxiang): Save updated name to Firebase user profile.
        return updatedName.trim()
    }

    private fun uiSaveProfileInterests(interests: List<String>): List<String> {
        // function: uiSaveProfileInterests
        // owner: Yuxiang
        // input: long-term interest ids
        // output: saved interest ids
        // TODO(Yuxiang): Save default interests permanently.
        return interests
    }

    private fun uiLoadSavedTrips(): List<SavedTrip> {
        // function: uiLoadSavedTrips
        // owner: Yuxiang
        // contributor: Zewen trip summary shape
        // output: saved trip cards
        // TODO(Yuxiang): Load saved trip summaries from Firebase/backend.
        return sampleSavedTrips()
    }

    // ---- Alex: sensor + fuzzy search trigger ----

    private fun uiGetCurrentLocation(): SensorLocation {
        // function: uiGetCurrentLocation
        // owner: Alex
        // output: current latitude/longitude/area
        // TODO(Alex): Replace sample location with SensorRepository latest GPS value.
        return SensorLocation(
            latitude = -37.8039,
            longitude = 144.9717,
            accuracyMeters = 12f,
            areaName = "Carlton",
        )
    }

    private fun uiObserveShakeEvents(screen: AuthScreen): String? {
        // function: uiObserveShakeEvents
        // owner: Alex
        // UI consumer: Yufei
        // input: current screen
        // output: UI action label
        // TODO(Alex): Connect real ShakeDetector event to this UI action mapping.
        return when (screen) {
            AuthScreen.Explore -> "refresh nearby places"
            AuthScreen.Trip -> "refresh trip progress"
            AuthScreen.PlanMyTrip,
            AuthScreen.PlanAddStop,
            AuthScreen.EditAddStop -> "refresh stop suggestions"
            else -> null
        }
    }

    private fun uiSearchPlacesByName(
        query: String,
        currentCity: String?,
        userLatitude: Double?,
        userLongitude: Double?,
        limit: Int,
    ): List<AddStopPlace> {
        // function: uiSearchPlacesByName
        // owner: Alex
        // data provider: Leyan attraction list
        // input: query, city, GPS, limit
        // output: name-match places
        // TODO(Alex): Replace with fuzzy name search; name relevance first.
        val source = melbournePopularAddStopPlaces()
        val result = if (query.isBlank()) {
            source
        } else {
            source.filter { place ->
                place.name.contains(query, ignoreCase = true)
            }
        }

        return result.take(limit)
    }

    // ---- Leyan: attraction data ----

    private fun uiExploreNearbyPlaces(
        category: ExplorePlaceCategory,
        userLatitude: Double?,
        userLongitude: Double?,
        limit: Int,
    ): List<ExplorePlace> {
        // function: uiExploreNearbyPlaces
        // owner: Alex
        // data provider: Leyan attraction list
        // input: category, GPS, limit
        // output: nearby places
        // TODO(Alex): Rank by location/category; UI displays name + indoor/outdoor only.
        return sampleNearbyExplorePlaces()
            .filter { place -> place.category == category }
            .take(limit)
    }

    private fun uiLoadAttractionDetail(attractionIdOrName: String): AttractionDetail {
        // function: uiLoadAttractionDetail
        // owner: Leyan
        // storage provider: Yuxiang database if needed
        // input: attraction id or name
        // output: detail data
        // TODO(Leyan): Load photo, hours, indoor/outdoor, website from real database.
        return sampleAttractionDetail(attractionIdOrName)
    }

    // ---- Zewen: itinerary planning ----

    private fun uiGenerateItinerary(
        request: PlanMyTripRequest,
    ): List<TripTimelineStop> {
        // function: uiGenerateItinerary
        // owner: Zewen
        // input: destination/date/time/interests/transport/required places
        // output: generated itinerary stops
        // TODO(Zewen): Replace with itinerary algorithm result.
        return listOf(
            TripTimelineStop("09:00", "Federation Square", TripStopStatus.Done),
            TripTimelineStop("10:00", "Melbourne Museum", TripStopStatus.Current),
            TripTimelineStop("12:00", "Lunch nearby", TripStopStatus.Upcoming),
            TripTimelineStop("14:00", "Royal Botanic Gardens", TripStopStatus.Upcoming),
        )
    }

    private fun uiLoadTripSummary(): TripSummary {
        // function: uiLoadTripSummary
        // owner: Zewen
        // output: short trip summary for Trip screen
        // TODO(Zewen): Return real multi-day summary with itinerary.
        return TripSummary(
            title = "3 days in Melbourne",
            description = "Day 1: Melbourne Museum, State Library and ACMI. Day 2: gardens and markets. Day 3: beach and riverside walk.",
        )
    }

    private fun uiUpdateItineraryAfterEdit(
        editedStops: List<TripTimelineStop>,
    ): List<TripTimelineStop> {
        // function: uiUpdateItineraryAfterEdit
        // owner: Zewen
        // storage provider: Yuxiang saves final itinerary
        // input: added/removed/reordered stops
        // output: recalculated itinerary
        // TODO(Zewen): Recalculate time/order after manual edit.
        return rebalanceTripStopsAfterManualEdit(editedStops)
    }

    private fun uiAdjustItineraryForWeather(
        reason: String,
    ): List<AdjustItineraryPlan> {
        // function: uiAdjustItineraryForWeather
        // owner: Zewen
        // input provider: LeYan weather forecast
        // input: weather reason
        // output: alternative itinerary plans
        // TODO(Zewen): Generate alternatives based on weather forecast.
        return sampleAdjustedItineraryPlans()
    }

    // ---- LeYan: weather ----

    private fun uiLoadWeatherSummary(): TripWeatherSummary {
        // function: uiLoadWeatherSummary
        // owner: LeYan
        // output: current weather summary
        // TODO(LeYan): Replace with one-week weather API result.
        return TripWeatherSummary(
            temperature = "18°C",
            condition = "Partly cloudy",
        )
    }

    private fun uiIsWeatherAvailableForDate(dateText: String): Boolean {
        // function: uiIsWeatherAvailableForDate
        // owner: LeYan
        // UI consumer: Yufei
        // input: selected date
        // output: weather available or unavailable
        // TODO(LeYan): Return false when selected date is more than one week away.
        return true
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

    private fun sampleSavedTrips(): List<SavedTrip> {
        // TODO: Replace with Yuxiang Firebase saved trips.
        // Empty list shows the empty state.
        return listOf(
            SavedTrip(
                destination = "Melbourne",
                durationText = "3 days",
                dateText = "May 2025",
            ),
            SavedTrip(
                destination = "Great Ocean Road",
                durationText = "1 day",
                dateText = "Mar 2025",
            ),
            SavedTrip(
                destination = "Sydney",
                durationText = "2 days",
                dateText = "Jan 2025",
            ),
        )
    }

    private fun melbournePopularAddStopPlaces(): List<AddStopPlace> {
        // TODO: 之后替换为 LeYan 的景点搜索/景点类型数据 + Alex 的距离定位数据。
        return listOf(
            AddStopPlace(
                name = "Melbourne Museum",
                distanceText = "1.8 km",
                environmentType = "Indoor",
            ),
            AddStopPlace(
                name = "National Gallery of Victoria",
                distanceText = "1.2 km",
                environmentType = "Indoor",
            ),
            AddStopPlace(
                name = "State Library Victoria",
                distanceText = "1.1 km",
                environmentType = "Indoor",
            ),
            AddStopPlace(
                name = "ACMI",
                distanceText = "1.0 km",
                environmentType = "Indoor",
            ),
            AddStopPlace(
                name = "Royal Botanic Gardens",
                distanceText = "2.4 km",
                environmentType = "Outdoor",
            ),
            AddStopPlace(
                name = "Queen Victoria Market",
                distanceText = "1.6 km",
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
