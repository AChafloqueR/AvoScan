package com.example.avoscan.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avoscan.ml.AvoClassifier
import com.example.avoscan.ml.EstadoDiagnostico
import com.example.avoscan.ui.theme.*

private data class InfoPlaga(
    val nombreComun: String,
    val nombreCientifico: String,
    val descripcion: String,
    val sintomas: List<String>,
    val recomendacion: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoScreen(
    resultado: AvoClassifier.Resultado?,
    onVolverClick: () -> Unit
) {
    val confianza = ((resultado?.confianza ?: 0f) * 100).toInt()
    val estado = resultado?.estado ?: EstadoDiagnostico.IMAGEN_INSUFICIENTE
    val mensaje = resultado?.mensaje ?: "Sin resultado"

    val info: InfoPlaga? = when (estado) {
        EstadoDiagnostico.SINTOMAS_ANTRACNOSIS -> InfoPlaga(
            "Antracnosis", "Colletotrichum gloeosporioides",
            "Enfermedad fúngica que genera manchas oscuras y hundidas en el fruto de la palta.",
            listOf("Manchas negras", "Hundimiento en la cáscara", "Pudrición progresiva"),
            "Separar frutos infectados y reducir humedad."
        )
        EstadoDiagnostico.SINTOMAS_RONA -> InfoPlaga(
            "Roña", "Sphaceloma perseae",
            "Produce lesiones ásperas y costrosas en la superficie del fruto.",
            listOf("Manchas rugosas", "Deformación del fruto"),
            "Aplicar control preventivo y monitoreo."
        )
        EstadoDiagnostico.SIN_SINTOMAS -> InfoPlaga(
            "Palta Saludable", "Sin enfermedad detectada",
            "No se detectaron enfermedades visibles en el fruto analizado.",
            listOf("Color uniforme", "Sin lesiones visibles"),
            "Mantener condiciones adecuadas de cultivo."
        )
        else -> null
    }

    val (colorEstado, severidadLabel, iconEstado) = when (estado.color) {
        com.example.avoscan.ml.ColorEstado.ROJO     -> Triple(SeverityHigh,   "Alta",       Icons.Default.BugReport)
        com.example.avoscan.ml.ColorEstado.NARANJA  -> Triple(SeverityMedium, "Media",      Icons.Default.BugReport)
        com.example.avoscan.ml.ColorEstado.AMARILLO -> Triple(SeverityWarn,   "Incierta",   Icons.Default.WarningAmber)
        com.example.avoscan.ml.ColorEstado.VERDE    -> Triple(SeverityLow,    "Sin riesgo", Icons.Default.CheckCircle)
        else                                        -> Triple(SeverityGray,   "—",          Icons.Default.WarningAmber)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Resultado de Detección", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Imagen analizada
            resultado?.imagePath?.let { path ->
                val bitmap = try { BitmapFactory.decodeFile(path) } catch (e: Exception) { null }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Card principal con estado + confianza + severidad
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(colorEstado.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(iconEstado, contentDescription = null, tint = colorEstado)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                info?.nombreComun ?: estado.titulo,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            info?.let {
                                Text(
                                    it.nombreCientifico,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("$confianza%",
                                color = colorEstado, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Confianza", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(severidadLabel,
                                color = colorEstado, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Severidad", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mensaje del estado (siempre)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Diagnóstico",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(mensaje,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp)
                }
            }

            // Cards de info detallada (solo si hay diagnóstico claro)
            info?.let { plaga ->
                Spacer(Modifier.height(12.dp))
                CardSeccion("Descripción", plaga.descripcion)

                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Síntomas",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        plaga.sintomas.forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colorEstado,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(s,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                CardSeccion("Recomendación", plaga.recomendacion)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CardSeccion(titulo: String, contenido: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(contenido,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp)
        }
    }
}