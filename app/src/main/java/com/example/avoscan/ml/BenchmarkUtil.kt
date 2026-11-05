package com.example.avoscan.ml

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.TensorFlowLite
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Benchmark de eficiencia en el dispositivo (OE2, OE3 y OE5).
 *
 * Genera automáticamente, en /Android/data/<paquete>/files/benchmark_<fecha>/ :
 *   config.csv            configuración del intérprete y del dispositivo
 *   latencia.csv          100 imágenes distintas x 3 modelos x 10 corridas
 *   ram.csv               PSS antes/después de cargar y pico, por corrida
 *   energia.csv           resumen energético por corrida y modelo
 *   energia_muestras.csv  lecturas de corriente cada segundo (Anexo 2)
 *
 * Requisitos:
 *   - 100 imágenes del test (224 x 224) en app/src/main/assets/benchmark/
 *   - Los tres .tflite Float16 reentrenados en app/src/main/assets/
 *   - Teléfono SIN cargador, en Modo Avión y con la pantalla siempre encendida.
 */
object BenchmarkUtil {

    private const val TAG = "AvoBench"
    private const val IMG = 224
    private const val CORRIDAS = 10
    private const val WARMUP = 50
    private const val BASE_SEG = 60
    private const val ENERGIA_SEG = 120
    private const val IMAGENES_ENERGIA = 10
    private const val PAUSA_ENTRE_MODELOS_MS = 30_000L
    private const val HILOS = 4
    private const val CARPETA_IMAGENES = "benchmark"

    /** MobileNetV2 se exportó sin la normalización dentro del modelo: espera valores en [-1, 1]. */
    private data class Modelo(val nombre: String, val asset: String, val entradaMenos1a1: Boolean)

    private val MODELOS = listOf(
        Modelo("MobileNetV3-Small", "mobilenetv3small_float16.tflite", false),
        Modelo("MobileNetV2", "mobilenetv2_float16.tflite", true),
        Modelo("EfficientNet-B0", "efficientnetb0_float16.tflite", false)
    )

    private class Imagen(val id: String, val rgb: ByteArray)

    private data class EstadoBateria(val nivel: Int, val temperaturaC: Double, val cargando: Boolean)

    // ------------------------------------------------------------------ utilidades

    private fun estadoBateria(ctx: Context): EstadoBateria {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val nivel = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val enchufado = (i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        return EstadoBateria(nivel, temp, enchufado)
    }

    /** Corriente instantánea en µA (valor absoluto; el signo depende del fabricante). */
    private fun corrienteUa(bm: BatteryManager): Long =
        abs(bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))

    /** PSS total del proceso en MB (incluye el heap nativo donde TFLite aloja pesos y tensores). */
    private fun pssMb(): Double {
        val mi = Debug.MemoryInfo()
        Debug.getMemoryInfo(mi)
        return mi.totalPss / 1024.0
    }

