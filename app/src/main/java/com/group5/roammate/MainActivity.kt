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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.group5.roammate.pet.OpenMeteoPetWeatherRepository
import com.group5.roammate.pet.PetEnvironmentSnapshot
import com.group5.roammate.pet.PetOutfitMode
import com.group5.roammate.pet.PetPreferences
import com.group5.roammate.pet.PetStateEngine
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
import com.group5.roammate.ui.screens.ProfileScreen
import com.group5.roammate.ui.screens.RoamMateMainTab
import com.group5.roammate.ui.screens.SavedTrip
import com.group5.roammate.ui.screens.SavedTripsScreen
import com.group5.roammate.ui.screens.TripScreen
import com.group5.roammate.ui.screens.TripStopStatus
import com.group5.roammate.ui.screens.TripTimelineStop
import com.group5.roammate.ui.screens.TripWeatherSummary
import com.group5.roammate.ui.pet.CompanionsScreen
import com.group5.roammate.ui.pet.PetCameraScreen
import com.group5.roammate.ui.theme.RoamMateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable the page to extend to the status bar and the bottom navigation bar area.
        enableEdgeToEdge()

        // setContent is the entry point of Jetpack Compose
        // and all pages will start to display from here.
        setContent {
            // RoamMateTheme responsible for unifying colors, fonts and the overall visual style.
            RoamMateTheme(dynamicColor = false) {
                // This is a temporary page status.
                var currentScreen by rememberSaveable { mutableStateOf(AuthScreen.Login) }

                // Jie pet feature state. The environment can later be replaced directly with
                // Yan/Alex/Sitao's weather, activity and location payloads.
                val petPreferences = remember { PetPreferences(applicationContext) }
                val petWeatherRepository = remember { OpenMeteoPetWeatherRepository() }
                val petScope = rememberCoroutineScope()
                var petProfile by remember { mutableStateOf(petPreferences.loadProfile()) }
                var petEnvironment by remember { mutableStateOf(PetEnvironmentSnapshot.demo()) }
                var isRefreshingPetWeather by remember { mutableStateOf(false) }
                var petWeatherError by remember { mutableStateOf<String?>(null) }
                val petState = PetStateEngine.buildUiState(petEnvironment, petProfile)

                suspend fun refreshPetWeather() {
                    isRefreshingPetWeather = true
                    petWeatherRepository.fetchCurrent(
                        latitude = -37.8136,
                        longitude = 144.9631,
                    ).onSuccess { weather ->
                        petEnvironment = petEnvironment.copy(
                            condition = weather.condition,
                            conditionLabel = weather.conditionLabel,
                            temperatureC = weather.temperatureC,
                            windSpeedKmh = weather.windSpeedKmh,
                            source = weather.source,
                        )
                        petWeatherError = null
                    }.onFailure { error ->
                        petWeatherError = error.message ?: "Weather request failed"
                    }
                    isRefreshingPetWeather = false
                }

                LaunchedEffect(Unit) {
                    refreshPetWeather()
                }

                // User name
                // TODO: Replace with Yuxiang Firebase user profile.
                var userName by rememberSaveable { mutableStateOf("Yufei") }

                // Attraction detail 当前展示的景点。之后由 Explore/Trip 点击的真实景点数据替换。
                var selectedAttractionDetail by remember {
                    mutableStateOf(sampleAttractionDetail("Melbourne Museum"))
                }

                // 记录详情页从哪里进来，返回按钮才能回到正确页面。
                var attractionDetailReturnScreen by rememberSaveable {
                    mutableStateOf(AuthScreen.Explore)
                }

                // Profile 入口使用：长期默认兴趣偏好。
                // TODO: 之后这里接 Yuxiang 的 Firebase 用户偏好数据库，永久保存。
                var profileDefaultInterests by rememberSaveable {
                    mutableStateOf(listOf("Museums", "Parks", "Food"))
                }

                // Plan My Trip 入口使用：只属于当前这次行程的兴趣。
                // TODO: 之后这里交给 Zewen 的本次行程规划 request，不写入长期默认偏好。
                var tripInterests by rememberSaveable {
                    mutableStateOf(listOf("Museums", "Parks", "Food"))
                }

                // Plan My Trip 入口添加的必去地点。
                // TODO: 之后这里要作为 Zewen itinerary request 的 must-visit places。
                var planRequiredPlaces by remember {
                    mutableStateOf(emptyList<AddStopPlace>())
                }

                // Saved trips
                // TODO: Replace with Yuxiang Firebase saved trip history.
                val savedTrips = remember {
                    sampleSavedTrips()
                }

                // TODO: 这组 Trip timeline 是临时演示假数据。
                // TODO: 等 Zewen 的后端行程生成完成后，改成读取后端返回的真实 itinerary。
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

                // Adjust itinerary 弹窗当前显示第几个候选方案。
                // TODO: Zewen 现在只提供两个方案，所以这里最多切换 0 和 1。
                var adjustPlanIndex by rememberSaveable { mutableStateOf(0) }

                // TODO: 这是临时假数据；之后替换成 Zewen 根据天气重新生成的两个候选行程。
                val adjustedItineraryPlans = remember {
                    sampleAdjustedItineraryPlans()
                }

                when (currentScreen) {
                    AuthScreen.Login -> {
                        // After the user opens the app, they first see the Login page.
                        LoginScreen(
                            modifier = Modifier.fillMaxSize(),

                            // This will run when the user clicks "Log in".
                            // TODO: 之后这里接 Yuxiang 的 Firebase 登录函数。
                            onLoginClick = { email, password ->
                                if (email.isBlank() || password.isBlank()) {
                                    Toast.makeText(
                                        this,
                                        "Please enter email and password",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    // 登录成功后进入 Home 页面。
                                    // TODO: 之后这里要根据 Yuxiang 的 Firebase 登录结果决定是否跳转。
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
                            // TODO: 之后这里接 Yuxiang 的 Firebase 注册函数。
                            onCreateAccountClick = { fullName, _, _ ->
                                // 注册成功后进入 Home 页面。
                                // TODO: 接入 Yuxiang 的 Firebase 注册后，再根据真实注册结果跳转。
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
                        // TODO: 这是 Home 顶部 Leave now 提醒的临时假数据。
                        // TODO: 之后由 Zewen 提供当前/下一站行程，Sitao 提供当前位置、路线时间和是否迟到。
                        val homeLeaveNowReminder = HomeLeaveNowReminder(
                            placeName = "Melbourne Museum",
                            scheduledTime = "10:00",
                            delayMinutes = 3,
                            navigationQuery = "Melbourne Museum",
                        )

                        // TODO: 这是 Home 顶部 Smart suggestion 的临时假数据。
                        // TODO: 之后由 Yan 提供天气变化，Zewen 根据建议触发行程调整。
                        val homeSmartSuggestion = HomeSmartSuggestion(
                            label = "Rain",
                            message = "Smart suggestion · Rain 2-4 PM, tap to adjust your plan",
                        )

                        HomeScreen(
                            modifier = Modifier.fillMaxSize(),

                            // TODO: 这里之后换成 Yuxiang 从 Firebase 读到的真实用户姓名。
                            userName = "Yufei",

                            // Home 的 Today's trip 跟 Trip/Edit itinerary 共用同一份 itinerary。
                            // TODO: 等 Zewen/Yuxiang 提供真实行程数据后，tripTimelineStops 改成后端/Firebase 数据。
                            todayTripStops = tripTimelineStops.toHomeTripStops(),

                            // Home 顶部提醒现在从数据对象读取，不再在 HomeScreen 里写死文字。
                            // 如果没有提醒，之后把这里传 null 即可隐藏卡片。
                            leaveNowReminder = homeLeaveNowReminder,

                            // Home 顶部天气建议现在从数据对象读取。
                            // 如果没有建议，之后把这里传 null 即可隐藏卡片。
                            smartSuggestion = homeSmartSuggestion,

                            // Jie pet module: this card shares live weather, outfit and mood state
                            // with the full Companions and camera experiences.
                            petStatus = HomePetStatus(
                                name = "Buddy",
                                description = petState.statusLine,
                                moodLabel = petState.mood.label,
                                petState = petState,
                            ),

                            // TODO: 之后这里用真实当前站点地址打开外部地图导航。
                            onNavigateReminderClick = {
                                openExternalMap(homeLeaveNowReminder.navigationQuery)
                            },

                            // TODO: 之后这里跳转到 Adjust itinerary 页面。
                            onSmartSuggestionClick = {
                                adjustPlanIndex = 0
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

                            // TODO: 这里之后接 Yan 的实时天气数据。
                            weatherSummary = TripWeatherSummary(
                                temperature = "18°C",
                                condition = "Partly cloudy",
                            ),

                            // TODO: 等 Zewen 的行程列表和 Sitao 的进度状态完成后，替换成真实顺序/状态。
                            stops = tripTimelineStops,

                            // TODO: 之后这里跳转 Attraction detail 页面。
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
                        // TODO: 这是 Explore 页的临时假数据。
                        // TODO: 之后 currentArea/places 由 Alex/Sitao 的 GPS + Yan/Leyan 的景点数据共同生成。
                        val nearbyExplorePlaces = remember {
                            sampleNearbyExplorePlaces()
                        }

                        ExploreScreen(
                            modifier = Modifier.fillMaxSize(),
                            currentArea = "Carlton",
                            places = nearbyExplorePlaces,

                            // TODO: 之后这里跳转 Attraction detail 页面。
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
                            // TODO: 之后这里应把景点交给 Zewen，让后端决定插入位置和重新排序。
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

                            // TODO: 之后这里把删除后的 stop list 发给 Zewen，重新生成/排序行程。
                            onRemoveStopClick = { removedStop ->
                                tripTimelineStops = rebalanceTripStopsAfterManualEdit(
                                    tripTimelineStops.filterNot { stop ->
                                        stop.time == removedStop.time && stop.title == removedStop.title
                                    },
                                )
                            },

                            // TODO: 之后这里跳转 Add a stop 搜索页面，并把新站点交给 Zewen 重新规划。
                            onAddStopClick = {
                                currentScreen = AuthScreen.EditAddStop
                            },

                            // TODO: 之后这里接 Yuxiang 保存行程；Zewen 返回新行程后再保存。
                            onSaveClick = {
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

                            // TODO: 之后这里把 request 交给 Zewen 的规划算法，再跳转到 Trip 页面。
                            onGenerateItineraryClick = { request ->
                                Toast.makeText(
                                    this,
                                    "Generate itinerary for ${request.destination}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                currentScreen = AuthScreen.Trip
                            },
                        )
                    }

                    AuthScreen.PlanAddStop -> {
                        AddStopScreen(
                            modifier = Modifier.fillMaxSize(),
                            currentCity = "Melbourne",
                            places = melbournePopularAddStopPlaces(),

                            // 从 Plan My Trip 进来，返回 Plan My Trip。
                            onBackClick = {
                                currentScreen = AuthScreen.PlanMyTrip
                            },

                            // TODO: 之后这里把 query 传给 Alex 的景点搜索接口。
                            // 输入框中间修改不传；只有按确认搜索按钮后才会走到这里。
                            onSearchConfirmClick = { query ->
                                Toast.makeText(
                                    this,
                                    "Search: $query",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },

                            // TODO: 之后这里把 added place 放进 Zewen 的 itinerary request。
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
                            places = melbournePopularAddStopPlaces(),

                            // 从 Edit itinerary 进来，返回 Edit itinerary。
                            onBackClick = {
                                currentScreen = AuthScreen.EditItinerary
                            },

                            // TODO: 之后这里把 query 传给 Alex 的景点搜索接口。
                            // 输入框中间修改不传；只有按确认搜索按钮后才会走到这里。
                            onSearchConfirmClick = { query ->
                                Toast.makeText(
                                    this,
                                    "Search: $query",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },

                            // TODO: 之后这里把新增地点交给 Zewen，让后端插入合适位置并重新生成行程。
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
                        CompanionsScreen(
                            state = petState,
                            profile = petProfile,
                            isRefreshingWeather = isRefreshingPetWeather,
                            weatherError = petWeatherError,
                            onStyleSelected = { style ->
                                val updatedProfile = petProfile.copy(selectedStyle = style)
                                petProfile = updatedProfile
                                petPreferences.saveProfile(updatedProfile)
                            },
                            onOutfitModeSelected = { outfitMode ->
                                val updatedProfile = petProfile.copy(outfitMode = outfitMode)
                                petProfile = updatedProfile
                                petPreferences.saveProfile(updatedProfile)
                            },
                            onRefreshWeather = {
                                petScope.launch { refreshPetWeather() }
                            },
                            onCycleGear = {
                                val modes = PetOutfitMode.entries
                                val nextIndex = (modes.indexOf(petProfile.outfitMode) + 1) % modes.size
                                val updatedProfile = petProfile.copy(outfitMode = modes[nextIndex])
                                petProfile = updatedProfile
                                petPreferences.saveProfile(updatedProfile)
                            },
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

                            // TODO: 这里之后换成 Yuxiang 从 Firebase 读到的真实用户姓名。
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

                            // TODO: 这里之后接 Yuxiang 的 Firebase logout。
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
                            // TODO: Send updatedName to Yuxiang Firebase user profile.
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
                }
            }
        }
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
        // TODO: 之后替换为 Yan 的景点搜索/景点类型数据 + Sitao 的距离定位数据。
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
        // Alex/Sitao 提供当前 GPS，Yan/Leyan 提供景点坐标/类型，再由前端或后端计算距离后显示。
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
        // Leyan/Yan 提供名称、图片、室内外、天气适配、营业时间、官网；
        // Alex/Sitao sensor/GPS 提供用户当前位置，再计算 distanceText。
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
}
