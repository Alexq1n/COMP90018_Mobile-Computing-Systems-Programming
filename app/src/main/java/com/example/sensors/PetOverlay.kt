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
    trackEnabled: Boolean,
    manualPetPosition: IntOffset,
    onCalibrationOffsetChanged: (IntOffset) -> Unit,
    onManualPetPositionChanged: (IntOffset) -> Unit
) {

    val currentOffset by rememberUpdatedState(
        calibrationOffset
    )

    val currentCallback by rememberUpdatedState(
        onCalibrationOffsetChanged
    )
    val currentManualPosition by rememberUpdatedState(
        manualPetPosition
    )
    val currentManualCallback by rememberUpdatedState(
        onManualPetPositionChanged
    )

    val currentTrackEnabled by rememberUpdatedState(
        trackEnabled
    )

    //For Both track on / track off, user can drag pet to the location they want.
    val petPosition =
        if (trackEnabled) {
            personPosition?.let { position ->


                IntOffset(
                    position.x + calibrationOffset.x,
                    position.y + calibrationOffset.y
                )
            } ?: manualPetPosition
        } else {
            manualPetPosition
        }

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
                            // Track on
                            if (currentTrackEnabled) {

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
                        } else{
                        // Track off
                        val newPosition =
                            IntOffset(
                                currentManualPosition.x +
                                        dragAmount.x.toInt(),
                                currentManualPosition.y +
                                        dragAmount.y.toInt()
                            )
                            currentManualCallback(newPosition)
                            }
                        }
                    }
            )
        }
    }