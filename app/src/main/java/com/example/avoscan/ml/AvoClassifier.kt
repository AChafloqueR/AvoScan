package com.example.avoscan.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

enum class ColorEstado { VERDE, AMARILLO, NARANJA, ROJO, GRIS }

enum class EstadoDiagnostico(val titulo: String, val color: ColorEstado) {
    SIN_SINTOMAS("Sin síntomas visibles", ColorEstado.VERDE),
    SINTOMAS_ANTRACNOSIS("Síntomas de antracnosis", ColorEstado.ROJO),
    SINTOMAS_RONA("Síntomas de roña", ColorEstado.NARANJA),
    INFECCION_LATENTE("Diagnóstico no concluyente", ColorEstado.AMARILLO),
    IMAGEN_INSUFICIENTE("Imagen insuficiente", ColorEstado.GRIS)
}

class AvoClassifier(context: Context) {
    private val modelFile = "MobileNetV3Small_float16.tflite"
    private val etiquetas = listOf("Antracnosis", "Roña", "Sano")
    private val tam = 224
    private val interpreter: Interpreter = Interpreter(cargarModelo(context, modelFile))

    companion object {
        const val UMBRAL_INSUFICIENTE = 0.60f
        const val UMBRAL_CONFIANZA_CLARA = 0.85f
        const val UMBRAL_MARGEN_MINIMO = 0.25f   // gap mínimo entre top1 y top2
    }

    data class Resultado(
        val etiqueta: String,
        val confianza: Float,
        val todas: List<Pair<String, Float>>,
        val estado: EstadoDiagnostico,
        val mensaje: String,
        val imagePath: String? = null
    )

    fun clasificar(bitmap: Bitmap): Resultado {
        val entrada = bitmapAByteBuffer(bitmap)
        val salida = Array(1) { FloatArray(etiquetas.size) }
        interpreter.run(entrada, salida)
        val probs = salida[0]

        val ordenados = probs.indices.sortedByDescending { probs[it] }
        val idxTop = ordenados[0]
        val idxSegundo = ordenados[1]
        val confTop = probs[idxTop]
        val confSegundo = probs[idxSegundo]
        val margen = confTop - confSegundo
        val claseTop = etiquetas[idxTop]

        val todas = etiquetas.indices.map { etiquetas[it] to probs[it] }
        val (estado, mensaje) = interpretarEstado(claseTop, confTop, margen)

        println("Probs: $todas | Margen: $margen | Estado: ${estado.titulo}")

        return Resultado(
            etiqueta = claseTop,
            confianza = confTop,
            todas = todas,
            estado = estado,
            mensaje = mensaje
        )
    }

    private fun interpretarEstado(
        clase: String,
        confianza: Float,
        margen: Float
    ): Pair<EstadoDiagnostico, String> {
        return when {
            confianza < UMBRAL_INSUFICIENTE -> Pair(
                EstadoDiagnostico.IMAGEN_INSUFICIENTE,
                "No fue posible un diagnóstico confiable. Tome otra foto con mejor iluminación, fondo claro y el fruto centrado."
            )
            margen < UMBRAL_MARGEN_MINIMO || confianza < UMBRAL_CONFIANZA_CLARA -> Pair(
                EstadoDiagnostico.INFECCION_LATENTE,
                "El modelo identifica señales ambiguas. Puede tratarse de una infección en etapa temprana, asintomática, o un caso visualmente parecido entre dos patologías. Se recomienda repetir el análisis o consultar a un técnico fitopatológico."
            )
            clase == "Sano" -> Pair(
                EstadoDiagnostico.SIN_SINTOMAS,
                "No se detectan síntomas visibles de enfermedad. Tenga en cuenta que la antracnosis puede permanecer latente y manifestarse luego de la cosecha."
            )
            clase == "Antracnosis" -> Pair(
                EstadoDiagnostico.SINTOMAS_ANTRACNOSIS,
                "Se detectaron lesiones compatibles con antracnosis (Colletotrichum gloeosporioides). Se recomienda separar el fruto y aplicar manejo fitosanitario adecuado."
            )
            else -> Pair(
                EstadoDiagnostico.SINTOMAS_RONA,
                "Se detectaron lesiones compatibles con roña (Sphaceloma perseae). Aunque la afectación suele ser superficial, evalúe el destino comercial del fruto."
            )
        }
    }

    private fun bitmapAByteBuffer(bitmap: Bitmap): ByteBuffer {
        val redim = Bitmap.createScaledBitmap(bitmap, tam, tam, true)
        val buffer = ByteBuffer.allocateDirect(tam * tam * 3 * 4).order(ByteOrder.nativeOrder())
        val pixeles = IntArray(tam * tam)
        redim.getPixels(pixeles, 0, tam, 0, 0, tam, tam)
        for (p in pixeles) {
            val r = ((p shr 16) and 0xFF).toFloat()
            val g = ((p shr 8) and 0xFF).toFloat()
            val b = (p and 0xFF).toFloat()
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }
        buffer.rewind()
        return buffer
    }

    private fun cargarModelo(context: Context, nombre: String): MappedByteBuffer {
        val afd = context.assets.openFd(nombre)
        val input = FileInputStream(afd.fileDescriptor)
        val buffer = input.channel.map(
            FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength
        )
        input.close()
        afd.close()
        return buffer
    }

    fun cerrar() = interpreter.close()
}