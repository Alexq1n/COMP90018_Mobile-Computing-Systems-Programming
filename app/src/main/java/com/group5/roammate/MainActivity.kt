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
                var weatherUpdatesEnabled by rememberSaveable { mutableStateOf(true) }

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
                                    // 临时跳转：现在 Home 还没做完，所以登录后先进入 Profile 页面预览。
                                    // TODO: HomeScreen 完成后，把这里改成跳转到 Home。
                                    currentScreen = AuthScreen.Profile
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
                                // 临时跳转：注册成功后先进入 Profile 页面预览。
                                // TODO: 接入 Yuxiang 的 Firebase 注册后，再根据真实注册结果跳转。
                                Toast.makeText(
                                    this,
                                    "Create account clicked for $fullName",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                currentScreen = AuthScreen.Profile
                            },

                            // An error prompt on the login page
                            onValidationError = { message ->
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }

                    AuthScreen.Profile -> {
                        ProfileScreen(
                            modifier = Modifier.fillMaxSize(),

                            // TODO: 这里之后换成 Yuxiang 从 Firebase 读到的真实用户姓名。
                            userName = "Yufei",
                            userInitial = "Y",
                            weatherUpdatesEnabled = weatherUpdatesEnabled,

                            // TODO: 这里之后让 Yuxiang 保存通知开关状态到用户资料里。
                            onWeatherUpdatesChanged = { isEnabled ->
                                weatherUpdatesEnabled = isEnabled
                            },

                            // TODO: 这里之后跳转到 Travel Preferences 页面。
                            onTravelPreferencesClick = {
                                Toast.makeText(this, "Travel preferences clicked", Toast.LENGTH_SHORT).show()
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

                            // TODO: Home/Trip/Explore/Pet 页面做好后，这里改成真正底部导航跳转。
                            onTabClick = { tab ->
                                if (tab != RoamMateMainTab.Profile) {
                                    Toast.makeText(
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
    Profile,
}
