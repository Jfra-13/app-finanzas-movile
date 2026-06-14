package com.example.finanzas_independientes_app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Single transaction item returned by GET /transacciones. */
data class TransaccionDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("monto") val monto: Double,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("categoriaId") val categoriaId: Long?,
    @SerializedName("categoriaNombre") val categoriaNombre: String?,
    @SerializedName("usuarioId") val usuarioId: Long
)

/** Spring Page wrapper for paginated transaction lists. */
data class PaginatedTransaccionDTO(
    @SerializedName("content") val content: List<TransaccionDTO>,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("number") val number: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("first") val first: Boolean,
    @SerializedName("last") val last: Boolean
)
