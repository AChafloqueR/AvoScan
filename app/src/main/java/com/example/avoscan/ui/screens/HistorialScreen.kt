package com.example.avoscan.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avoscan.data.AnalisisEntity
import com.example.avoscan.data.AvoDatabase
import com.example.avoscan.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class FiltroHistorial(val label: String) {
    TODAS("Todas"),
    HOY("Hoy"),
    SEMANA("Esta Semana"),
    MES("Este Mes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen() {

    val context = LocalContext.current
    val dao = remember { AvoDatabase.getInstance(context).analisisDao() }
    val todos by dao.obtenerTodos().collectAsState(initial = emptyList())

    var filtro by remember { mutableStateOf(FiltroHistorial.TODAS) }
    val filtrados = remember(todos, filtro) { aplicarFiltro(todos, filtro) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Historial", fontWeight = FontWeight.SemiBold) },
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
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroHistorial.values().forEach { f ->
                    FilterChip(
                        selected = filtro == f,
                        onClick  = { filtro = f },
                        label    = { Text(f.label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor     = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filtrados.isEmpty()) {
                EstadoVacio(filtro)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtrados) { item ->
                        HistorialItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorialItemCard(item: AnalisisEntity) {

    val bitmap = remember(item.imagePath) {
        item.imagePath?.let {
            try { BitmapFactory.decodeFile(it) } catch (e: Exception) { null }
        }
    }

    val (color, severidadLabel) = estadoEstiloHistorial(item.estadoColor)
    val fechaFormateada = remember(item.fecha) {
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es")).format(Date(item.fecha))
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.etiqueta,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    fechaFormateada,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        severidadLabel,
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${(item.confianza * 100).toInt()}%",
                    color = color,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EstadoVacio(filtro: FiltroHistorial) {
    val mensaje = when (filtro) {
        FiltroHistorial.TODAS  -> "Aún no hay análisis. Comienza detectando una palta."
        FiltroHistorial.HOY    -> "No hay análisis registrados hoy."
        FiltroHistorial.SEMANA -> "No hay análisis en los últimos 7 días."
        FiltroHistorial.MES    -> "No hay análisis en el último mes."
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                mensaje,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

private fun estadoEstiloHistorial(estadoColor: String): Pair<Color, String> = when (estadoColor) {
    "ROJO"     -> SeverityHigh   to "Alta"
    "NARANJA"  -> SeverityMedium to "Media"
    "AMARILLO" -> SeverityWarn   to "Incierta"
    "VERDE"    -> SeverityLow    to "Sin riesgo"
    else       -> SeverityGray   to "—"
}

private fun aplicarFiltro(items: List<AnalisisEntity>, filtro: FiltroHistorial): List<AnalisisEntity> {
    if (filtro == FiltroHistorial.TODAS) return items

    val limite: Long = when (filtro) {
        FiltroHistorial.HOY -> Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        FiltroHistorial.SEMANA -> Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis
        FiltroHistorial.MES -> Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }.timeInMillis
        else -> 0L
    }
    return items.filter { it.fecha >= limite }
}