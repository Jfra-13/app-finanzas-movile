package com.example.finanzas_independientes_app.data.remote.dto

// Body for POST /api/v1/usuarios/registro. tipoNegocio is optional and persisted.
data class UsuarioRegistroDTO(
    val nombre: String,
    val email: String,
    val password: String,
    val tipoNegocio: String? = null
)