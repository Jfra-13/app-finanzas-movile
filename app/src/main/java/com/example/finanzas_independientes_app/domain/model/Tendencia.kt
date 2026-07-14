package com.example.finanzas_independientes_app.domain.model

/**
 * Income/expense trend over a rolling window. Parallel arrays, oldest first.
 * `periodos` labels are `yyyy-MM` (monthly) or the week's Monday `yyyy-MM-dd` (weekly).
 */
data class Tendencia(
    val periodos: List<String>,
    val ingresos: List<Double>,
    val egresos: List<Double>
)
