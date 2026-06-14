package com.example.finanzas_independientes_app.domain.repository

import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.Meta
import com.example.finanzas_independientes_app.domain.model.PaginatedTransacciones
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.TendenciaMensual

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

    suspend fun obtenerProgresoMetas(): ApiResult<ProgresoMetas>

    // --- Goals ---

    suspend fun fijarMeta(montoObjetivo: Double, diasLaborables: List<Int>): ApiResult<Meta>

    suspend fun obtenerMetaActual(): ApiResult<Meta>

    // --- Categories ---

    suspend fun listarCategorias(): ApiResult<List<Categoria>>

    suspend fun crearCategoria(nombre: String, tipo: String): ApiResult<Categoria>

    // --- Analytics (read-only) ---

    /** Returns monthly expense totals grouped by category name. */
    suspend fun obtenerResumenCategorias(): ApiResult<Map<String, Double>>

    suspend fun obtenerTendenciaMensual(meses: Int? = null): ApiResult<TendenciaMensual>

    suspend fun obtenerSaludFinanciera(): ApiResult<List<SaludFinancieraItem>>
}
