package com.group5.roammate.ui.pet

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.group5.roammate.pet.PetUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

private val CameraTeal = Color(0xFF008B8F)
private val CameraCoral = Color(0xFFFF6F61)

private data class FaceAnchor(
    val xFraction: Float,
    val yFraction: Float,
)

/**
 * Camera experience for a RoamMate travel photo. The visible preview, pet, weather and place stamp
 * are captured together, so the gallery image is the same composition the user sees on screen.
 */
@Composable
fun PetCameraScreen(
    state: PetUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var lensFacing by remember { mutableIntStateOf(CameraCharacteristics.LENS_FACING_FRONT) }
    var petVisible by remember { mutableStateOf(true) }
    var trackFace by remember { mutableStateOf(true) }
    var faceAnchor by remember { mutableStateOf<FaceAnchor?>(null) }
    var manualX by remember { mutableFloatStateOf(0.70f) }
    var manualY by remember { mutableFloatStateOf(0.43f) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraBounds by remember { mutableStateOf<Rect?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var lastSavedUri by remember { mutableStateOf<Uri?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                cameraBounds = Rect(
                    bounds.left.roundToInt(),
                    bounds.top.roundToInt(),
                    bounds.right.roundToInt(),
                    bounds.bottom.roundToInt(),
                )
            },
    ) {
        val density = LocalDensity.current
        val stageWidthPx = with(density) { maxWidth.toPx() }
        val stageHeightPx = with(density) { maxHeight.toPx() }
        val petSize = 174.dp
        val petSizePx = with(density) { petSize.toPx() }

        if (hasCameraPermission) {
            CameraFeed(
                lensFacing = lensFacing,
                trackingEnabled = petVisible && trackFace,
                onFaceAnchorChanged = { faceAnchor = it },
                onCameraError = { cameraError = it },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CameraPermissionPanel(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onBack = onBack,
            )
        }

        if (hasCameraPermission && petVisible) {
            val detected = faceAnchor
            val trackedX = detected?.xFraction?.let { raw ->
                if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) 1f - raw else raw
            }
            val centerX = if (trackFace && trackedX != null) {
                (trackedX + 0.22f).coerceIn(0.24f, 0.80f)
            } else {
                manualX
            }
            val centerY = if (trackFace && detected != null) {
                (detected.yFraction - 0.10f).coerceIn(0.24f, 0.70f)
            } else {
                manualY
            }
            val topLeftX = centerX * stageWidthPx - petSizePx / 2f
            val topLeftY = centerY * stageHeightPx - petSizePx / 2f

            PetAvatar(
                state = state,
                animateIdle = !isSaving,
                modifier = Modifier
                    .offset { IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt()) }
                    .size(petSize)
                    .pointerInput(stageWidthPx, stageHeightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            trackFace = false
                            manualX = (manualX + dragAmount.x / stageWidthPx).coerceIn(0.18f, 0.82f)
                            manualY = (manualY + dragAmount.y / stageHeightPx).coerceIn(0.20f, 0.78f)
                        }
                    },
            )
        }

        if (hasCameraPermission) {
            TravelPhotoStamp(
                condition = "${state.environment.conditionLabel} · ${state.environment.temperatureC.toInt()}°C",
                location = state.environment.locationLabel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 18.dp, top = 70.dp),
            )
        }

        if (controlsVisible && hasCameraPermission) {
            CameraControls(
                petVisible = petVisible,
                trackFace = trackFace,
                isSaving = isSaving,
                hasSavedPhoto = lastSavedUri != null,
                onBack = onBack,
                onFlipCamera = {
                    lensFacing = if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                        CameraCharacteristics.LENS_FACING_BACK
                    } else {
                        CameraCharacteristics.LENS_FACING_FRONT
                    }
                    faceAnchor = null
                },
                onTogglePet = {
                    petVisible = !petVisible
                    if (petVisible) trackFace = true
                },
                onToggleTracking = {
                    trackFace = !trackFace
                    faceAnchor = null
                },
                onCapture = {
                    val bounds = cameraBounds
                    if (activity == null || bounds == null || isSaving) return@CameraControls
                    scope.launch {
                        isSaving = true
                        controlsVisible = false
                        delay(120)
                        val captured = captureWindowRegion(activity, rootView, bounds)
                            .fold(
                                onSuccess = { bitmap -> savePetPhoto(context, bitmap) },
                                onFailure = { Result.failure(it) },
                            )
                        controlsVisible = true
                        isSaving = false
                        captured.onSuccess { uri ->
                            lastSavedUri = uri
                            Toast.makeText(
                                context,
                                "Travel photo saved to Pictures/RoamMate",
                                Toast.LENGTH_LONG,
                            ).show()
                        }.onFailure { error ->
                            Toast.makeText(
                                context,
                                "Could not save photo: ${error.message ?: "unknown error"}",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                },
                onOpenPhoto = {
                    openSavedPhoto(context, lastSavedUri)
                },
            )
        }

        cameraError?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xE6212121),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(18.dp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TravelPhotoStamp(
    condition: String,
    location: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
            Text(
                text = condition,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = location,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CameraControls(
    petVisible: Boolean,
    trackFace: Boolean,
    isSaving: Boolean,
    hasSavedPhoto: Boolean,
    onBack: () -> Unit,
    onFlipCamera: () -> Unit,
    onTogglePet: () -> Unit,
    onToggleTracking: () -> Unit,
    onCapture: () -> Unit,
    onOpenPhoto: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GlassButton(label = "‹ Back", onClick = onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(label = if (petVisible) "Pet on" else "Pet off", onClick = onTogglePet)
                GlassButton(label = "Flip", onClick = onFlipCamera)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.56f))
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GlassButton(
                    label = if (trackFace) "Track on" else "Free move",
                    enabled = petVisible,
                    onClick = onToggleTracking,
                )
                Text(
                    text = "Drag Buddy anytime",
                    color = Color.White.copy(alpha = 0.74f),
                    fontSize = 10.sp,
                )
            }

            Button(
                onClick = onCapture,
                enabled = !isSaving,
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.55f),
                ),
                border = BorderStroke(5.dp, CameraCoral),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text(
                    text = if (isSaving) "…" else "●",
                    color = CameraCoral,
                    fontSize = 28.sp,
                )
            }

            GlassButton(
                label = if (hasSavedPhoto) "View photo" else "Photos",
                onClick = onOpenPhoto,
            )
        }
    }
}

