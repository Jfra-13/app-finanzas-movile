package com.example.finanzas_independientes_app.domain.model

/** Full user profile served by the backend (unlike [Usuario], which is JWT-session data). */
data class Perfil(
    val id: Long,
    val nombre: String,
    val email: String,
    val telefono: String?,
    val fotoUrl: String?,
    val tipoNegocio: String?,
    val plan: String?
)
