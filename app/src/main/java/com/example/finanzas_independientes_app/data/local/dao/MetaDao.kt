package com.example.finanzas_independientes_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.finanzas_independientes_app.data.local.entity.MetaEntity

@Dao
interface MetaDao {

    @Query("SELECT * FROM metas WHERE activa = 1 LIMIT 1")
    suspend fun getActiva(): MetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MetaEntity)

    @Query("DELETE FROM metas")
    suspend fun clear()

    /** Keep only the latest active goal cached. */
    @Transaction
    suspend fun replaceActiva(item: MetaEntity) {
        clear()
        upsert(item)
    }
}
