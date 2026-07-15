package com.example.finanzas_independientes_app.core.session

import com.example.finanzas_independientes_app.data.remote.dto.AuthData

/**
 * Single source of truth for the authenticated session. Tokens and profile
 * live encrypted via [SecureStorage]. Reads are synchronous so interceptors
 * and the token authenticator can use them off the main thread.
 */
class SessionManager(private val storage: SecureStorage) {

    val accessToken: String? get() = storage.getString(KEY_TOKEN)
    val refreshToken: String? get() = storage.getString(KEY_REFRESH)
    val usuarioId: Long? get() = storage.getLong(KEY_USER_ID)
    val nombre: String? get() = storage.getString(KEY_NOMBRE)
    val email: String? get() = storage.getString(KEY_EMAIL)
    val tipoNegocio: String? get() = storage.getString(KEY_TIPO_NEGOCIO)

    fun isLoggedIn(): Boolean = accessToken != null

    /** Persists the full session after login or refresh (token pair + profile). */
    fun saveSession(auth: AuthData) {
        storage.putString(KEY_TOKEN, auth.token)
        storage.putString(KEY_REFRESH, auth.refreshToken)
        storage.putLong(KEY_USER_ID, auth.usuarioId)
        storage.putString(KEY_NOMBRE, auth.nombre)
        storage.putString(KEY_EMAIL, auth.email)
        auth.tipoNegocio?.let { storage.putString(KEY_TIPO_NEGOCIO, it) }
            ?: storage.remove(KEY_TIPO_NEGOCIO)
    }

    /** Rotates only the token pair (used by the refresh flow). */
    fun updateTokens(token: String, refreshToken: String) {
        storage.putString(KEY_TOKEN, token)
        storage.putString(KEY_REFRESH, refreshToken)
    }

    fun updateTipoNegocio(tipoNegocio: String) {
        storage.putString(KEY_TIPO_NEGOCIO, tipoNegocio)
    }

    fun updateNombre(nombre: String) {
        storage.putString(KEY_NOMBRE, nombre)
    }

    fun clear() = storage.clear()

    private companion object {
        const val KEY_TOKEN = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER_ID = "usuario_id"
        const val KEY_NOMBRE = "nombre"
        const val KEY_EMAIL = "email"
        const val KEY_TIPO_NEGOCIO = "tipo_negocio"
    }
}
