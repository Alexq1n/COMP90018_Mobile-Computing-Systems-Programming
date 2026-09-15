package com.example.sensors

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    onPersonPositionChanged: (IntOffset) -> Unit,
    onFaceLost: () -> Unit,
    onCalibrationOffsetZero: () -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context)
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

        var trackedFacePosition: IntOffset? = null
        var lastFaceDetectedTime =
            System.currentTimeMillis()

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

                            val face =
                                if (trackedFacePosition == null) {

                                    faces.firstOrNull()

                                } else {

                                    faces.minByOrNull { face ->

                                        val box =
                                            face.boundingBox

                                        val centerX =
                                            box.centerX()

                                        val centerY =
                                            box.centerY()

                                        val dx =
                                            centerX -
                                                    trackedFacePosition!!.x

                                        val dy =
                                            centerY -
                                                    trackedFacePosition!!.y

                                        dx * dx + dy * dy
                                    }
                                }

                            if (face != null) {

                                lastFaceDetectedTime =
                                    System.currentTimeMillis()

                                val box =
                                    face.boundingBox

                                val centerX =
                                    box.centerX()

                                val centerY =
                                    box.centerY()

                                val position =
                                    IntOffset(
                                        centerX,
                                        centerY
                                    )

                                trackedFacePosition =
                                    position

                                onPersonPositionChanged(
                                    position
                                )

                            } else {
                                onFaceLost()
                                val currentTime =
                                    System.currentTimeMillis()

                                if (
                                    currentTime -
                                    lastFaceDetectedTime > 1500
                                ) {

                                    trackedFacePosition = null
                                    onCalibrationOffsetZero()

                                }
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

    AndroidView(
        factory = {
            previewView
        },
        modifier = Modifier
    )
}