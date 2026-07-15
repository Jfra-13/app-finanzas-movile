package com.example.finanzas_independientes_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.finanzas_independientes_app.data.local.entity.CategoriaEntity

@Dao
interface CategoriaDao {

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    suspend fun getAll(): List<CategoriaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CategoriaEntity>)

    @Query("DELETE FROM categorias")
    suspend fun clear()

    /** Replace the whole cached set atomically. */
    @Transaction
    suspend fun replaceAll(items: List<CategoriaEntity>) {
        clear()
        upsertAll(items)
    }
}
