package com.example.finanzas_independientes_app.domain.model

/** Full authentication session returned after login or refresh. */
data class AuthSession(
    val token: String,
    val refreshToken: String,
    val usuario: Usuario,
    /** True only when this login reactivated an account that was pending deletion. */
    val cuentaReactivada: Boolean = false
)
