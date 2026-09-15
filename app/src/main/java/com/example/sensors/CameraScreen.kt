package com.example.sensors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun CameraScreen(
    onBack: () -> Unit
) {

    var personPosition by remember {
        mutableStateOf<IntOffset?>(null)
    }

    var calibrationOffset by remember {
        mutableStateOf(IntOffset.Zero)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        CameraPreview(
            onPersonPositionChanged = { position ->
                personPosition = position
            },
            onFaceLost = {
                personPosition = null
            },
            onCalibrationOffsetZero = {
                calibrationOffset = IntOffset.Zero
            }

        )

        PetOverlay(
            personPosition = personPosition,
            calibrationOffset = calibrationOffset,
            onCalibrationOffsetChanged = {
                calibrationOffset = it
            }
        )

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