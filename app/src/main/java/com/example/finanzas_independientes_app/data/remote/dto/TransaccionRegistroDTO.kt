package com.example.finanzas_independientes_app.data.remote.dto

// Body for POST /api/v1/finanzas/transacciones. usuarioId is derived from the
// JWT by the server, so it is never sent in the body.
data class TransaccionRegistroDTO(
    val monto: Double,
    val tipo: String,
    val descripcion: String? = null,
    val fecha: String? = null,
    val categoriaId: Long? = null
)