package com.example.finanzas_independientes_app.domain.model

/** A single financial health signal (alert or congratulation). */
data class SaludFinancieraItem(
    val tipo: String,
    val code: String,
    val mensaje: String
)
