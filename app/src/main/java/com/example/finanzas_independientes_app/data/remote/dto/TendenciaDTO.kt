package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Income/expense trend from GET /tendencia. Parallel arrays, oldest first.
 * `periodos` labels are `yyyy-MM` for MES and the week's Monday `yyyy-MM-dd` for SEMANA.
 */
data class TendenciaDTO(
    @SerializedName("periodos") val periodos: List<String>,
    @SerializedName("ingresos") val ingresos: List<Double>,
    @SerializedName("egresos") val egresos: List<Double>
)
