package com.example.finanzas_independientes_app.domain.repository

import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.ComparacionCategorias
import com.example.finanzas_independientes_app.domain.model.CompararCon
import com.example.finanzas_independientes_app.domain.model.IngresoDiaSemana
import com.example.finanzas_independientes_app.domain.model.Presupuesto
import com.example.finanzas_independientes_app.domain.model.ProyeccionMensual
import com.example.finanzas_independientes_app.domain.model.Meta
import com.example.finanzas_independientes_app.domain.model.PaginatedTransacciones
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ResumenDiarioDia
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.Tendencia

/**
 * Contract for all finanzas-domain network operations.
 * Identity is provided by the JWT; no user-id parameters are needed here.
 */
interface FinanzasRepository {

    // --- Transactions ---

    suspend fun registrarTransaccion(
        monto: Double,
        tipo: String,
        descripcion: String? = null,
        fecha: String? = null,
        categoriaId: Long? = null
    ): ApiResult<Unit>

    suspend fun listarTransacciones(
        tipo: String? = null,
        categoriaId: Long? = null,
        desde: String? = null,
        hasta: String? = null,
        page: Int = 0,
        size: Int = 20,
        sort: String? = null
    ): ApiResult<PaginatedTransacciones>

    suspend fun actualizarTransaccion(
        id: Long,
        monto: Double,
        tipo: String,
        descripcion: String? = null,
        fecha: String? = null,
        categoriaId: Long? = null
    ): ApiResult<Unit>

    suspend fun eliminarTransaccion(id: Long): ApiResult<Unit>

    // --- Daily income & quota ---

    /** Returns the total income for today. */
    suspend fun obtenerHoy(): ApiResult<Double>

    /** Returns the daily quota from the persisted goal (no params). */
    suspend fun obtenerCuotaDiaria(): ApiResult<Double>

    // --- Analytics & summaries ---

    suspend fun obtenerResumenSemanal(): ApiResult<List<ResumenSemanalDia>>

    /** Per-day totals for a month (YYYY-MM; null = current month). Only days with activity. */
    suspend fun obtenerResumenDiario(mes: String? = null): ApiResult<List<ResumenDiarioDia>>

    suspend fun obtenerProgresoMetas(): ApiResult<ProgresoMetas>

    // --- Goals ---

    suspend fun fijarMeta(montoObjetivo: Double, diasLaborables: List<Int>): ApiResult<Meta>

    suspend fun obtenerMetaActual(): ApiResult<Meta>

    // --- Categories ---

    suspend fun listarCategorias(): ApiResult<List<Categoria>>

    suspend fun crearCategoria(nombre: String, tipo: String): ApiResult<Categoria>

    /** Renames a category. `tipo` is immutable server-side. */
    suspend fun actualizarCategoria(id: Long, nombre: String): ApiResult<Unit>

    /** Deletes a category; its transactions become uncategorized on the server. */
    suspend fun eliminarCategoria(id: Long): ApiResult<Unit>

    // --- Analytics (read-only) ---

    /**
     * Expense totals grouped by category name, over an optional inclusive
     * YYYY-MM-DD range; without params it covers the current month.
     */
    suspend fun obtenerResumenCategorias(
        desde: String? = null,
        hasta: String? = null
    ): ApiResult<Map<String, Double>>

    /** Trend bucketed by `granularidad` (SEMANA|MES) over the last `ventana` periods. */
    suspend fun obtenerTendencia(granularidad: String, ventana: Int): ApiResult<Tendencia>

    /** Income per weekday over the last `ventana` weeks (null = server default of 4). */
    suspend fun obtenerIngresosPorDiaSemana(ventana: Int? = null): ApiResult<List<IngresoDiaSemana>>

    suspend fun obtenerSaludFinanciera(): ApiResult<List<SaludFinancieraItem>>

    // --- Budgets, comparison & projection ---

    /** All category budgets with current-month consumption. */
    suspend fun obtenerPresupuestos(): ApiResult<List<Presupuesto>>

    /** Upsert the monthly cap for a category (one budget per category). */
    suspend fun guardarPresupuesto(categoriaId: Long, montoMensual: Double): ApiResult<Presupuesto>

    suspend fun eliminarPresupuesto(id: Long): ApiResult<Unit>

    /** Spend per category vs a comparison window (null dates = current month to date). */
    suspend fun obtenerComparacionCategorias(
        desde: String? = null,
        hasta: String? = null,
        compararCon: CompararCon = CompararCon.PERIODO_ANTERIOR
    ): ApiResult<ComparacionCategorias>

    /** Linear run-rate projection for the current month. */
    suspend fun obtenerProyeccionMensual(): ApiResult<ProyeccionMensual>
}
