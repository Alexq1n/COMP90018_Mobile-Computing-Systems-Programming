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
        enableEdgeToEdge()
        setContent {
            RoamMateTheme(dynamicColor = false) {
                LoginScreen(
                    modifier = Modifier.fillMaxSize(),
                    onLoginClick = { email, password ->
                        val message = if (email.isBlank() || password.isBlank()) {
                            "Please enter email and password"
                        } else {
                            "Log in clicked"
                        }
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    },
                    onCreateAccountClick = {
                        Toast.makeText(this, "Create account clicked", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}
