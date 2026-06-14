package com.example.finanzas_independientes_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.finanzas_independientes_app.domain.model.Meta

/** Cached active goal for offline reads. */
@Entity(tableName = "metas")
data class MetaEntity(
    @PrimaryKey val id: Long,
    val montoObjetivo: Double,
    val periodo: String,
    val diasLaborables: List<Int>,
    val activa: Boolean
)

fun MetaEntity.toDomain(): Meta = Meta(
    id = id,
    montoObjetivo = montoObjetivo,
    periodo = periodo,
    diasLaborables = diasLaborables,
    activa = activa
)

fun Meta.toEntity(): MetaEntity = MetaEntity(
    id = id,
    montoObjetivo = montoObjetivo,
    periodo = periodo,
    diasLaborables = diasLaborables,
    activa = activa
)
