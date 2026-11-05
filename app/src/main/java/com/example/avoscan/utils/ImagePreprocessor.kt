package com.example.avoscan.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

object ImagePreprocessor {

    private const val WORK_SIZE = 320         // resolución de trabajo (rápido)
    private const val GRAY_BG = 0xFF808080.toInt()
    private const val MIN_FRUIT_RATIO = 0.15f
    private const val MAX_FRUIT_RATIO = 0.70f
    private const val PADDING_PX = 12

    fun preprocesarFruto(input: Bitmap): Bitmap {

        // 1) Trabajar a tamaño reducido para velocidad
        val work = Bitmap.createScaledBitmap(input, WORK_SIZE, WORK_SIZE, true)
        val pixels = IntArray(WORK_SIZE * WORK_SIZE)
        work.getPixels(pixels, 0, WORK_SIZE, 0, 0, WORK_SIZE, WORK_SIZE)

        // 2) Construir máscara de foreground por umbralización HSV
        val mask = BooleanArray(pixels.size)
        for (i in pixels.indices) mask[i] = esForeground(pixels[i])

        // 3) Encontrar la componente conectada más grande (flood fill)
        val labels = IntArray(pixels.size)
        var bestLabel = 0
        var bestSize = 0
        var bestBounds = intArrayOf(0, 0, WORK_SIZE - 1, WORK_SIZE - 1)
        var nextLabel = 0

        for (y in 0 until WORK_SIZE) {
            for (x in 0 until WORK_SIZE) {
                val idx = y * WORK_SIZE + x
                if (mask[idx] && labels[idx] == 0) {
                    nextLabel++
                    val (size, bounds) = floodFill(x, y, mask, labels, nextLabel, WORK_SIZE)
                    if (size > bestSize) {
                        bestSize = size
                        bestLabel = nextLabel
                        bestBounds = bounds
                    }
                }
            }
        }

        // 4) Validar: si el fruto es muy chico o cubre toda la imagen, omitir
        val ratio = bestSize.toFloat() / pixels.size
        if (ratio < MIN_FRUIT_RATIO || ratio > MAX_FRUIT_RATIO) {
            return input
        }

        // 5) Recortar con padding
        val x0 = max(0, bestBounds[0] - PADDING_PX)
        val y0 = max(0, bestBounds[1] - PADDING_PX)
        val x1 = min(WORK_SIZE - 1, bestBounds[2] + PADDING_PX)
        val y1 = min(WORK_SIZE - 1, bestBounds[3] + PADDING_PX)
        val w  = x1 - x0 + 1
        val h  = y1 - y0 + 1

        // 6) Construir bitmap de salida: pixeles del fruto + fondo gris
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val origIdx = (y0 + y) * WORK_SIZE + (x0 + x)
                out[y * w + x] = if (labels[origIdx] == bestLabel) pixels[origIdx] else GRAY_BG
            }
        }

        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun esForeground(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)

        val maxC  = max(r, max(g, b))
        val minC  = min(r, min(g, b))
        val delta = maxC - minC

        val saturation = if (maxC == 0) 0f else delta.toFloat() / maxC
        val value      = maxC / 255f

        val esBlancoIsh = saturation < 0.12f && value > 0.82f
        val esNegroIsh  = value < 0.10f
        return !esBlancoIsh && !esNegroIsh
    }

    private fun floodFill(
        startX: Int, startY: Int,
        mask: BooleanArray, labels: IntArray, label: Int,
        size: Int
    ): Pair<Int, IntArray> {
        val stack = ArrayDeque<Int>()
        // Empaquetar (x,y) en un int: high 16 = x, low 16 = y
        stack.addLast((startX shl 16) or (startY and 0xFFFF))

        var count = 0
        var minX = startX; var maxX = startX
        var minY = startY; var maxY = startY

        while (stack.isNotEmpty()) {
            val packed = stack.removeLast()
            val x = packed shr 16
            val y = packed and 0xFFFF

            if (x < 0 || x >= size || y < 0 || y >= size) continue
            val idx = y * size + x
            if (!mask[idx] || labels[idx] != 0) continue

            labels[idx] = label
            count++
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y

            stack.addLast(((x + 1) shl 16) or (y and 0xFFFF))
            stack.addLast(((x - 1) shl 16) or (y and 0xFFFF))
            stack.addLast((x shl 16) or ((y + 1) and 0xFFFF))
            stack.addLast((x shl 16) or ((y - 1) and 0xFFFF))
        }

        return Pair(count, intArrayOf(minX, minY, maxX, maxY))
    }
}