@Composable
private fun GlassButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black.copy(alpha = 0.48f),
            contentColor = Color.White,
            disabledContainerColor = Color.Black.copy(alpha = 0.25f),
            disabledContentColor = Color.White.copy(alpha = 0.45f),
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 13.dp,
            vertical = 9.dp,
        ),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CameraPermissionPanel(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Camera permission needed",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "RoamMate uses the camera only while you compose a travel photo with Buddy.",
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = CameraTeal),
        ) {
            Text("Allow camera")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
        ) {
            Text("Back")
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraFeed(
    lensFacing: Int,
    trackingEnabled: Boolean,
    onFaceAnchorChanged: (FaceAnchor?) -> Unit,
    onCameraError: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentFaceCallback by rememberUpdatedState(onFaceAnchorChanged)
    val currentErrorCallback by rememberUpdatedState(onCameraError)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, lensFacing, trackingEnabled) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .setMinFaceSize(0.12f)
                .build(),
        )

        cameraProviderFuture.addListener(
            {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    if (trackingEnabled) {
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage == null) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val image = InputImage.fromMediaImage(mediaImage, rotation)
                            val rotatedWidth = if (rotation == 90 || rotation == 270) {
                                imageProxy.height.toFloat()
                            } else {
                                imageProxy.width.toFloat()
                            }
                            val rotatedHeight = if (rotation == 90 || rotation == 270) {
                                imageProxy.width.toFloat()
                            } else {
                                imageProxy.height.toFloat()
                            }
                            detector.process(image)
                                .addOnSuccessListener { faces ->
                                    val face = faces.maxByOrNull {
                                        it.boundingBox.width() * it.boundingBox.height()
                                    }
                                    currentFaceCallback(
                                        face?.let {
                                            FaceAnchor(
                                                xFraction = (it.boundingBox.exactCenterX() / rotatedWidth)
                                                    .coerceIn(0f, 1f),
                                                yFraction = (it.boundingBox.exactCenterY() / rotatedHeight)
                                                    .coerceIn(0f, 1f),
                                            )
                                        },
                                    )
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        }
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                    currentErrorCallback(null)
                }.onFailure {
                    currentErrorCallback("Camera could not start. Try flipping camera or use a real phone.")
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            detector.close()
            analysisExecutor.shutdown()
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

private suspend fun captureWindowRegion(
    activity: Activity,
    rootView: View,
    bounds: Rect,
): Result<Bitmap> = try {
    require(bounds.width() > 0 && bounds.height() > 0) { "Camera preview is not ready" }
    val bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        suspendCancellableCoroutine<Bitmap> { continuation ->
            PixelCopy.request(
                activity.window,
                bounds,
                bitmap,
                { result ->
                    if (continuation.isActive) {
                        if (result == PixelCopy.SUCCESS) {
                            continuation.resume(bitmap)
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException("Screen capture error $result"),
                            )
                        }
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }
    } else {
        val fullBitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        withContext(Dispatchers.Main.immediate) { rootView.draw(Canvas(fullBitmap)) }
        val cropLeft = bounds.left.coerceIn(0, fullBitmap.width - 1)
        val cropTop = bounds.top.coerceIn(0, fullBitmap.height - 1)
        Bitmap.createBitmap(
            fullBitmap,
            cropLeft,
            cropTop,
            bounds.width().coerceAtMost(fullBitmap.width - cropLeft),
            bounds.height().coerceAtMost(fullBitmap.height - cropTop),
        )
    }.let { Result.success(it) }
} catch (error: Throwable) {
    Result.failure(error)
}

private suspend fun savePetPhoto(context: Context, bitmap: Bitmap): Result<Uri> =
    withContext(Dispatchers.IO) {
        runCatching {
            val displayName = "RoamMate_${System.currentTimeMillis()}.jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RoamMate")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = checkNotNull(
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
                ) { "Could not create gallery item" }
                try {
                    resolver.openOutputStream(uri)?.use { stream ->
                        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, stream)) {
                            "Could not encode photo"
                        }
                    } ?: error("Could not open gallery item")
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri
                } catch (error: Throwable) {
                    resolver.delete(uri, null, null)
                    throw error
                }
            } else {
                val directory = File(
                    checkNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)),
                    "RoamMate",
                ).apply { mkdirs() }
                val file = File(directory, displayName)
                FileOutputStream(file).use { stream ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, stream)) {
                        "Could not encode photo"
                    }
                }
                suspendCancellableCoroutine { continuation ->
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file.absolutePath),
                        arrayOf("image/jpeg"),
                    ) { _, uri ->
                        if (continuation.isActive) continuation.resume(uri ?: Uri.fromFile(file))
                    }
                }
            }
        }
    }

private fun openSavedPhoto(context: Context, uri: Uri?) {
    val intent = if (uri != null) {
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "No gallery app is available", Toast.LENGTH_SHORT).show()
        }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
