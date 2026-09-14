package com.example.sensors


import android.graphics.Rect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context)
    }

    var personPosition by remember {
        mutableStateOf<IntOffset?>(null)
    }

    DisposableEffect(lifecycleOwner) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        val detectorOptions =
            FaceDetectorOptions.Builder()
                .setPerformanceMode(
                    FaceDetectorOptions.PERFORMANCE_MODE_FAST
                )
                .setMinFaceSize(0.1f)
                .build()

        val faceDetector =
            FaceDetection.getClient(detectorOptions)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder().build()

            preview.surfaceProvider =
                previewView.surfaceProvider

            val cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalysis =
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(context)
            ) { imageProxy ->

                val mediaImage =
                    imageProxy.image

                if (mediaImage != null) {

                    val image =
                        InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                    faceDetector
                        .process(image)
                        .addOnSuccessListener { faces ->

                            val face = faces.firstOrNull()

                            if (face != null) {

                                val box =
                                    face.boundingBox

                                val centerX =
                                    box.centerX()

                                val centerY =
                                    box.centerY()

                                personPosition =
                                    IntOffset(
                                        centerX,
                                        centerY
                                    )
                            } else {
                                personPosition = null
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(context))

        onDispose {

            faceDetector.close()

            if (cameraProviderFuture.isDone) {
                cameraProviderFuture
                    .get()
                    .unbindAll()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Camera
        AndroidView(
            factory = {
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Virtual pet
        personPosition?.let { position ->

            Text(
                text = "🐕",
                modifier = Modifier
                    .offset {
                        IntOffset(
                            position.x,
                            position.y
                        )
                    }
                    .background(Color.Transparent)
            )
        }

        // Back button
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