package com.example.finanzas_independientes_app.core.session

/**
 * Synchronous secure key-value store. Synchronous on purpose: the OkHttp
 * [okhttp3.Authenticator] runs off the main thread and must read/write tokens
 * without suspending. Hides the concrete crypto backend so it can be swapped
 * (Keystore-direct, Tink) without touching callers.
 */
interface SecureStorage {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun putLong(key: String, value: Long)
    fun getLong(key: String): Long?
    fun remove(key: String)
    fun clear()
}
