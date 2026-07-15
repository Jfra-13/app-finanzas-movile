package com.example.finanzas_independientes_app.domain.model

/** Income or expense transaction belonging to the authenticated user. */
data class Transaccion(
    val id: Long,
    val monto: Double,
    val tipo: String,
    val descripcion: String?,
    val fecha: String,
    val categoriaId: Long?,
    val categoriaNombre: String?,
    val usuarioId: Long
)
