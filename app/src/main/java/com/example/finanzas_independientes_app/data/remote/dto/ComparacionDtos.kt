package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** An inclusive date window echoed back by the comparison endpoint. */
data class PeriodoDTO(
    @SerializedName("desde") val desde: String,
    @SerializedName("hasta") val hasta: String
)

/** Per-category actual-vs-previous spend. `deltaPct` is null when the base is 0. */
data class ComparacionCategoriaItemDTO(
    @SerializedName("categoria") val categoria: String,
    @SerializedName("actual") val actual: Double,
    @SerializedName("anterior") val anterior: Double,
    @SerializedName("deltaAbs") val deltaAbs: Double,
    @SerializedName("deltaPct") val deltaPct: Double?
)

/** GET /finanzas/analiticas/comparacion-categorias. */
data class ComparacionCategoriasDTO(
    @SerializedName("periodoActual") val periodoActual: PeriodoDTO,
    @SerializedName("periodoAnterior") val periodoAnterior: PeriodoDTO,
    @SerializedName("categorias") val categorias: List<ComparacionCategoriaItemDTO>,
    @SerializedName("totalActual") val totalActual: Double,
    @SerializedName("totalAnterior") val totalAnterior: Double,
    @SerializedName("totalDeltaPct") val totalDeltaPct: Double?
)
