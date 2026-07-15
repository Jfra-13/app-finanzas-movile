package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * One past goal period from GET /finanzas/metas/historial (code GOALS_HISTORY_OK).
 * Shape per docs/backend-profile.md §4 (client-requested spec; confirm against
 * Swagger once the backend ships the endpoint).
 */
data class MetaHistorialItemDTO(
    @SerializedName("periodo") val periodo: String,
    @SerializedName("metaMensual") val metaMensual: Double,
    @SerializedName("utilidadReal") val utilidadReal: Double,
    @SerializedName("cumplida") val cumplida: Boolean
)
