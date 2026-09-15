package com.example.sensors

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset

@Composable
fun PetOverlay(
    personPosition: IntOffset?,
    calibrationOffset: IntOffset,
    onCalibrationOffsetChanged: (IntOffset) -> Unit
) {

    val currentOffset by rememberUpdatedState(
        calibrationOffset
    )

    val currentCallback by rememberUpdatedState(
        onCalibrationOffsetChanged
    )

    personPosition?.let { position ->

        val petPosition =
            IntOffset(
                position.x + calibrationOffset.x,
                position.y + calibrationOffset.y
            )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            Text(
                text = "🐕",
                modifier = Modifier
                    .offset {
                        petPosition
                    }
                    .background(Color.Transparent)
                    .pointerInput(Unit) {

                        detectDragGestures { change, dragAmount ->

                            change.consume()

                            val newOffset =
                                IntOffset(
                                    currentOffset.x +
                                            dragAmount.x.toInt(),

                                    currentOffset.y +
                                            dragAmount.y.toInt()
                                )

                            currentCallback(
                                newOffset
                            )
                        }
                    }
            )
        }
    }
}