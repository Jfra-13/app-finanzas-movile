package com.example.finanzas_independientes_app.domain.model

/** Totals for a single day (`fecha` is YYYY-MM-DD). */
data class ResumenDiarioDia(
    val fecha: String,
    val ingresos: Double,
    val egresos: Double
)
