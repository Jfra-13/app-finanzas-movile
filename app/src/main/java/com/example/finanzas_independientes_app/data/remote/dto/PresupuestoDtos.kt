package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Body for POST /finanzas/presupuestos. Upsert: same categoriaId replaces the cap. */
data class SetPresupuestoRequest(
    @SerializedName("categoriaId") val categoriaId: Long,
    @SerializedName("montoMensual") val montoMensual: Double
)

/** A budget with its current-month consumption, from GET /finanzas/presupuestos. */
data class PresupuestoDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("categoriaId") val categoriaId: Long,
    @SerializedName("categoriaNombre") val categoriaNombre: String,
    @SerializedName("montoMensual") val montoMensual: Double,
    @SerializedName("gastadoMes") val gastadoMes: Double,
    @SerializedName("restante") val restante: Double,
    @SerializedName("consumoPct") val consumoPct: Double,
    @SerializedName("excedido") val excedido: Boolean
)
