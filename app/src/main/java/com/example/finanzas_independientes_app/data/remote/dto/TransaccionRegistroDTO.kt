package com.example.finanzas_independientes_app.data.remote.dto

data class TransaccionRegistroDTO(
    val monto: Double,
    val tipo: String,
    val usuarioId: Long
)