package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Monthly income/expense trend from GET /tendencia-mensual. */
data class TendenciaMensualDTO(
    @SerializedName("meses") val meses: List<String>,
    @SerializedName("ingresos") val ingresos: List<Double>,
    @SerializedName("egresos") val egresos: List<Double>
)
