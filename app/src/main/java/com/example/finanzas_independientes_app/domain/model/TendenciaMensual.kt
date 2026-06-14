package com.example.finanzas_independientes_app.domain.model

/** Monthly income/expense trend over a rolling window. */
data class TendenciaMensual(
    val meses: List<String>,
    val ingresos: List<Double>,
    val egresos: List<Double>
)
