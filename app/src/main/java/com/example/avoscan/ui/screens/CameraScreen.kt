package com.example.avoscan.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avoscan.data.AnalisisEntity
import com.example.avoscan.data.AvoDatabase
import com.example.avoscan.ml.AvoClassifier
import com.example.avoscan.ui.components.CameraPreview
import com.example.avoscan.ui.theme.GreenPrimary
import com.example.avoscan.ui.theme.StatBlueIcon
import com.example.avoscan.ui.theme.StatBlueLight
import com.example.avoscan.utils.decodificarConOrientacion
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onResultado: (AvoClassifier.Resultado) -> Unit) {

    val context = LocalContext.current
    val classifier = remember { AvoClassifier(context) }
    val dao = remember { AvoDatabase.getInstance(context).analisisDao() }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) { onDispose { classifier.cerrar() } }

    var capturando by remember { mutableStateOf(false) }

    fun procesarYNavegar(resultado: AvoClassifier.Resultado) {
        scope.launch(Dispatchers.IO) {
            dao.insertar(
                AnalisisEntity(
                    fecha = System.currentTimeMillis(),
                    estadoTitulo = resultado.estado.titulo,
                    estadoColor = resultado.estado.color.name,
                    etiqueta = resultado.etiqueta,
                    confianza = resultado.confianza,
                    mensaje = resultado.mensaje,
                    imagePath = resultado.imagePath
                )
            )
        }
        onResultado(resultado)
    }

    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            val input = context.contentResolver.openInputStream(u)
            val bitmap = BitmapFactory.decodeStream(input)
            input?.close()

            // Guardar copia en cache para poder mostrarla luego
            val file = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val res = classifier.clasificar(bitmap)
            procesarYNavegar(res.copy(imagePath = file.absolutePath))
        }
    }

    if (capturando) {
        VistaCaptura(
            onCancel = { capturando = false },
            onCaptured = { file ->
                val bitmap = decodificarConOrientacion(file.absolutePath)
                val res = classifier.clasificar(bitmap)
                procesarYNavegar(res.copy(imagePath = file.absolutePath))
            }
        )
    } else {
        VistaDetectarLanding(
            onTomarFoto = { capturando = true },
            onAbrirGaleria = { launcherGaleria.launch("image/*") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VistaDetectarLanding(
    onTomarFoto: () -> Unit,
    onAbrirGaleria: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Detectar Plaga", fontWeight = FontWeight.SemiBold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // Icono grande
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(GreenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(70.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Detecta plagas en tus cultivos",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Toma una foto o selecciona una imagen de tu galería",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // Botón principal: Tomar Foto
            Button(
                onClick = onTomarFoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tomar Foto", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // Botón secundario: Galería
            OutlinedButton(
                onClick = onAbrirGaleria,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(GreenPrimary)
                )
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = GreenPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Seleccionar de Galería",
                    color = GreenPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Tip card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StatBlueLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = StatBlueIcon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Toma fotos claras y enfocadas para mejores resultados",
                        color = StatBlueIcon,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun VistaCaptura(
    onCancel: () -> Unit,
    onCaptured: (File) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var triggerCapture by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { cameraPermissionState.launchPermissionRequest() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(
                triggerCapture = triggerCapture,
                onCaptureDone = { triggerCapture = false },
                onImageCaptured = onCaptured
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Permiso de cámara requerido", color = Color.White)
            }
        }

        // Back arrow
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
        }

        // Shutter
        FloatingActionButton(
            onClick = { triggerCapture = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .size(76.dp),
            containerColor = Color.White,
            shape = RoundedCornerShape(38.dp)
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = "Capturar",
                tint = GreenPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}