package com.example.avoscan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.avoscan.data.AvoDatabase
import com.example.avoscan.ml.AvoClassifier
import com.example.avoscan.ml.EstadoDiagnostico

@Composable
fun ResultadoHistorialScreen(
    analisisId: Long,
    onVolverClick: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AvoDatabase.getInstance(context).analisisDao() }

    var resultado by remember { mutableStateOf<AvoClassifier.Resultado?>(null) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(analisisId) {
        val entity = dao.obtenerPorId(analisisId)
        if (entity != null) {
            // Reconstruir el Resultado desde el Entity guardado
            resultado = AvoClassifier.Resultado(
                etiqueta = entity.etiqueta,
                confianza = entity.confianza,
                todas = emptyList(),  // no se necesita para mostrar
                estado = mapearEstado(entity.estadoColor, entity.etiqueta),
                mensaje = entity.mensaje,  // ← usamos el guardado en BD
                imagePath = entity.imagePath
            )
        }
        cargando = false
    }

    when {
        cargando -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        resultado == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se pudo cargar el análisis")
            }
        }
        else -> {
            ResultadoScreen(
                resultado = resultado,
                onVolverClick = onVolverClick
            )
        }
    }
}

private fun mapearEstado(colorGuardado: String, etiqueta: String): EstadoDiagnostico {
    return when (colorGuardado) {
        "VERDE"      -> EstadoDiagnostico.SIN_SINTOMAS
        "ROJO"       -> EstadoDiagnostico.SINTOMAS_ANTRACNOSIS
        "NARANJA"    -> EstadoDiagnostico.SINTOMAS_RONA
        "AMARILLO"   -> EstadoDiagnostico.INFECCION_LATENTE
        "GRIS_CLARO" -> EstadoDiagnostico.NO_ES_PALTA
        else         -> {

            when (etiqueta) {
                "Antracnosis" -> EstadoDiagnostico.SINTOMAS_ANTRACNOSIS
                "Roña"        -> EstadoDiagnostico.SINTOMAS_RONA
                "Sano"        -> EstadoDiagnostico.SIN_SINTOMAS
                "No-palta"    -> EstadoDiagnostico.NO_ES_PALTA
                else          -> EstadoDiagnostico.IMAGEN_INSUFICIENTE
            }
        }
    }
}