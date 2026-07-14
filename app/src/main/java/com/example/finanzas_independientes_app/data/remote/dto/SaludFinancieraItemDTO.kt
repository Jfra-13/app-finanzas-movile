package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** One financial health signal from GET /salud-financiera. */
data class SaludFinancieraItemDTO(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("code") val code: String,
    @SerializedName("severidad") val severidad: String,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("categoriaId") val categoriaId: Long?
)
