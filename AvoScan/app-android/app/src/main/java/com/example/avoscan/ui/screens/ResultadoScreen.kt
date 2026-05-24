package com.example.avoscan.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avoscan.ml.AvoClassifier

private data class InfoPlaga(
    val nombreComun: String,
    val nombreCientifico: String,
    val descripcion: String,
    val sintomas: List<String>,
    val recomendacion: String
)

@Composable
fun ResultadoScreen(
    resultado: AvoClassifier.Resultado?,
    onHistorialClick: () -> Unit
) {

    val etiqueta = resultado?.etiqueta ?: "Sin resultado"

    val confianza =
        ((resultado?.confianza ?: 0f) * 100).toInt()

    val info = when (etiqueta) {

        "Antracnosis" -> InfoPlaga(
            nombreComun = "Antracnosis",
            nombreCientifico = "Colletotrichum gloeosporioides",

            descripcion =
                "Enfermedad fúngica que genera manchas oscuras y hundidas en el fruto de la palta.",

            sintomas = listOf(
                "Manchas negras",
                "Hundimiento en la cáscara",
                "Pudrición progresiva"
            ),

            recomendacion =
                "Separar frutos infectados y reducir humedad."
        )

        "Roña" -> InfoPlaga(
            nombreComun = "Roña",
            nombreCientifico = "Sphaceloma perseae",

            descripcion =
                "Produce lesiones ásperas y costrosas en la superficie del fruto.",

            sintomas = listOf(
                "Manchas rugosas",
                "Deformación del fruto"
            ),

            recomendacion =
                "Aplicar control preventivo y monitoreo."
        )

        else -> InfoPlaga(
            nombreComun = "Palta Sana",
            nombreCientifico = "Sin enfermedad detectada",

            descripcion =
                "No se detectaron enfermedades visibles en el fruto analizado.",

            sintomas = listOf(
                "Color uniforme",
                "Sin lesiones visibles"
            ),

            recomendacion =
                "Mantener condiciones adecuadas de cultivo."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF163A24),
                        Color(0xFF081C15)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        // NOMBRE PRINCIPAL

        Text(
            text = info.nombreComun,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(5.dp))

        // NOMBRE CIENTIFICO

        Text(
            text = info.nombreCientifico,
            color = Color(0xFFB7E4C7),
            fontSize = 18.sp,
            fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(25.dp))

        // IMAGEN ANALIZADA

        resultado?.imagePath?.let { path ->

            val bitmap = try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                null
            }

            bitmap?.let {

                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(25.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CONFIANZA

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1B4332)
            ),

            shape = RoundedCornerShape(20.dp),

            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(18.dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "$confianza%",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Confianza del modelo",
                    color = Color(0xFFD8F3DC)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // DESCRIPCION

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2D6A4F)
            ),

            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Descripción",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = info.descripcion,
                    color = Color(0xFFD8F3DC),
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SINTOMAS

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF40916C)
            ),

            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Síntomas",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                info.sintomas.forEach {

                    Text(
                        text = "• $it",
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // RECOMENDACION
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF52B788)
            ),

            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Recomendación",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = info.recomendacion,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onHistorialClick,

            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),

            shape = RoundedCornerShape(20.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF95D5B2)
            )
        ) {

            Text(
                text = "Ver historial",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}