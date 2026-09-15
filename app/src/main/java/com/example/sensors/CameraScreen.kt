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
import androidx.compose.ui.platform.LocalView
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import android.content.ContentValues
import android.widget.Toast
import android.provider.MediaStore
import androidx.core.content.ContextCompat


@Composable
fun CameraScreen(
    onBack: () -> Unit
) {
    val view = LocalView.current
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
    // Track on by default when pet on
    var trackEnabled by remember {
        mutableStateOf(true)
    }
    // Pet location, (900,450) by default
    var manualPetPosition by remember {
        mutableStateOf(
            IntOffset(900, 450)
        )
    }

    var imageCapture by remember {
        mutableStateOf<ImageCapture?>(null)
    }

    // Take picture and save to system album
    fun takePhoto() {
        // Get ImageCapture and ContentResolver
        val capture =
            imageCapture ?: return


        val resolver =
            view.context.contentResolver


        // Set the name and media type of the image
        val contentValues =
            ContentValues().apply {

                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "pet_photo_${System.currentTimeMillis()}.jpg"
                )

                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg"
                )

                // Save to Pictures/PetCamera
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/PetCamera"
                )
            }


        // Tell CameraX where to save the photo
        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(
                resolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

        // Start taking picture
        capture.takePicture(
            outputOptions,

            ContextCompat.getMainExecutor(
                view.context
            ),

            object : ImageCapture.OnImageSavedCallback {

                // Successfully take photo and notify user
                override fun onImageSaved(
                    outputFileResults:
                    ImageCapture.OutputFileResults
                ) {

                    Toast.makeText(
                        view.context,
                        "Photo saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Fail to take photo
                override fun onError(
                    exception: ImageCaptureException
                ) {

                    Toast.makeText(
                        view.context,
                        "Failed to save photo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }






    // Camera Screen
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
            },
            onImageCaptureReady = { capture ->
                imageCapture = capture}
        )

        // Pet overlay
        // Pet on: track will be on by default
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

        // Take Photo Button
        Button(
            onClick = {
                takePhoto()
            },

            // Cant take photo if camera is not ready
            enabled = imageCapture != null,

            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = 80.dp,
                    start = 20.dp
                )
        ) {
            Text("Take Photo")
        }


        // Pet off: track off, stop ML Kit
        Button(
            onClick = {
                petEnabled = !petEnabled
                // Pet on to off
                if (!petEnabled) {
                    trackEnabled = false

                    personPosition = null

                    calibrationOffset =
                        IntOffset.Zero
                } else{
                    // Pet off to on: track will be on by default
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
                    // Track on to off, pet will be at the manual position, not back to the default position
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