    private fun f3(v: Double) = String.format(Locale.US, "%.3f", v)
    private fun f4(v: Double) = String.format(Locale.US, "%.4f", v)
    private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)

    private fun abrirInterprete(ctx: Context, asset: String): Interpreter {
        ctx.assets.openFd(asset).use { afd ->
            FileInputStream(afd.fileDescriptor).use { fis ->
                val modelo = fis.channel.map(
                    FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength
                )
                return Interpreter(modelo, Interpreter.Options().setNumThreads(HILOS))
            }
        }
    }

    private fun cargarImagenes(ctx: Context): List<Imagen> {
        val nombres = ctx.assets.list(CARPETA_IMAGENES)?.sorted().orEmpty()
        require(nombres.isNotEmpty()) { "No hay imágenes en assets/$CARPETA_IMAGENES" }
        return nombres.map { n ->
            val bmp = ctx.assets.open("$CARPETA_IMAGENES/$n").use { BitmapFactory.decodeStream(it) }
            val esc = if (bmp.width == IMG && bmp.height == IMG) bmp
            else Bitmap.createScaledBitmap(bmp, IMG, IMG, true)
            val px = IntArray(IMG * IMG)
            esc.getPixels(px, 0, IMG, 0, 0, IMG, IMG)
            val rgb = ByteArray(IMG * IMG * 3)
            var j = 0
            for (p in px) {
                rgb[j++] = ((p shr 16) and 0xFF).toByte()
                rgb[j++] = ((p shr 8) and 0xFF).toByte()
                rgb[j++] = (p and 0xFF).toByte()
            }
            Imagen(n.substringBeforeLast('.'), rgb)
        }
    }

    private fun nuevoBuffer(): ByteBuffer =
        ByteBuffer.allocateDirect(IMG * IMG * 3 * 4).order(ByteOrder.nativeOrder())

    /** Rellena el tensor de entrada. Se llama FUERA del cronómetro. */
    private fun llenar(buf: ByteBuffer, img: Imagen, menos1a1: Boolean) {
        buf.rewind()
        for (b in img.rgb) {
            val v = (b.toInt() and 0xFF).toFloat()
            buf.putFloat(if (menos1a1) v / 127.5f - 1f else v)
        }
        buf.rewind()
    }

    /** Muestrea la corriente cada segundo mientras se ejecuta [trabajo]. */
    private fun muestrearCorriente(bm: BatteryManager, trabajo: () -> Unit): List<Long> {
        val muestras = Collections.synchronizedList(ArrayList<Long>())
        val ex = Executors.newSingleThreadScheduledExecutor()
        ex.scheduleAtFixedRate({ muestras.add(corrienteUa(bm)) }, 0, 1, TimeUnit.SECONDS)
        trabajo()
        ex.shutdown()
        ex.awaitTermination(3, TimeUnit.SECONDS)
        return ArrayList(muestras)
    }

    // ------------------------------------------------------------------ medición de un modelo

    private fun medirModelo(
        ctx: Context, dir: File, corrida: Int, posicion: Int, m: Modelo,
        imagenes: List<Imagen>, baseUa: Double, bm: BatteryManager
    ) {
        Log.i(TAG, "Corrida $corrida | posición $posicion | ${m.nombre}")

        // --- RAM: PSS antes y después de cargar ---
        System.gc(); Thread.sleep(2000)
        val pssAntes = pssMb()
        val itp = abrirInterprete(ctx, m.asset)
        val salida = Array(1) { FloatArray(4) }
        val buf = nuevoBuffer()
        llenar(buf, imagenes[0], m.entradaMenos1a1)
        itp.run(buf, salida)                       // primera ejecución: reserva los tensores
        val pssCarga = pssMb()

        // --- Calentamiento (descartado) ---
        repeat(WARMUP) { buf.rewind(); itp.run(buf, salida) }

        // --- Latencia: 100 imágenes distintas, solo la llamada al intérprete ---
        var pssPico = pssCarga
        val lat = StringBuilder()
        imagenes.forEachIndexed { k, img ->
            llenar(buf, img, m.entradaMenos1a1)
            val t0 = System.nanoTime()
            itp.run(buf, salida)
            val ms = (System.nanoTime() - t0) / 1_000_000.0
            lat.append("$corrida,$posicion,${m.nombre},${img.id},${f3(ms)}\n")
            if (k % 10 == 0) pssPico = maxOf(pssPico, pssMb())   // fuera del cronómetro
        }
        File(dir, "latencia.csv").appendText(lat.toString())

        File(dir, "ram.csv").appendText(
            "$corrida,${m.nombre},${f3(pssAntes)},${f3(pssCarga)},${f3(pssPico)},${f3(pssPico - pssAntes)}\n"
        )

        // --- Energía: inferencia continua durante ENERGIA_SEG segundos ---
        val buffers = imagenes.take(IMAGENES_ENERGIA).map { img ->
            nuevoBuffer().also { llenar(it, img, m.entradaMenos1a1) }
        }
        var inferencias = 0
        val muestras = muestrearCorriente(bm) {
            val fin = SystemClock.elapsedRealtime() + ENERGIA_SEG * 1000L
            while (SystemClock.elapsedRealtime() < fin) {
                val b = buffers[inferencias % buffers.size]
                b.rewind()
                itp.run(b, salida)
                inferencias++
            }
        }
        val mediaUa = muestras.average()
        val netaUa = mediaUa - baseUa
        val mAhTotal = (netaUa / 1000.0) * (ENERGIA_SEG / 3600.0)
        val mAhPor1000 = mAhTotal / inferencias * 1000.0
        val estado = estadoBateria(ctx)

        File(dir, "energia.csv").appendText(
            "$corrida,${m.nombre},${f1(baseUa)},${f1(mediaUa)},${f1(netaUa)},$inferencias," +
                    "$ENERGIA_SEG,${f4(mAhPor1000)},${estado.nivel},${f1(estado.temperaturaC)}\n"
        )
        val sb = StringBuilder()
        muestras.forEachIndexed { s, ua -> sb.append("$corrida,${m.nombre},$s,$ua\n") }
        File(dir, "energia_muestras.csv").appendText(sb.toString())

        Log.i(TAG, "  RAM ${f3(pssPico - pssAntes)} MB | ${f4(mAhPor1000)} mAh/1000 | $inferencias inferencias")
        itp.close()
    }

    // ------------------------------------------------------------------ ejecución completa

    fun ejecutar(ctx: Context) {
        Thread {
            try {
                correr(ctx.applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "ERROR: ${e.message}", e)
            }
        }.apply { priority = Thread.MAX_PRIORITY }.start()
    }

    private fun correr(ctx: Context) {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val inicio = estadoBateria(ctx)
        if (inicio.cargando) {
            Log.e(TAG, "El teléfono está conectado al cargador. Desconéctalo y vuelve a abrir la app.")
            return
        }

        val fecha = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val dir = File(ctx.getExternalFilesDir(null), "benchmark_$fecha").apply { mkdirs() }
        val imagenes = cargarImagenes(ctx)

        File(dir, "config.csv").writeText(buildString {
            appendLine("clave,valor")
            appendLine("dispositivo,${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android,${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("tflite_runtime,${TensorFlowLite.runtimeVersion()}")
            appendLine("hilos_cpu,$HILOS")
            appendLine("delegado,ninguno (CPU)")
            appendLine("corridas,$CORRIDAS")
            appendLine("calentamiento,$WARMUP")
            appendLine("imagenes_latencia,${imagenes.size}")
            appendLine("linea_base_s,$BASE_SEG")
            appendLine("energia_s,$ENERGIA_SEG")
            appendLine("pausa_entre_modelos_s,${PAUSA_ENTRE_MODELOS_MS / 1000}")
            appendLine("bateria_inicio_pct,${inicio.nivel}")
            appendLine("temperatura_inicio_C,${f1(inicio.temperaturaC)}")
        })
        File(dir, "latencia.csv").writeText("corrida,posicion,modelo,id_imagen,latencia_ms\n")
        File(dir, "ram.csv").writeText("corrida,modelo,pss_antes_mb,pss_tras_carga_mb,pss_pico_mb,ram_atribuible_mb\n")
        File(dir, "energia.csv").writeText(
            "corrida,modelo,corriente_base_uA,corriente_media_uA,corriente_neta_uA," +
                    "inferencias,duracion_s,mAh_por_1000,bateria_pct,temperatura_C\n"
        )
        File(dir, "energia_muestras.csv").writeText("corrida,modelo,segundo,corriente_uA\n")

        Log.i(TAG, "### INICIO BENCHMARK ### carpeta: ${dir.absolutePath}")

        for (c in 1..CORRIDAS) {
            if (estadoBateria(ctx).cargando) {
                Log.e(TAG, "Se conectó el cargador en la corrida $c. Benchmark detenido.")
                return
            }
            // Línea base en reposo (app abierta, pantalla encendida, sin inferencia)
            val base = muestrearCorriente(bm) { Thread.sleep(BASE_SEG * 1000L) }.average()
            Log.i(TAG, "Corrida $c | línea base ${f1(base)} uA")

            // Orden rotado en cada corrida para que ningún modelo vaya siempre primero
            val orden = MODELOS.indices.map { MODELOS[(it + c - 1) % MODELOS.size] }
            orden.forEachIndexed { i, m ->
                medirModelo(ctx, dir, c, i + 1, m, imagenes, base, bm)
                System.gc()
                Thread.sleep(PAUSA_ENTRE_MODELOS_MS)   // enfriamiento entre modelos
            }
        }

        val fin = estadoBateria(ctx)
        File(dir, "config.csv").appendText(
            "bateria_fin_pct,${fin.nivel}\ntemperatura_fin_C,${f1(fin.temperaturaC)}\n"
        )
        Log.i(TAG, "### BENCHMARK COMPLETADO ### archivos en: ${dir.absolutePath}")
    }
}
