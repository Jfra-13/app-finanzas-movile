package com.example.finanzas_independientes_app.fakes

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.ComparacionCategorias
import com.example.finanzas_independientes_app.domain.model.CompararCon
import com.example.finanzas_independientes_app.domain.model.IngresoDiaSemana
import com.example.finanzas_independientes_app.domain.model.Meta
import com.example.finanzas_independientes_app.domain.model.MetaHistorialItem
import com.example.finanzas_independientes_app.domain.model.PaginatedTransacciones
import com.example.finanzas_independientes_app.domain.model.Periodo
import com.example.finanzas_independientes_app.domain.model.Presupuesto
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ProyeccionMensual
import com.example.finanzas_independientes_app.domain.model.ResumenDiarioDia
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.Tendencia
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository

/**
 * Configurable [FinanzasRepository] for ViewModel tests. Set the `*Result` vars
 * to choose what each call returns (defaults are successes carrying the real
 * ApiCode and an empty payload); every call is recorded in a `*Calls` list
 * holding its arguments, or in a plain counter when the method takes none.
 * The interface has no Flow-returning methods, so there are no flows to expose.
 */
class FakeFinanzasRepository : FinanzasRepository {

    data class RegistrarTransaccionCall(
        val monto: Double,
        val tipo: String,
        val descripcion: String?,
        val fecha: String?,
        val categoriaId: Long?
    )

    data class ListarTransaccionesCall(
        val tipo: String?,
        val categoriaId: Long?,
        val sinCategoria: Boolean?,
        val desde: String?,
        val hasta: String?,
        val page: Int,
        val size: Int,
        val sort: String?
    )

    data class ActualizarTransaccionCall(
        val id: Long,
        val monto: Double,
        val tipo: String,
        val descripcion: String?,
        val fecha: String?,
        val categoriaId: Long?
    )

    data class FijarMetaCall(val montoObjetivo: Double, val diasLaborables: List<Int>)
    data class CrearCategoriaCall(val nombre: String, val tipo: String)
    data class ActualizarCategoriaCall(val id: Long, val nombre: String)
    data class RangoCall(val desde: String?, val hasta: String?)
    data class TendenciaCall(val granularidad: String, val ventana: Int)
    data class GuardarPresupuestoCall(val categoriaId: Long, val montoMensual: Double)
    data class ComparacionCall(val desde: String?, val hasta: String?, val compararCon: CompararCon)

