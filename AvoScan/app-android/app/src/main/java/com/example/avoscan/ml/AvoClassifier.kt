package com.example.avoscan.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class AvoClassifier(context: Context) {
    private val modelFile = "MobileNetV3Small_float16.tflite"

    private val etiquetas = listOf("Antracnosis", "Roña", "Sano")
    private val tam = 224

    private val interpreter: Interpreter = Interpreter(cargarModelo(context, modelFile))

    data class Resultado(
        val etiqueta: String,
        val confianza: Float,
        val todas: List<Pair<String, Float>>,
        val imagePath: String? = null
    )

    fun clasificar(bitmap: Bitmap): Resultado {

        val entrada = bitmapAByteBuffer(bitmap)
        val salida = Array(1) { FloatArray(etiquetas.size) }
        interpreter.run(entrada, salida)
        val probs = salida[0]
        val idxMax = probs.indices.maxByOrNull { probs[it] } ?: 0
        val todas = etiquetas.indices.map {
            etiquetas[it] to probs[it]
        }
        println(todas)
        return Resultado(
            etiquetas[idxMax],
            probs[idxMax],
            todas
        )
    }

    // Convierte el Bitmap a la entrada del modelo: 224x224, RGB, valores [0-255] SIN normalizar
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