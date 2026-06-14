package com.example.finanzas_independientes_app.domain.model

/** Full authentication session returned after login or refresh. */
data class AuthSession(
    val token: String,
    val refreshToken: String,
    val usuario: Usuario
)
