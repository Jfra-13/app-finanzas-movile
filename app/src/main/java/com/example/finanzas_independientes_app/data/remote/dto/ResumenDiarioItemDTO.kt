package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** One day's totals in the daily summary response. `fecha` is YYYY-MM-DD. */
data class ResumenDiarioItemDTO(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("ingresos") val ingresos: Double,
    @SerializedName("egresos") val egresos: Double
)
