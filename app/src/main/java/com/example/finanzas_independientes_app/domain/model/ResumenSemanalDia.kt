package com.example.finanzas_independientes_app.domain.model

/** One day's income/expense totals in the weekly summary. */
data class ResumenSemanalDia(
    val dia: String,
    val ingresos: Double,
    val egresos: Double
)
