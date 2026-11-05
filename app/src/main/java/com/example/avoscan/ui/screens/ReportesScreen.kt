package com.example.avoscan.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen() {

    val context = LocalContext.current
    val dao = remember { AvoDatabase.getInstance(context).analisisDao() }
    val analisis by dao.obtenerTodos().collectAsState(initial = emptyList())

    val estadisticas = remember(analisis) { calcularEstadisticas(analisis) }
    val datosMes     = remember(analisis) { agruparPlagasPorMes(analisis) }
    val distribucion = remember(analisis) { calcularDistribucion(analisis) }
    val calidad      = remember(analisis) { calcularCalidadModelo(analisis) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reportes y Análisis", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = GreenPrimary,
                    titleContentColor = Color.White
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
            Text("Resumen de Detección",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatMiniCard(Modifier.weight(1f), Icons.Default.CameraAlt,
                    StatBlueIcon, StatBlueLight, estadisticas.total.toString(), "Total Detecciones")
                StatMiniCard(Modifier.weight(1f), Icons.Default.BarChart,
                    StatOrangeIcon, StatOrangeLight, "%.1f".format(estadisticas.promedio), "Promedio")
                StatMiniCard(Modifier.weight(1f), Icons.Default.TrendingUp,
                    StatGreenIcon, StatGreenLight, estadisticas.maximoMes.toString(), "Máximo Mes")
            }

            Spacer(Modifier.height(20.dp))

            CardSeccion(titulo = "Número de Plagas Detectadas por Mes") {
                BarChart(datosMes)
            }

            Spacer(Modifier.height(16.dp))

            CardSeccion(titulo = "Distribución por Enfermedad") {
                DonutChart(distribucion)
            }

            Spacer(Modifier.height(16.dp))

            CardSeccion(titulo = "Calidad del Modelo") {
                CalidadModeloView(calidad)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CardSeccion(titulo: String, content: @Composable () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun StatMiniCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    valor: String,
    etiqueta: String
) {
    Card(
        modifier  = modifier,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(valor, color = iconTint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BarChart(items: List<DatoMes>) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(180.dp), Alignment.Center) {
            Text("Aún no hay datos suficientes",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        return
    }

    val maxValue       = items.maxOf { it.plagas }.coerceAtLeast(1)
    val chartHeightDp  = 180
    val labelSpaceDp   = 24
    val maxBarHeightDp = chartHeightDp - labelSpaceDp

    Column {
        Row(Modifier.fillMaxWidth().height(chartHeightDp.dp)) {
            Column(
                Modifier.fillMaxHeight().padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$maxValue detecciones", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0 detecciones", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { dato ->
                    val fraction = dato.plagas.toFloat() / maxValue
                    val barHeightDp = (maxBarHeightDp * fraction).toInt()
                    val spacerAboveDp = maxBarHeightDp - barHeightDp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight().weight(1f)
                    ) {
                        Spacer(Modifier.height(spacerAboveDp.dp))
                        Text("${dato.plagas} detecciones",
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier
                            .width(48.dp)
                            .height(barHeightDp.dp.coerceAtLeast(4.dp))
                            .background(GreenPrimary,
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(start = 80.dp)) {
            items.forEach { dato ->
                Text(dato.mes,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DonutChart(items: List<DatoDistribucion>) {
    val total = items.sumOf { it.cantidad }.toFloat()
    if (total == 0f) {
        Box(Modifier.fillMaxWidth().height(160.dp), Alignment.Center) {
            Text("Sin datos suficientes",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        return
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                var startAngle = -90f
                val strokeW = 30.dp.toPx()
                val padding = strokeW / 2
                items.forEach { d ->
                    val sweep = (d.cantidad / total) * 360f
                    drawArc(
                        color = d.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(padding, padding),
                        size = Size(size.width - strokeW, size.height - strokeW),
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${total.toInt()}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("análisis",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            items.forEach { d ->
                val pct = (d.cantidad / total * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(d.color))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.label, color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${d.cantidad} ($pct%)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalidadModeloView(calidad: CalidadModelo) {
    if (calidad.totalAnalisis == 0) {
        Text("Aún no hay análisis registrados",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        return
    }

    ConfianzaBar("Promedio general", calidad.promedioGeneral, GreenPrimary)
    calidad.porClase.forEach { (label, valor, color) ->
        ConfianzaBar(label, valor, color)
    }
}

@Composable
private fun ConfianzaBar(label: String, valor: Double, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Text("%.1f%%".format(valor),
                color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (valor / 100).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

// === Modelos de datos privados ===

private data class EstadisticasReporte(val total: Int, val promedio: Double, val maximoMes: Int)
private data class DatoMes(val mes: String, val plagas: Int)
private data class DatoDistribucion(val label: String, val cantidad: Int, val color: Color)
private data class CalidadModelo(
    val totalAnalisis: Int,
    val promedioGeneral: Double,
    val porClase: List<Triple<String, Double, Color>>
)

// === Cálculos ===

private fun calcularEstadisticas(list: List<AnalisisEntity>): EstadisticasReporte {
    if (list.isEmpty()) return EstadisticasReporte(0, 0.0, 0)
    val grouped = list.groupBy {
        val cal = Calendar.getInstance().apply { timeInMillis = it.fecha }
        cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
    }
    val cantidadesPorMes = grouped.values.map { it.size }
    return EstadisticasReporte(list.size, cantidadesPorMes.average(), cantidadesPorMes.max())
}

private fun agruparPlagasPorMes(list: List<AnalisisEntity>): List<DatoMes> {
    val plagas = list.filter { it.estadoColor in listOf("ROJO", "NARANJA") }
    if (plagas.isEmpty()) return emptyList()
    return plagas.groupBy {
        val cal = Calendar.getInstance().apply { timeInMillis = it.fecha }
        cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
    }
        .entries.sortedBy { it.key }
        .map { (_, items) ->
            val mes = SimpleDateFormat("MMM", Locale("es"))
                .format(Date(items.first().fecha))
                .replaceFirstChar { it.uppercase() }
                .removeSuffix(".")
            DatoMes(mes, items.size)
        }
}

private fun calcularDistribucion(list: List<AnalisisEntity>): List<DatoDistribucion> {
    val antracnosis  = list.count { it.estadoColor == "ROJO" }
    val rona         = list.count { it.estadoColor == "NARANJA" }
    val sano         = list.count { it.estadoColor == "VERDE" }
    val incierto     = list.count { it.estadoColor == "AMARILLO" }
    val insuficiente = list.count { it.estadoColor == "GRIS" }

    return listOfNotNull(
        if (antracnosis > 0)  DatoDistribucion("Antracnosis", antracnosis,  SeverityHigh)   else null,
        if (rona > 0)         DatoDistribucion("Roña",        rona,         SeverityMedium) else null,
        if (sano > 0)         DatoDistribucion("Saludable",   sano,         SeverityLow)    else null,
        if (incierto > 0)     DatoDistribucion("Incierto",    incierto,     SeverityWarn)   else null,
        if (insuficiente > 0) DatoDistribucion("Insuficiente", insuficiente, SeverityGray)  else null
    )
}

private fun calcularCalidadModelo(list: List<AnalisisEntity>): CalidadModelo {
    if (list.isEmpty()) return CalidadModelo(0, 0.0, emptyList())

    val promedioGeneral = list.map { it.confianza.toDouble() }.average() * 100

    val antracnosis = list.filter { it.estadoColor == "ROJO" }
    val rona        = list.filter { it.estadoColor == "NARANJA" }
    val sano        = list.filter { it.estadoColor == "VERDE" }

    val porClase = mutableListOf<Triple<String, Double, Color>>()
    if (antracnosis.isNotEmpty())
        porClase.add(Triple("Antracnosis",
            antracnosis.map { it.confianza.toDouble() }.average() * 100, SeverityHigh))
    if (rona.isNotEmpty())
        porClase.add(Triple("Roña",
            rona.map { it.confianza.toDouble() }.average() * 100, SeverityMedium))
    if (sano.isNotEmpty())
        porClase.add(Triple("Saludable",
            sano.map { it.confianza.toDouble() }.average() * 100, SeverityLow))

    return CalidadModelo(list.size, promedioGeneral, porClase)
}