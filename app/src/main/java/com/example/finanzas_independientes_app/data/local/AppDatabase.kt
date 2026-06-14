package com.example.finanzas_independientes_app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.finanzas_independientes_app.data.local.dao.CategoriaDao
import com.example.finanzas_independientes_app.data.local.dao.MetaDao
import com.example.finanzas_independientes_app.data.local.dao.TransaccionDao
import com.example.finanzas_independientes_app.data.local.entity.CategoriaEntity
import com.example.finanzas_independientes_app.data.local.entity.MetaEntity
import com.example.finanzas_independientes_app.data.local.entity.TransaccionEntity

/** Local cache for offline-first reads of history, categories and the active goal. */
@Database(
    entities = [TransaccionEntity::class, CategoriaEntity::class, MetaEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transaccionDao(): TransaccionDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun metaDao(): MetaDao

    companion object {
        const val NAME = "finanzas.db"
    }
}
