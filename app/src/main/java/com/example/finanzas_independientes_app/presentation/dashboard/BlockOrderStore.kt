package com.example.finanzas_independientes_app.presentation.dashboard

import android.content.Context

/**
 * Persists the user's preferred order of the reorderable dashboard blocks. Only
 * these three blocks move (Metas stays pinned). Corrupt/legacy values fall back
 * to [DEFAULT] rather than dropping or duplicating a block.
 */
object BlockOrderStore {

    const val DIARIO = "diario"
    const val SEMANAL = "semanal"
    const val ACUMULADO = "acumulado"

    val DEFAULT = listOf(DIARIO, SEMANAL, ACUMULADO)

    private const val PREFS = "dashboard_prefs"
    private const val KEY = "block_order"

    fun load(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return DEFAULT
        val parts = raw.split(",")
        // Only trust it if it's exactly the known set, no more, no less.
        return if (parts.size == DEFAULT.size && parts.toSet() == DEFAULT.toSet()) parts else DEFAULT
    }

    fun save(context: Context, order: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, order.joinToString(","))
            .apply()
    }

    fun displayName(key: String): String = when (key) {
        DIARIO -> "Progreso diario"
        SEMANAL -> "Progreso semanal"
        ACUMULADO -> "Dinero acumulado"
        else -> key
    }
}
