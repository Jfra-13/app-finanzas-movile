package com.example.finanzas_independientes_app.domain.model

/** A single financial health signal (alert or congratulation). */
data class SaludFinancieraItem(
    val tipo: String,
    val code: String,
    val severidad: String,
    val mensaje: String,
    /** Present only on PRESUPUESTO_EXCEDIDO, for deep-linking to the category. */
    val categoriaId: Long?
)
