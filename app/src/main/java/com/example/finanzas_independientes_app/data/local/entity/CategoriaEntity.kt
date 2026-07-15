package com.example.finanzas_independientes_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.finanzas_independientes_app.domain.model.Categoria

/** Cached category row for offline reads. */
@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey val id: Long,
    val nombre: String,
    val tipo: String
)

fun CategoriaEntity.toDomain(): Categoria = Categoria(id = id, nombre = nombre, tipo = tipo)

fun Categoria.toEntity(): CategoriaEntity = CategoriaEntity(id = id, nombre = nombre, tipo = tipo)
