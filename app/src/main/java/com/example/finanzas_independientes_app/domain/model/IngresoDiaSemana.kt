package com.example.finanzas_independientes_app.domain.model

/** Aggregated income for one weekday (`dia` is uppercase, accent-free: LUNES..DOMINGO). */
data class IngresoDiaSemana(
    val dia: String,
    val ingresos: Double
)
