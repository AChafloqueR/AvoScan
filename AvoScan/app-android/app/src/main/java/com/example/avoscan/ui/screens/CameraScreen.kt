package com.example.avoscan.ui.screens

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avoscan.data.local.HistorialManager
import com.example.avoscan.ml.AvoClassifier
import com.example.avoscan.ui.components.CameraPreview
import com.example.avoscan.utils.decodificarConOrientacion
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onResultado: (AvoClassifier.Resultado) -> Unit
) {
    val context = LocalContext.current
    val classifier = remember { AvoClassifier(context) }
    DisposableEffect(Unit) { onDispose { classifier.cerrar() } }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var triggerCapture by remember { mutableStateOf(false) }

    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        uri?.let {

            val inputStream =
                context.contentResolver.openInputStream(it)

            val bitmap =
                BitmapFactory.decodeStream(inputStream)

            val res =
                classifier.clasificar(bitmap)

            val nuevoResultado = res.copy(
                imagePath = it.toString()
            )

            onResultado(nuevoResultado)
        }
    }

    LaunchedEffect(Unit) { cameraPermissionState.launchPermissionRequest() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B4332), Color(0xFF081C15))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text("Escaneo AvoScan", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                shape = RoundedCornerShape(30.dp)
            ) {
                if (cameraPermissionState.status.isGranted) {
                    CameraPreview(
                        triggerCapture = triggerCapture,
                        onCaptureDone = { triggerCapture = false },
                        onImageCaptured = { file ->
                            val bitmap = decodificarConOrientacion(file.absolutePath)

                            val res = classifier.clasificar(bitmap)

                            val nuevoResultado = res.copy(
                                imagePath = file.absolutePath
                            )

                            onResultado(nuevoResultado)
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Permiso de cámara requerido")
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { triggerCapture = true },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF52B788))
            ) {
                Text("Capturar imagen", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = {
                    launcherGaleria.launch("image/*")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF40916C)
                )
            ) {

                Text(
                    text = "Seleccionar de galería",
                    fontSize = 18.sp
                )
            }
        }
    }
}
