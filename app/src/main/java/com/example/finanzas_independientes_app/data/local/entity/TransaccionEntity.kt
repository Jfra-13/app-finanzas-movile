package com.example.finanzas_independientes_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.finanzas_independientes_app.domain.model.Transaccion

/** Cached transaction row for offline reads of the history. */
@Entity(tableName = "transacciones")
data class TransaccionEntity(
    @PrimaryKey val id: Long,
    val monto: Double,
    val tipo: String,
    val descripcion: String?,
    val fecha: String,
    val categoriaId: Long?,
    val categoriaNombre: String?,
    val usuarioId: Long
)

fun TransaccionEntity.toDomain(): Transaccion = Transaccion(
    id = id,
    monto = monto,
    tipo = tipo,
    descripcion = descripcion,
    fecha = fecha,
    categoriaId = categoriaId,
    categoriaNombre = categoriaNombre,
    usuarioId = usuarioId
)

fun Transaccion.toEntity(): TransaccionEntity = TransaccionEntity(
    id = id,
    monto = monto,
    tipo = tipo,
    descripcion = descripcion,
    fecha = fecha,
    categoriaId = categoriaId,
    categoriaNombre = categoriaNombre,
    usuarioId = usuarioId
)
