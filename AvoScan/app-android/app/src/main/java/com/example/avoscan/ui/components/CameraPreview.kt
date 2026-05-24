package com.example.avoscan.ui.components

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    triggerCapture: Boolean,
    onCaptureDone: () -> Unit,
    onImageCaptured: (File) -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember {
        mutableStateOf(null)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),

        factory = { ctx ->

            val previewView = PreviewView(ctx)

            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({

                val cameraProvider =
                    cameraProviderFuture.get()

                val preview =
                    Preview.Builder().build()

                preview.setSurfaceProvider(
                    previewView.surfaceProvider
                )

                imageCapture =
                    ImageCapture.Builder().build()

                val cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA

                try {

                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )

    LaunchedEffect(triggerCapture) {

        if (triggerCapture) {

            val photoFile = File(
                context.cacheDir,
                "captured_image.jpg"
            )

            val outputOptions =
                ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture?.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),

                object : ImageCapture.OnImageSavedCallback {

                    override fun onImageSaved(
                        outputFileResults: ImageCapture.OutputFileResults
                    ) {

                        Toast.makeText(
                            context,
                            "Imagen capturada",
                            Toast.LENGTH_SHORT
                        ).show()

                        onImageCaptured(photoFile)
                        onCaptureDone()
                    }

                    override fun onError(
                        exception: ImageCaptureException
                    ) {

                        exception.printStackTrace()

                        onCaptureDone()
                    }
                }
            )
        }
    }
}