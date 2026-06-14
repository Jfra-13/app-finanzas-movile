package com.example.finanzas_independientes_app.domain.model

/** Authenticated user profile derived from the JWT session. */
data class Usuario(
    val usuarioId: Long,
    val nombre: String,
    val email: String,
    val tipoNegocio: String?
)
