package com.example.finanzas_independientes_app.data.remote.dto

/**
 * Profile payload from GET/PUT /usuarios/me. `fotoUrl` and `plan` are part of
 * the contract but arrive null until the backend supports them.
 */
data class PerfilDTO(
    val id: Long,
    val nombre: String,
    val email: String,
    val telefono: String?,
    val fotoUrl: String?,
    val tipoNegocio: String?,
    val plan: String?
)

/** Partial update for PUT /usuarios/me: only non-null fields change. Email is not editable. */
data class UpdatePerfilRequest(
    val nombre: String? = null,
    val telefono: String? = null
)
