package com.example.sensors

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner


@Composable
fun CameraScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context)
    }


    DisposableEffect(lifecycleOwner) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)


        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()


            val preview =
                Preview.Builder().build()


            preview.surfaceProvider =
                previewView.surfaceProvider


            val cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA


            cameraProvider.unbindAll()


            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )

        }, ContextCompat.getMainExecutor(context))


        onDispose {

            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Camera Preview
        AndroidView(
            factory = {
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )


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