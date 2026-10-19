package com.example.finanzas_independientes_app.data.local

import okhttp3.Cache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Removes every piece of the signed-in user's data cached on disk: the Room
 * offline cache and the OkHttp response cache. Run it whenever the session
 * ends, so the next account on the same device never sees the previous one.
 *
 * Blocking I/O: call it off the main thread (Room throws otherwise).
 */
@Singleton
class UserDataWiper @Inject constructor(
    private val db: AppDatabase,
    private val httpCache: Cache
) {
    // ponytail: wipe-on-logout instead of scoping every DAO query by usuarioId;
    // the cache only ever holds one user. Scope queries if multi-account lands.
    fun wipe() {
        db.clearAllTables()
        httpCache.evictAll()
    }
}
