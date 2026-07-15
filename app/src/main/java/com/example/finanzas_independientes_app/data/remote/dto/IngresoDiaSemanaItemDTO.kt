package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * One weekday's aggregated income from GET /ingresos-por-dia-semana.
 * Always 7 items Monday-first; `dia` is uppercase without accents (LUNES, MIERCOLES).
 */
data class IngresoDiaSemanaItemDTO(
    @SerializedName("dia") val dia: String,
    @SerializedName("ingresos") val ingresos: Double
)
