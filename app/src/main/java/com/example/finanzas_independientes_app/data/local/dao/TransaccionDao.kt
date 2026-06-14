package com.example.finanzas_independientes_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.finanzas_independientes_app.data.local.entity.TransaccionEntity

@Dao
interface TransaccionDao {

    @Query("SELECT * FROM transacciones ORDER BY fecha DESC, id DESC")
    suspend fun getAll(): List<TransaccionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TransaccionEntity>)

    @Query("DELETE FROM transacciones")
    suspend fun clear()

    /** Replace the whole cached history atomically (used when loading the first page). */
    @Transaction
    suspend fun replaceAll(items: List<TransaccionEntity>) {
        clear()
        upsertAll(items)
    }
}
