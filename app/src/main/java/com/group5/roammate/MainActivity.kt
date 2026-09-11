package com.group5.roammate

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.group5.roammate.ui.screens.CreateAccountScreen
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

                            // TODO: 这组 Today's trip 是临时演示假数据。
                            // TODO: 等 Zewen/Yuxiang 提供真实行程数据后，删除这段 listOf(...)，改成传真实行程列表。
                            todayTripStops = listOf(
                                HomeTripStop(
                                    time = "09:00",
                                    title = "Federation Square",
                                    subtitle = "",
                                    icon = "A",
                                    status = HomeTripStopStatus.Done,
                                ),
                                HomeTripStop(
                                    time = "10:00",
                                    title = "Melbourne Museum",
                                    subtitle = "Now",
                                    icon = "M",
                                    status = HomeTripStopStatus.Current,
                                ),
                                HomeTripStop(
                                    time = "12:00",
                                    title = "Lunch nearby",
                                    subtitle = "Next",
                                    icon = "L",
                                    status = HomeTripStopStatus.Next,
                                ),
                            ),

                            // TODO: 这里现在先用吉祥物图片当 pet 占位。
                            // TODO: 等 Jie/后端做好真正宠物后，把这组 HomePetStatus 换成真实 pet 数据。
                            petStatus = HomePetStatus(
                                name = "Buddy",
                                description = "Rainy day · been walking a while",
                                moodLabel = "Tired",
                                imageRes = R.drawable.roammate_wombat,
                            ),

                            // TODO: 之后这里打开外部地图导航。
                            onNavigateReminderClick = {
                                Toast.makeText(this, "External map navigation is not ready yet", Toast.LENGTH_SHORT).show()
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

                            // TODO: 之后这里跳转到 Trip 页面。
                            onTripClick = {
                                Toast.makeText(this, "Trip page is not ready yet", Toast.LENGTH_SHORT).show()
                            },

                            // 底部导航先接好 Home/Profile，其余页面等对应 UI 做好后再跳转。
                            onTabClick = { tab ->
                                when (tab) {
                                    RoamMateMainTab.Home -> Unit
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

                    AuthScreen.PlanMyTrip -> {
                        PlanMyTripScreen(
                            modifier = Modifier.fillMaxSize(),

                            // 这里是本次行程兴趣，不是 Profile 的长期默认兴趣。
                            selectedInterests = tripInterests,
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

                            // TODO: 之后这里跳转到 Add a stop 搜索页面，复用搜索景点的界面。
                            onSearchPlacesClick = {
                                Toast.makeText(this, "Add a stop search is not ready yet", Toast.LENGTH_SHORT).show()
                            },

                            // TODO: 之后这里把 request 交给 Zewen 的规划算法，再跳转到 Trip 页面。
                            onGenerateItineraryClick = { request ->
                                Toast.makeText(
                                    this,
                                    "Generate itinerary for ${request.destination}",
                                    Toast.LENGTH_SHORT,
                                ).show()
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

                            // 底部导航先接好 Home/Profile，其余页面等对应 UI 做好后再跳转。
                            onTabClick = { tab ->
                                when (tab) {
                                    RoamMateMainTab.Profile -> Unit
                                    RoamMateMainTab.Home -> currentScreen = AuthScreen.Home
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
}

private enum class AuthScreen {
    Login,
    CreateAccount,
    Home,
    PlanMyTrip,
    PlanTripInterests,
    Profile,
    ProfileInterests,
}
