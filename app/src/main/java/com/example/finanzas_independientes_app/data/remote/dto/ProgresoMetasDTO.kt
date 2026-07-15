package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Consolidated goal-progress payload from GET /progreso-metas. */
data class ProgresoMetasDTO(
    @SerializedName("ingresoDiario") val ingresoDiario: Double,
    @SerializedName("metaDiaria") val metaDiaria: Double,
    @SerializedName("ingresoSemanal") val ingresoSemanal: Double,
    @SerializedName("metaSemanal") val metaSemanal: Double,
    @SerializedName("ingresoMensual") val ingresoMensual: Double,
    @SerializedName("metaMensual") val metaMensual: Double
)
