package com.example.finanzas_independientes_app.domain.usecase

import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem

/**
 * Orders financial-health signals by severity so the UI shows the most urgent
 * first: ALTA, then MEDIA, then BAJA. Unknown severities sort last. The sort is
 * stable, so signals sharing a severity keep the server's order.
 */
class OrdenarSenalesSaludUseCase {

    operator fun invoke(senales: List<SaludFinancieraItem>): List<SaludFinancieraItem> =
        senales.sortedBy { rango(it.severidad) }

    private fun rango(severidad: String): Int = when (severidad.uppercase()) {
        "ALTA" -> 0
        "MEDIA" -> 1
        "BAJA" -> 2
        else -> 3
    }
}
