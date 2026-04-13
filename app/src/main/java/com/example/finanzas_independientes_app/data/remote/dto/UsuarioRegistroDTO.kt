package com.example.finanzas_independientes_app.data.remote.dto

// Este es el molde exacto de lo que Spring Boot recibirá
data class UsuarioRegistroDTO(
    val nombre: String,
    val email: String,
    val password: String
)