    var registrarTransaccionResult: ApiResult<Unit> =
        ApiResult.Success(Unit, ApiCode.TRANSACTION_CREATED)
    var listarTransaccionesResult: ApiResult<PaginatedTransacciones> = ApiResult.Success(
        PaginatedTransacciones(emptyList(), 0, 0, 0, 20, first = true, last = true),
        ApiCode.TRANSACTIONS_OK
    )
    var actualizarTransaccionResult: ApiResult<Unit> =
        ApiResult.Success(Unit, ApiCode.TRANSACTION_UPDATED)
    var eliminarTransaccionResult: ApiResult<Unit> =
        ApiResult.Success(Unit, ApiCode.TRANSACTION_DELETED)
    var obtenerHoyResult: ApiResult<Double> = ApiResult.Success(0.0, ApiCode.TODAY_INCOME_OK)
    var obtenerCuotaDiariaResult: ApiResult<Double> =
        ApiResult.Success(0.0, ApiCode.DAILY_QUOTA_OK)
    var obtenerResumenSemanalResult: ApiResult<List<ResumenSemanalDia>> =
        ApiResult.Success(emptyList(), ApiCode.WEEKLY_SUMMARY_OK)
    var obtenerResumenDiarioResult: ApiResult<List<ResumenDiarioDia>> =
        ApiResult.Success(emptyList(), ApiCode.DAILY_SUMMARY_OK)
    var obtenerProgresoMetasResult: ApiResult<ProgresoMetas> = ApiResult.Success(
        ProgresoMetas(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        ApiCode.GOALS_PROGRESS_OK
    )
    var fijarMetaResult: ApiResult<Meta> = ApiResult.Success(
        Meta(1, 0.0, "2026-01", emptyList(), true),
        ApiCode.GOAL_SET
    )
    var obtenerMetaActualResult: ApiResult<Meta> = ApiResult.Success(
        Meta(1, 0.0, "2026-01", emptyList(), true),
        ApiCode.GOAL_OK
    )
    var obtenerHistorialMetasResult: ApiResult<List<MetaHistorialItem>> =
        ApiResult.Success(emptyList(), ApiCode.GOALS_HISTORY_OK)
    var listarCategoriasResult: ApiResult<List<Categoria>> =
        ApiResult.Success(emptyList(), ApiCode.CATEGORIES_OK)
    /** When null, a success echoing the call arguments is returned. */
    var crearCategoriaResult: ApiResult<Categoria>? = null
    var actualizarCategoriaResult: ApiResult<Unit> =
        ApiResult.Success(Unit, ApiCode.CATEGORY_UPDATED)
    var eliminarCategoriaResult: ApiResult<Unit> =
        ApiResult.Success(Unit, ApiCode.CATEGORY_DELETED)
    var obtenerResumenCategoriasResult: ApiResult<Map<String, Double>> =
        ApiResult.Success(emptyMap(), ApiCode.CATEGORY_SUMMARY_OK)
    var obtenerTendenciaResult: ApiResult<Tendencia> =
        ApiResult.Success(Tendencia(emptyList(), emptyList(), emptyList()), ApiCode.TREND_OK)
    var obtenerIngresosPorDiaSemanaResult: ApiResult<List<IngresoDiaSemana>> =
        ApiResult.Success(emptyList(), ApiCode.WEEKDAY_INCOME_OK)
    var obtenerSaludFinancieraResult: ApiResult<List<SaludFinancieraItem>> =
        ApiResult.Success(emptyList(), ApiCode.FINANCIAL_HEALTH_OK)
    var obtenerPresupuestosResult: ApiResult<List<Presupuesto>> =
        ApiResult.Success(emptyList(), ApiCode.BUDGETS_OK)
    /** When null, a success echoing the call arguments is returned. */
    var guardarPresupuestoResult: ApiResult<Presupuesto>? = null
    var eliminarPresupuestoResult: ApiResult<Unit> =
        ApiResult.Success(Unit, ApiCode.BUDGET_DELETED)
    var obtenerComparacionCategoriasResult: ApiResult<ComparacionCategorias> = ApiResult.Success(
        ComparacionCategorias(
            Periodo("2026-01-01", "2026-01-31"),
            Periodo("2025-12-01", "2025-12-31"),
            emptyList(), 0.0, 0.0, null
        ),
        ApiCode.CATEGORY_COMPARISON_OK
    )
    var obtenerProyeccionMensualResult: ApiResult<ProyeccionMensual> = ApiResult.Success(
        ProyeccionMensual("2026-01", 1, 31, 20, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true),
        ApiCode.MONTHLY_PROJECTION_OK
    )

    val registrarTransaccionCalls = mutableListOf<RegistrarTransaccionCall>()
    val listarTransaccionesCalls = mutableListOf<ListarTransaccionesCall>()
    val actualizarTransaccionCalls = mutableListOf<ActualizarTransaccionCall>()
    val eliminarTransaccionCalls = mutableListOf<Long>()
    var obtenerHoyCalls = 0
    var obtenerCuotaDiariaCalls = 0
    var obtenerResumenSemanalCalls = 0
    val obtenerResumenDiarioCalls = mutableListOf<String?>()
    var obtenerProgresoMetasCalls = 0
    val fijarMetaCalls = mutableListOf<FijarMetaCall>()
    var obtenerMetaActualCalls = 0
    val obtenerHistorialMetasCalls = mutableListOf<Int?>()
    var listarCategoriasCalls = 0
    val crearCategoriaCalls = mutableListOf<CrearCategoriaCall>()
    val actualizarCategoriaCalls = mutableListOf<ActualizarCategoriaCall>()
    val eliminarCategoriaCalls = mutableListOf<Long>()
    val obtenerResumenCategoriasCalls = mutableListOf<RangoCall>()
    val obtenerTendenciaCalls = mutableListOf<TendenciaCall>()
    val obtenerIngresosPorDiaSemanaCalls = mutableListOf<Int?>()
    var obtenerSaludFinancieraCalls = 0
    var obtenerPresupuestosCalls = 0
    val guardarPresupuestoCalls = mutableListOf<GuardarPresupuestoCall>()
    val eliminarPresupuestoCalls = mutableListOf<Long>()
    val obtenerComparacionCategoriasCalls = mutableListOf<ComparacionCall>()
    var obtenerProyeccionMensualCalls = 0

    override suspend fun registrarTransaccion(
        monto: Double,
        tipo: String,
        descripcion: String?,
        fecha: String?,
        categoriaId: Long?
    ): ApiResult<Unit> {
        registrarTransaccionCalls +=
            RegistrarTransaccionCall(monto, tipo, descripcion, fecha, categoriaId)
        return registrarTransaccionResult
    }

    override suspend fun listarTransacciones(
        tipo: String?,
        categoriaId: Long?,
        sinCategoria: Boolean?,
        desde: String?,
        hasta: String?,
        page: Int,
        size: Int,
        sort: String?
    ): ApiResult<PaginatedTransacciones> {
        listarTransaccionesCalls += ListarTransaccionesCall(
            tipo, categoriaId, sinCategoria, desde, hasta, page, size, sort
        )
        return listarTransaccionesResult
    }

    override suspend fun actualizarTransaccion(
        id: Long,
        monto: Double,
        tipo: String,
        descripcion: String?,
        fecha: String?,
        categoriaId: Long?
    ): ApiResult<Unit> {
        actualizarTransaccionCalls +=
            ActualizarTransaccionCall(id, monto, tipo, descripcion, fecha, categoriaId)
        return actualizarTransaccionResult
    }

    override suspend fun eliminarTransaccion(id: Long): ApiResult<Unit> {
        eliminarTransaccionCalls += id
        return eliminarTransaccionResult
    }

    override suspend fun obtenerHoy(): ApiResult<Double> {
        obtenerHoyCalls++
        return obtenerHoyResult
    }

    override suspend fun obtenerCuotaDiaria(): ApiResult<Double> {
        obtenerCuotaDiariaCalls++
        return obtenerCuotaDiariaResult
    }

    override suspend fun obtenerResumenSemanal(): ApiResult<List<ResumenSemanalDia>> {
        obtenerResumenSemanalCalls++
        return obtenerResumenSemanalResult
    }

    override suspend fun obtenerResumenDiario(mes: String?): ApiResult<List<ResumenDiarioDia>> {
        obtenerResumenDiarioCalls += mes
        return obtenerResumenDiarioResult
    }

    override suspend fun obtenerProgresoMetas(): ApiResult<ProgresoMetas> {
        obtenerProgresoMetasCalls++
        return obtenerProgresoMetasResult
    }

    override suspend fun fijarMeta(montoObjetivo: Double, diasLaborables: List<Int>): ApiResult<Meta> {
        fijarMetaCalls += FijarMetaCall(montoObjetivo, diasLaborables)
        return fijarMetaResult
    }

    override suspend fun obtenerMetaActual(): ApiResult<Meta> {
        obtenerMetaActualCalls++
        return obtenerMetaActualResult
    }

    override suspend fun obtenerHistorialMetas(meses: Int?): ApiResult<List<MetaHistorialItem>> {
        obtenerHistorialMetasCalls += meses
        return obtenerHistorialMetasResult
    }

    override suspend fun listarCategorias(): ApiResult<List<Categoria>> {
        listarCategoriasCalls++
        return listarCategoriasResult
    }

    override suspend fun crearCategoria(nombre: String, tipo: String): ApiResult<Categoria> {
        crearCategoriaCalls += CrearCategoriaCall(nombre, tipo)
        return crearCategoriaResult
            ?: ApiResult.Success(Categoria(1, nombre, tipo), ApiCode.CATEGORY_CREATED)
    }

    override suspend fun actualizarCategoria(id: Long, nombre: String): ApiResult<Unit> {
        actualizarCategoriaCalls += ActualizarCategoriaCall(id, nombre)
        return actualizarCategoriaResult
    }

    override suspend fun eliminarCategoria(id: Long): ApiResult<Unit> {
        eliminarCategoriaCalls += id
        return eliminarCategoriaResult
    }

    override suspend fun obtenerResumenCategorias(
        desde: String?,
        hasta: String?
    ): ApiResult<Map<String, Double>> {
        obtenerResumenCategoriasCalls += RangoCall(desde, hasta)
        return obtenerResumenCategoriasResult
    }

    override suspend fun obtenerTendencia(granularidad: String, ventana: Int): ApiResult<Tendencia> {
        obtenerTendenciaCalls += TendenciaCall(granularidad, ventana)
        return obtenerTendenciaResult
    }

    override suspend fun obtenerIngresosPorDiaSemana(
        ventana: Int?
    ): ApiResult<List<IngresoDiaSemana>> {
        obtenerIngresosPorDiaSemanaCalls += ventana
        return obtenerIngresosPorDiaSemanaResult
    }

    override suspend fun obtenerSaludFinanciera(): ApiResult<List<SaludFinancieraItem>> {
        obtenerSaludFinancieraCalls++
        return obtenerSaludFinancieraResult
    }

    override suspend fun obtenerPresupuestos(): ApiResult<List<Presupuesto>> {
        obtenerPresupuestosCalls++
        return obtenerPresupuestosResult
    }

    override suspend fun guardarPresupuesto(
        categoriaId: Long,
        montoMensual: Double
    ): ApiResult<Presupuesto> {
        guardarPresupuestoCalls += GuardarPresupuestoCall(categoriaId, montoMensual)
        return guardarPresupuestoResult ?: ApiResult.Success(
            Presupuesto(1, categoriaId, "General", montoMensual, 0.0, montoMensual, 0.0, false),
            ApiCode.BUDGET_SET
        )
    }

    override suspend fun eliminarPresupuesto(id: Long): ApiResult<Unit> {
        eliminarPresupuestoCalls += id
        return eliminarPresupuestoResult
    }

    override suspend fun obtenerComparacionCategorias(
        desde: String?,
        hasta: String?,
        compararCon: CompararCon
    ): ApiResult<ComparacionCategorias> {
        obtenerComparacionCategoriasCalls += ComparacionCall(desde, hasta, compararCon)
        return obtenerComparacionCategoriasResult
    }

    override suspend fun obtenerProyeccionMensual(): ApiResult<ProyeccionMensual> {
        obtenerProyeccionMensualCalls++
        return obtenerProyeccionMensualResult
    }
}
