package com.example.finanzas_independientes_app.domain.model

/** Monthly goal set by the user. */
data class Meta(
    val id: Long,
    val montoObjetivo: Double,
    val periodo: String,
    val diasLaborables: List<Int>,
    val activa: Boolean
)
