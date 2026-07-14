package com.example.finanzas_independientes_app.domain.repository

import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.AuthSession
import com.example.finanzas_independientes_app.domain.model.Perfil

/**
 * Contract for all authentication-related network operations.
 * Implementations live in the data layer and are never exposed to the UI.
 */
interface AuthRepository {

    /** Authenticate with email + password. Persists the session on success. */
    suspend fun login(email: String, password: String): ApiResult<AuthSession>

    /** Register a new account. Returns Unit on success (data: null from server). */
    suspend fun registro(
        nombre: String,
        email: String,
        password: String,
        tipoNegocio: String? = null
    ): ApiResult<Unit>

    /** Request an OTP email for password reset. */
    suspend fun forgotPassword(email: String): ApiResult<Unit>

    /** Validate the OTP without consuming it. */
    suspend fun verifyOtp(email: String, otp: String): ApiResult<Unit>

    /** Consume the OTP and set a new password. */
    suspend fun resetPassword(email: String, otp: String, newPassword: String): ApiResult<Unit>

    /** Update the authenticated user's business type and persist it in the session. */
    suspend fun actualizarNegocio(tipoNegocio: String): ApiResult<Unit>

    /** Fetch the authenticated user's full profile from the server. */
    suspend fun obtenerPerfil(): ApiResult<Perfil>

    /** Partial profile update: only non-null fields change. Email is not editable. */
    suspend fun actualizarPerfil(nombre: String? = null, telefono: String? = null): ApiResult<Perfil>

    /**
     * Ends the session: best-effort server-side refresh-token revocation, then
     * always clears the local session — a network failure must never keep the
     * user logged in on the device.
     */
    suspend fun logout()
}
