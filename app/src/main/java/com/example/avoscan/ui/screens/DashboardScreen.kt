package com.example.avoscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avoscan.data.AnalisisEntity
import com.example.avoscan.data.AvoDatabase
import com.example.avoscan.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onDetectarClick: () -> Unit,
    onVerHistorialClick: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AvoDatabase.getInstance(context).analisisDao() }

    val total           by dao.contarTotal().collectAsState(initial = 0)
    val alertas         by dao.contarAlertas().collectAsState(initial = 0)
    val recientes       by dao.obtenerRecientes(5).collectAsState(initial = emptyList())

    val hace30dias      = remember { System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 }
    val plagasActivas   by dao.contarPlagasActivas(hace30dias).collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        HeroCard(onDetectarClick)

        Spacer(Modifier.height(24.dp))
        SectionHeader("Estadísticas")
        Spacer(Modifier.height(12.dp))

        StatCardFull(
            icon     = Icons.Default.CameraAlt,
            iconTint = StatBlueIcon,
            iconBg   = StatBlueLight,
            valor    = total.toString(),
            etiqueta = "Detecciones"
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCardSmall(
                modifier = Modifier.weight(1f),
                icon     = Icons.Default.Warning,
                iconTint = StatOrangeIcon,
                iconBg   = StatOrangeLight,
                valor    = alertas.toString(),
                etiqueta = "Alertas"
            )
            StatCardSmall(
                modifier = Modifier.weight(1f),
                icon     = Icons.Default.BugReport,
                iconTint = StatRedIcon,
                iconBg   = StatRedLight,
                valor    = plagasActivas.toString(),
                etiqueta = "Plagas Activas"
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("Detecciones Recientes")
            Text(
                text = "Ver todas",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onVerHistorialClick() }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (recientes.isEmpty()) {
            EmptyState()
        } else {
            recientes.forEach { item ->
                DetectionListItem(item)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun HeroCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GreenPrimary),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "¡Bienvenido!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Mantén tus cultivos protegidos",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
            Icon(
                Icons.Default.Agriculture,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun StatCardFull(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    valor: String,
    etiqueta: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    valor,
                    color = iconTint,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    etiqueta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun StatCardSmall(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    valor: String,
    etiqueta: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                valor,
                color = iconTint,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                etiqueta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DetectionListItem(item: AnalisisEntity) {
    val (color, label) = estadoEstilo(item.estadoColor)
    val fecha = formatRelativeDate(item.fecha)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.etiqueta,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    fecha,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(item.confianza * 100).toInt()}%",
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Aún no hay análisis. ¡Comienza detectando una palta!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun estadoEstilo(estadoColor: String): Pair<Color, String> = when (estadoColor) {
    "ROJO"     -> SeverityHigh   to "Alta"
    "NARANJA"  -> SeverityMedium to "Media"
    "AMARILLO" -> SeverityWarn   to "Incierta"
    "VERDE"    -> SeverityLow    to "Sin riesgo"
    else       -> SeverityGray   to "—"
}

private fun formatRelativeDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val dias = diff / (24L * 60 * 60 * 1000)
    return when {
        dias < 1 -> "Hoy"
        dias < 2 -> "Ayer"
        dias < 7 -> "Hace $dias días"
        dias < 30 -> "Hace ${(dias / 7).toInt()} semanas"
        else -> SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date(timestamp))
    }
}