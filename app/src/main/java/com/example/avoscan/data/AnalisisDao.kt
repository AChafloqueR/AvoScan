package com.example.avoscan.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalisisDao {

    @Query("SELECT * FROM analisis ORDER BY fecha DESC")
    fun obtenerTodos(): Flow<List<AnalisisEntity>>

    @Query("SELECT * FROM analisis ORDER BY fecha DESC LIMIT :limite")
    fun obtenerRecientes(limite: Int = 5): Flow<List<AnalisisEntity>>

    @Query("SELECT COUNT(*) FROM analisis")
    fun contarTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM analisis WHERE estadoColor IN ('ROJO', 'NARANJA')")
    fun contarAlertas(): Flow<Int>

    @Query("SELECT COUNT(*) FROM analisis WHERE estadoColor IN ('ROJO', 'NARANJA') AND fecha >= :desde")
    fun contarPlagasActivas(desde: Long): Flow<Int>

    @Insert
    suspend fun insertar(analisis: AnalisisEntity): Long

    @Delete
    suspend fun eliminar(analisis: AnalisisEntity)

    @Query("DELETE FROM analisis")
    suspend fun eliminarTodos()
}