package com.group5.roammate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.group5.roammate.ui.screens.AddStopPlace
import com.group5.roammate.ui.screens.AddStopScreen
import com.group5.roammate.ui.screens.CreateAccountScreen
import com.group5.roammate.ui.screens.EditItineraryScreen
import com.group5.roammate.ui.screens.HomePetStatus
import com.group5.roammate.ui.screens.HomeScreen
import com.group5.roammate.ui.screens.HomeTripStop
import com.group5.roammate.ui.screens.HomeTripStopStatus
import com.group5.roammate.ui.screens.InterestsSaveTarget
import com.group5.roammate.ui.screens.InterestsScreen
import com.group5.roammate.ui.screens.LoginScreen
import com.group5.roammate.ui.screens.PlanMyTripScreen
import com.group5.roammate.ui.screens.ProfileScreen
import com.group5.roammate.ui.screens.RoamMateMainTab
import com.group5.roammate.ui.screens.TripScreen
import com.group5.roammate.ui.screens.TripStopStatus
import com.group5.roammate.ui.screens.TripTimelineStop
import com.group5.roammate.ui.screens.TripWeatherSummary
import com.group5.roammate.ui.theme.RoamMateTheme

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
                                time = "14:00",
                                title = "Royal Botanic Gardens",
                                status = TripStopStatus.Upcoming,
                            ),
                        ),
                    )
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
                        HomeScreen(
                            modifier = Modifier.fillMaxSize(),

                            // TODO: 这里之后换成 Yuxiang 从 Firebase 读到的真实用户姓名。
                            userName = "Yufei",

                            // Home 的 Today's trip 跟 Trip/Edit itinerary 共用同一份 itinerary。
                            // TODO: 等 Zewen/Yuxiang 提供真实行程数据后，tripTimelineStops 改成后端/Firebase 数据。
                            todayTripStops = tripTimelineStops.toHomeTripStops(),

                            // TODO: 这里现在先用吉祥物图片当 pet 占位。
                            // TODO: 等 Jie/后端做好真正宠物后，把这组 HomePetStatus 换成真实 pet 数据。
                            petStatus = HomePetStatus(
                                name = "Buddy",
                                description = "Rainy day · been walking a while",
                                moodLabel = "Tired",
                                imageRes = R.drawable.roammate_wombat,
                            ),

                            // TODO: 之后这里用真实当前站点地址打开外部地图导航。
                            onNavigateReminderClick = {
                                openExternalMap("Melbourne Museum")
                            },

                            // TODO: 之后这里跳转到 Adjust itinerary 页面。
                            onSmartSuggestionClick = {
                                Toast.makeText(this, "Adjust itinerary page is not ready yet", Toast.LENGTH_SHORT).show()
                            },

                            // TODO: 之后这里跳转到 Companions/Pet 页面。
                            onPetCardClick = {
                                Toast.makeText(this, "Pet page is not ready yet", Toast.LENGTH_SHORT).show()
                            },

                            // 点击 Home 页底部按钮，进入 Plan My Trip 页面。
                            onPlanMyTripClick = {
                                currentScreen = AuthScreen.PlanMyTrip
                            },

                            // TODO: 之后这里跳转到 Explore 页面。
                            onExploreClick = {
                                Toast.makeText(this, "Explore page is not ready yet", Toast.LENGTH_SHORT).show()
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
                                    else -> Toast.makeText(
                                        this,
                                        "${tab.label} page is not ready yet",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
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
                                Toast.makeText(
                                    this,
                                    "Attraction detail for ${stop.title} is not ready yet",
                                    Toast.LENGTH_SHORT,
                                ).show()
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
                                    RoamMateMainTab.Profile -> currentScreen = AuthScreen.Profile
                                    else -> Toast.makeText(
                                        this,
                                        "${tab.label} page is not ready yet",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
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

                    AuthScreen.Profile -> {
                        ProfileScreen(
                            modifier = Modifier.fillMaxSize(),

                            // TODO: 这里之后换成 Yuxiang 从 Firebase 读到的真实用户姓名。
                            userName = "Yufei",

                            // 从 Profile 进入 Interests，保存为用户长期默认偏好。
                            onTravelPreferencesClick = {
                                currentScreen = AuthScreen.ProfileInterests
                            },

                            // TODO: 这里之后跳转到 Saved Trips 页面，数据由 Yuxiang 提供。
                            onSavedTripsClick = {
                                Toast.makeText(this, "Saved trips clicked", Toast.LENGTH_SHORT).show()
                            },

                            // TODO: 这里之后跳转到 Edit Profile 页面。
                            onEditProfileClick = {
                                Toast.makeText(this, "Edit profile clicked", Toast.LENGTH_SHORT).show()
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
                                    else -> Toast.makeText(
                                        this,
                                        "${tab.label} page is not ready yet",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
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
}

private enum class AuthScreen {
    Login,
    CreateAccount,
    Home,
    Trip,
    EditItinerary,
    PlanAddStop,
    EditAddStop,
    PlanMyTrip,
    PlanTripInterests,
    Profile,
    ProfileInterests,
}
