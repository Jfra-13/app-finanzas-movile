package com.example.finanzas_independientes_app.data.remote.dto

/** Login / refresh success payload (envelope `data`). */
data class AuthData(
    val token: String,
    val refreshToken: String,
    val usuarioId: Long,
    val nombre: String,
    val email: String,
    val tipoNegocio: String?,
    // Additive: true only when this login reactivated an account pending deletion
    // (false on normal login and on refresh). Defaults to false when the server omits it.
    val cuentaReactivada: Boolean = false
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

/** Body for POST /usuarios/me/eliminar — password re-confirms identity. */
data class DeleteAccountRequest(val password: String)
