package com.example.finanzas_independientes_app.domain.model

/** Consolidated daily/weekly/monthly goal progress. */
data class ProgresoMetas(
    val ingresoDiario: Double,
    val metaDiaria: Double,
    val ingresoSemanal: Double,
    val metaSemanal: Double,
    val ingresoMensual: Double,
    val metaMensual: Double
)
