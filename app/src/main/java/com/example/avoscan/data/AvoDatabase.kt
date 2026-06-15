package com.example.avoscan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AnalisisEntity::class], version = 1)
abstract class AvoDatabase : RoomDatabase() {

    abstract fun analisisDao(): AnalisisDao

    companion object {
        @Volatile private var INSTANCE: AvoDatabase? = null

        fun getInstance(context: Context): AvoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AvoDatabase::class.java,
                    "avo_db"
                ).build().also { INSTANCE = it }
            }
    }
}