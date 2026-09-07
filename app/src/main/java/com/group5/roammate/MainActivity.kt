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
import com.group5.roammate.ui.theme.RoamMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allows the app UI to draw behind the status bar and navigation bar.
        enableEdgeToEdge()

        // setContent starts the Jetpack Compose UI for this app.
        setContent {
            // RoamMateTheme controls the app colors, fonts, and overall visual style.
            RoamMateTheme(dynamicColor = false) {
                // This is a tiny temporary navigation state.
                // Later, we can replace it with a real NavHost when more pages are ready.
                var currentScreen by rememberSaveable { mutableStateOf(AuthScreen.Login) }

                when (currentScreen) {
                    AuthScreen.Login -> {
                        // LoginScreen is currently the first screen users see.
                        LoginScreen(
                            modifier = Modifier.fillMaxSize(),

                            // This callback runs when the user taps the Log in button.
                            // TODO: Connect this to Yuxiang's Firebase login function later.
                            onLoginClick = { email, password ->
                                val message = if (email.isBlank() || password.isBlank()) {
                                    "Please enter email and password"
                                } else {
                                    "Log in clicked"
                                }

                                // Toast is a small temporary message shown at the bottom of the screen.
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            },

                            // This callback opens the Create Account page.
                            onCreateAccountClick = {
                                currentScreen = AuthScreen.CreateAccount
                            },
                        )
                    }

                    AuthScreen.CreateAccount -> {
                        CreateAccountScreen(
                            modifier = Modifier.fillMaxSize(),

                            // This callback goes back to Login when the user taps the arrow.
                            onBackClick = {
                                currentScreen = AuthScreen.Login
                            },

                            // This callback goes back to Login when the user taps the Log in link.
                            onLoginClick = {
                                currentScreen = AuthScreen.Login
                            },

                            // This callback receives validated account input from the UI.
                            // TODO: Send fullName/email/password to Yuxiang's Firebase register function.
                            onCreateAccountClick = { fullName, _, _ ->
                                Toast.makeText(
                                    this,
                                    "Create account clicked for $fullName",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },

                            // The UI sends simple validation messages here.
                            onValidationError = { message ->
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
}
