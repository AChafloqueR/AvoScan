package com.example.avoscan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analisis")
data class AnalisisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long,
    val estadoTitulo: String,
    val estadoColor: String,
    val etiqueta: String,
    val confianza: Float,
    val mensaje: String,
    val imagePath: String?
)