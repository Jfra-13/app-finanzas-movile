package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** One day's totals in the weekly summary response. */
data class ResumenSemanalItemDTO(
    @SerializedName("dia") val dia: String,
    @SerializedName("ingresos") val ingresos: Double,
    @SerializedName("egresos") val egresos: Double
)
