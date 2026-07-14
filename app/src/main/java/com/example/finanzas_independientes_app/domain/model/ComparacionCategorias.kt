package com.example.finanzas_independientes_app.domain.model

/** Basis for the comparison window. Sent verbatim as the `compararCon` query param. */
enum class CompararCon { PERIODO_ANTERIOR, MISMO_PERIODO_ANIO_ANTERIOR }

/** An inclusive date window. */
data class Periodo(val desde: String, val hasta: String)

/** Per-category actual-vs-previous spend. `deltaPct` null = previous base was 0 (show "—"). */
data class ComparacionCategoriaItem(
    val categoria: String,
    val actual: Double,
    val anterior: Double,
    val deltaAbs: Double,
    val deltaPct: Double?
)

data class ComparacionCategorias(
    val periodoActual: Periodo,
    val periodoAnterior: Periodo,
    val categorias: List<ComparacionCategoriaItem>,
    val totalActual: Double,
    val totalAnterior: Double,
    val totalDeltaPct: Double?
)
