package com.example.sensors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp



@Composable
fun CameraScreen(
    onBack: () -> Unit
) {
    // Set the pet at the top-right corner.
    var personPosition by remember {
        mutableStateOf<IntOffset?>(null)
    }

    var calibrationOffset by remember {
        mutableStateOf(IntOffset.Zero)
    }

    var petEnabled by remember {
        mutableStateOf(true)
    }
    // Track on by default
    var trackEnabled by remember {
        mutableStateOf(true)
    }
    // Track off pet location
    var manualPetPosition by remember {
        mutableStateOf(
            IntOffset(900, 450)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        CameraPreview(
            petEnabled = petEnabled,

            trackEnabled = trackEnabled,

            onPersonPositionChanged = { position ->
                personPosition = position
            },
            onCalibrationOffsetZero = {
                calibrationOffset = IntOffset.Zero
            }

        )
        if (petEnabled) {

            PetOverlay(
                personPosition = personPosition,
                calibrationOffset = calibrationOffset,

                trackEnabled =
                    trackEnabled,
                manualPetPosition =
                    manualPetPosition,

                onCalibrationOffsetChanged = {
                    calibrationOffset = it
                },

                onManualPetPositionChanged = {
                    manualPetPosition = it
                }
            )
        }

        // Pet off: track off, stop ML Kit
        Button(
            onClick = {
                petEnabled = !petEnabled

                if (!petEnabled) {
                    trackEnabled = false

                    personPosition = null

                    calibrationOffset =
                        IntOffset.Zero
                } else{
                    // Pet off to on
                    trackEnabled = true
                    manualPetPosition =
                        IntOffset(900, 450)
                    // Reset person position
                    personPosition = null
                    calibrationOffset =
                        IntOffset.Zero
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
        ) {
            Text(
                if (petEnabled) {
                    "Hide Pet"
                } else {
                    "Show Pet"
                }
            )
        }
        Button(
            onClick = {
                if(trackEnabled){
                    // Track on to off
                    if(personPosition != null){
                        manualPetPosition = IntOffset(
                            personPosition!!.x + calibrationOffset.x,
                            personPosition!!.y + calibrationOffset.y
                        )
                    }
                    trackEnabled = false
                } else{
                    // Track off to on
                    trackEnabled = true
                    calibrationOffset = IntOffset.Zero
                    personPosition = null
                }
            },
            enabled = petEnabled,


            colors = ButtonDefaults.buttonColors(
                // Track on to Green
                containerColor = if (trackEnabled) {
                    Color.Green
                } else {
                    // Track off to Red
                    Color.Red
                }),

            modifier = Modifier.align(Alignment.TopEnd)
                .padding(
                    top=80.dp,
                    end = 20.dp
                )
        ){
            Text(
                if (trackEnabled) {
                    "Track On"
                } else {
                    "Track Off"
                }
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        ) {
            Text("Back")
        }
    }
}