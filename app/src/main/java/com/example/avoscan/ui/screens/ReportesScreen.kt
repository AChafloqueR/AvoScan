package com.example.avoscan.ui.screens

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reportes y Análisis", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor      = GreenPrimary,
                    titleContentColor   = Color.White
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
            Text(
                "Resumen de Detección",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Default.CameraAlt,
                    iconTint = StatBlueIcon,
                    iconBg   = StatBlueLight,
                    valor    = estadisticas.total.toString(),
                    etiqueta = "Total Detecciones"
                )
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Default.BarChart,
                    iconTint = StatOrangeIcon,
                    iconBg   = StatOrangeLight,
                    valor    = "%.1f".format(estadisticas.promedio),
                    etiqueta = "Promedio"
                )
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Default.TrendingUp,
                    iconTint = StatGreenIcon,
                    iconBg   = StatGreenLight,
                    valor    = estadisticas.maximoMes.toString(),
                    etiqueta = "Máximo Mes"
                )
            }

            Spacer(Modifier.height(20.dp))

            Card(
                modifier  = Modifier.fillMaxWidth(),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Número de Plagas Detectadas por Mes",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    BarChart(datosMes)
                }
            }

            Spacer(Modifier.height(20.dp))
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(valor,    color = iconTint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BarChart(items: List<DatoMes>) {

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Aún no hay datos suficientes para mostrar",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    val maxValue       = items.maxOf { it.plagas }.coerceAtLeast(1)
    val chartHeightDp  = 180
    val labelSpaceDp   = 24
    val maxBarHeightDp = chartHeightDp - labelSpaceDp

    Column {
        Row(modifier = Modifier.fillMaxWidth().height(chartHeightDp.dp)) {

            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$maxValue detecciones", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0 detecciones",        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { dato ->
                    val fraction       = dato.plagas.toFloat() / maxValue
                    val barHeightDp    = (maxBarHeightDp * fraction).toInt()
                    val spacerAboveDp  = maxBarHeightDp - barHeightDp

                    Column(
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.Bottom,
                        modifier              = Modifier.fillMaxHeight().weight(1f)
                    ) {
                        Spacer(Modifier.height(spacerAboveDp.dp))
                        Text(
                            "${dato.plagas} detecciones",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(barHeightDp.dp.coerceAtLeast(4.dp))
                                .background(
                                    GreenPrimary,
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(start = 80.dp)) {
            items.forEach { dato ->
                Text(
                    dato.mes,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private data class EstadisticasReporte(
    val total: Int,
    val promedio: Double,
    val maximoMes: Int
)

private data class DatoMes(
    val mes: String,
    val plagas: Int
)

private fun calcularEstadisticas(list: List<AnalisisEntity>): EstadisticasReporte {
    if (list.isEmpty()) return EstadisticasReporte(0, 0.0, 0)

    val grouped = list.groupBy {
        val cal = Calendar.getInstance().apply { timeInMillis = it.fecha }
        cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
    }
    val cantidadesPorMes = grouped.values.map { it.size }
    val promedio = cantidadesPorMes.average()
    val maximo   = cantidadesPorMes.maxOrNull() ?: 0
    return EstadisticasReporte(list.size, promedio, maximo)
}

private fun agruparPlagasPorMes(list: List<AnalisisEntity>): List<DatoMes> {
    val plagas = list.filter { it.estadoColor in listOf("ROJO", "NARANJA") }
    if (plagas.isEmpty()) return emptyList()

    return plagas
        .groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.fecha }
            cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
        }
        .entries
        .sortedBy { it.key }
        .map { (_, items) ->
            val fecha = Date(items.first().fecha)
            val mesLabel = SimpleDateFormat("MMM", Locale("es"))
                .format(fecha)
                .replaceFirstChar { it.uppercase() }
                .removeSuffix(".")
            DatoMes(mesLabel, items.size)
        }
}