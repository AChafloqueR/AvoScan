package com.example.avoscan.data.local

object HistorialManager {

    val historial = mutableListOf<String>()

    fun agregarResultado(
        enfermedad: String,
        confianza: Float
    ) {

        historial.add(
            "$enfermedad - ${(confianza * 100).toInt()}%"
        )
    }
}