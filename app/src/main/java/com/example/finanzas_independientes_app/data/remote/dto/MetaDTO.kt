package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Active goal returned by POST /metas and GET /metas/actual. */
data class MetaDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("montoObjetivo") val montoObjetivo: Double,
    @SerializedName("periodo") val periodo: String,
    @SerializedName("diasLaborables") val diasLaborables: List<Int>,
    @SerializedName("activa") val activa: Boolean
)

/** Request body for POST /metas. */
data class MetaRequest(
    @SerializedName("montoObjetivo") val montoObjetivo: Double,
    @SerializedName("diasLaborables") val diasLaborables: List<Int>
)
