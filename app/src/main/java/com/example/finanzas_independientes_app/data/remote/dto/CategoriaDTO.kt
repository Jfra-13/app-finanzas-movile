package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Category item from GET /categorias or POST /categorias response. */
data class CategoriaDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo") val tipo: String
)

/** Request body for POST /categorias. */
data class CategoriaRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo") val tipo: String
)
