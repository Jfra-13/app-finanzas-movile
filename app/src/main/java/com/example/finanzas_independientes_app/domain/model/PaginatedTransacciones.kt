package com.example.finanzas_independientes_app.domain.model

/** Spring Page result for paginated transaction lists. */
data class PaginatedTransacciones(
    val content: List<Transaccion>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val first: Boolean,
    val last: Boolean
)
