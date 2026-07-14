package com.example.finanzas_independientes_app.domain.model

/** A category budget with its current-month consumption. */
data class Presupuesto(
    val id: Long,
    val categoriaId: Long,
    val categoriaNombre: String,
    val montoMensual: Double,
    val gastadoMes: Double,
    val restante: Double,
    val consumoPct: Double,
    val excedido: Boolean
)
