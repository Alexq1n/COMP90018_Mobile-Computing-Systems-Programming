package com.group5.roammate

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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
                // LoginScreen is currently the first screen users see.
                // Later, this area can be replaced by a navigation host.
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

                    // This callback runs when the user taps Create account.
                    // TODO: Navigate to the Create Account screen later.
                    onCreateAccountClick = {
                        Toast.makeText(this, "Create account clicked", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}