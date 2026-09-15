package com.example.sensors
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
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

@Composable
@OptIn(ExperimentalGetImage::class)
fun CameraPreview(
    petEnabled: Boolean,
    trackEnabled: Boolean,
    onPersonPositionChanged: (IntOffset) -> Unit,
    onCalibrationOffsetZero: () -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context)
    }

    DisposableEffect(lifecycleOwner, petEnabled,trackEnabled) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        // ML Kit
        val detectorOptions =
            FaceDetectorOptions.Builder()
                .setPerformanceMode(
                    FaceDetectorOptions.PERFORMANCE_MODE_FAST
                )
                .setMinFaceSize(0.1f)
                .build()
        val faceDetector =
            FaceDetection.getClient(detectorOptions)

        // Track first person shown in the camera
        var trackedFacePosition: IntOffset? = null
        // Track time that uses to reset the CalibrationOffset
        var lastFaceDetectedTime =
            System.currentTimeMillis()

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder().build()

            // Display camera
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

            val imageCapture =
                ImageCapture.Builder()
                    .setCaptureMode(
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    )
                    .build()

            // Run ImageAnalysis only if Pet on + Track on
            if (petEnabled && trackEnabled) {
                // Apply ML kit on this frame
                // imageProxy: one frame of camera image per time.
                imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(context)
                ) { imageProxy ->

                    val mediaImage =
                        imageProxy.image

                    if (mediaImage != null) {
                        //Convert to ML Kit InputImage
                        val image =
                            InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                        faceDetector
                            .process(image)
                            .addOnSuccessListener { faces ->

                                // Only track first person shown in the camera
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
                                // Pass face location to other file if face found.
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

                                     // Pass the face location to other file
                                    onPersonPositionChanged(
                                        position
                                    )

                                } else {
                                     // Reset the offset only if a new face is detected
                                     // longer than 1.5s of the previous detection.
                                    val currentTime =
                                        System.currentTimeMillis()

                                    if (
                                        currentTime -
                                        lastFaceDetectedTime > 1500
                                    ) {

                                        trackedFacePosition = null
                                        onPersonPositionChanged(
                                            IntOffset(900,350)
                                        )
                                        onCalibrationOffsetZero()
                                        lastFaceDetectedTime = currentTime
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
        } else{
            // Pet or track disabled → no image analysis
            imageAnalysis.clearAnalyzer()
        }


            // Bind camera, preview and image analysis together to the camera screen.
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
                imageCapture
            )
            // Pass image to camera screen
            onImageCaptureReady(imageCapture)

        }, ContextCompat.getMainExecutor(context))

        // If the camera provider is already closed, also close ML Kit
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