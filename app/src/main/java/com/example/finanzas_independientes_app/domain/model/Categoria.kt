package com.example.finanzas_independientes_app.domain.model

/** Transaction category (system-wide or user-created). */
data class Categoria(
    val id: Long,
    val nombre: String,
    val tipo: String
)
