package com.example.finanzas_independientes_app.domain.model

/** A past monthly goal and whether it was met. */
data class MetaHistorialItem(
    /** Period in `yyyy-MM`. */
    val periodo: String,
    val metaMensual: Double,
    val utilidadReal: Double,
    val cumplida: Boolean
)
