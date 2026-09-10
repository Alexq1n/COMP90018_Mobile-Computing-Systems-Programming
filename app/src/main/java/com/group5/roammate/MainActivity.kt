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
import com.group5.roammate.ui.screens.LoginScreen
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

                            // TODO: 之后这里跳转到 Plan My Trip 页面。
                            onPlanMyTripClick = {
                                Toast.makeText(this, "Plan My Trip page is not ready yet", Toast.LENGTH_SHORT).show()
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

                    AuthScreen.Profile -> {
                        ProfileScreen(
                            modifier = Modifier.fillMaxSize(),

                            // TODO: 这里之后换成 Yuxiang 从 Firebase 读到的真实用户姓名。
                            userName = "Yufei",

                            // TODO: 这里之后跳转到共用的 Interests 页面，并保存为用户默认长期偏好。
                            onTravelPreferencesClick = {
                                Toast.makeText(this, "Interests page is not ready yet", Toast.LENGTH_SHORT).show()
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
                }
            }
        }
    }
}

private enum class AuthScreen {
    Login,
    CreateAccount,
    Home,
    Profile,
}
