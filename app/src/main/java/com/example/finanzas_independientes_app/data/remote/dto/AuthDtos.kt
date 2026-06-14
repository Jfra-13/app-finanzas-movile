package com.example.finanzas_independientes_app.data.remote.dto

/** Login / refresh success payload (envelope `data`). */
data class AuthData(
    val token: String,
    val refreshToken: String,
    val usuarioId: Long,
    val nombre: String,
    val email: String,
    val tipoNegocio: String?
)

data class RefreshRequest(val refreshToken: String)

data class ForgotPasswordRequest(val email: String)

data class VerifyOtpRequest(val email: String, val otp: String)

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)

data class UpdateNegocioRequest(val tipoNegocio: String)
