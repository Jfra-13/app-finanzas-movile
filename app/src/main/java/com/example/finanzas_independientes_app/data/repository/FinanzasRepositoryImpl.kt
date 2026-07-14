package com.example.finanzas_independientes_app.data.repository

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.core.network.safeApiCall
import com.example.finanzas_independientes_app.core.network.safeUnitCall
import com.example.finanzas_independientes_app.data.local.dao.CategoriaDao
import com.example.finanzas_independientes_app.data.local.dao.MetaDao
import com.example.finanzas_independientes_app.data.local.dao.TransaccionDao
import com.example.finanzas_independientes_app.data.local.entity.toDomain
import com.example.finanzas_independientes_app.data.local.entity.toEntity
import com.example.finanzas_independientes_app.data.mapper.toDomain
import com.example.finanzas_independientes_app.data.remote.FinanzasApi
import com.example.finanzas_independientes_app.data.remote.dto.CategoriaRequest
import com.example.finanzas_independientes_app.data.remote.dto.MetaRequest
import com.example.finanzas_independientes_app.data.remote.dto.SetPresupuestoRequest
import com.example.finanzas_independientes_app.data.remote.dto.UpdateCategoriaRequest
import com.example.finanzas_independientes_app.data.remote.dto.TransaccionRegistroDTO
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
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanzasRepositoryImpl @Inject constructor(
    private val api: FinanzasApi,
    private val gson: Gson,
    private val transaccionDao: TransaccionDao,
    private val categoriaDao: CategoriaDao,
    private val metaDao: MetaDao
) : FinanzasRepository {

    // --- Transactions ---

    override suspend fun registrarTransaccion(
        monto: Double,
        tipo: String,
        descripcion: String?,
        fecha: String?,
        categoriaId: Long?
    ): ApiResult<Unit> = safeUnitCall(gson) {
        api.registrarTransaccion(TransaccionRegistroDTO(monto, tipo, descripcion, fecha, categoriaId))
    }

    override suspend fun listarTransacciones(
        tipo: String?,
        categoriaId: Long?,
        desde: String?,
        hasta: String?,
        page: Int,
        size: Int,
        sort: String?
    ): ApiResult<PaginatedTransacciones> {
        val sinFiltros = tipo == null && categoriaId == null && desde == null && hasta == null
        val result = safeApiCall(gson) {
            api.listarTransacciones(tipo, categoriaId, desde, hasta, page, size, sort)
        }
        return when (result) {
            is ApiResult.Success -> {
                val paginated = result.data.toDomain()
                // Cache only the unfiltered first page as the offline snapshot.
                if (page == 0 && sinFiltros) {
                    transaccionDao.replaceAll(paginated.content.map { it.toEntity() })
                }
                ApiResult.Success(paginated, result.code)
            }
            is ApiResult.Error -> {
                // Offline fallback: serve the cached first page when the network is down.
                if (result.error is AppError.Network && page == 0 && sinFiltros) {
                    val cached = transaccionDao.getAll().map { it.toDomain() }
                    if (cached.isNotEmpty()) {
                        ApiResult.Success(
                            PaginatedTransacciones(
                                content = cached,
                                totalElements = cached.size.toLong(),
                                totalPages = 1,
                                number = 0,
                                size = cached.size,
                                first = true,
                                last = true
                            ),
                            ApiCode.TRANSACTIONS_OK
                        )
                    } else {
                        result
                    }
                } else {
                    result
                }
            }
        }
    }

    override suspend fun actualizarTransaccion(
        id: Long,
        monto: Double,
        tipo: String,
        descripcion: String?,
        fecha: String?,
        categoriaId: Long?
    ): ApiResult<Unit> = safeUnitCall(gson) {
        api.actualizarTransaccion(id, TransaccionRegistroDTO(monto, tipo, descripcion, fecha, categoriaId))
    }

    override suspend fun eliminarTransaccion(id: Long): ApiResult<Unit> =
        safeUnitCall(gson) { api.eliminarTransaccion(id) }

    // --- Daily income & quota ---

    override suspend fun obtenerHoy(): ApiResult<Double> =
        safeApiCall(gson) { api.obtenerHoy() }

    override suspend fun obtenerCuotaDiaria(): ApiResult<Double> =
        safeApiCall(gson) { api.obtenerCuotaDiaria() }

    // --- Analytics & summaries ---

    override suspend fun obtenerResumenSemanal(): ApiResult<List<ResumenSemanalDia>> {
        val result = safeApiCall(gson) { api.obtenerResumenSemanal() }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.map { it.toDomain() }, result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun obtenerResumenDiario(mes: String?): ApiResult<List<ResumenDiarioDia>> {
        val result = safeApiCall(gson) { api.obtenerResumenDiario(mes) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.map { it.toDomain() }, result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun obtenerProgresoMetas(): ApiResult<ProgresoMetas> {
        val result = safeApiCall(gson) { api.obtenerProgresoMetas() }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
        }
    }

    // --- Goals ---

    override suspend fun fijarMeta(
        montoObjetivo: Double,
        diasLaborables: List<Int>
    ): ApiResult<Meta> {
        val result = safeApiCall(gson) {
            api.fijarMeta(MetaRequest(montoObjetivo, diasLaborables))
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun obtenerMetaActual(): ApiResult<Meta> {
        val result = safeApiCall(gson) { api.obtenerMetaActual() }
        return when (result) {
            is ApiResult.Success -> {
                val meta = result.data.toDomain()
                metaDao.replaceActiva(meta.toEntity())
                ApiResult.Success(meta, result.code)
            }
            is ApiResult.Error -> {
                if (result.error is AppError.Network) {
                    val cached = metaDao.getActiva()?.toDomain()
                    if (cached != null) ApiResult.Success(cached, ApiCode.GOAL_OK) else result
                } else {
                    result
                }
            }
        }
    }

    // --- Categories ---

    override suspend fun listarCategorias(): ApiResult<List<Categoria>> {
        val result = safeApiCall(gson) { api.listarCategorias() }
        return when (result) {
            is ApiResult.Success -> {
                val categorias = result.data.map { it.toDomain() }
                categoriaDao.replaceAll(categorias.map { it.toEntity() })
                ApiResult.Success(categorias, result.code)
            }
            is ApiResult.Error -> {
                if (result.error is AppError.Network) {
                    val cached = categoriaDao.getAll().map { it.toDomain() }
                    if (cached.isNotEmpty()) ApiResult.Success(cached, ApiCode.CATEGORIES_OK) else result
                } else {
                    result
                }
            }
        }
    }

    override suspend fun crearCategoria(nombre: String, tipo: String): ApiResult<Categoria> {
        val result = safeApiCall(gson) { api.crearCategoria(CategoriaRequest(nombre, tipo)) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun actualizarCategoria(id: Long, nombre: String): ApiResult<Unit> =
        safeUnitCall(gson) { api.actualizarCategoria(id, UpdateCategoriaRequest(nombre)) }

    override suspend fun eliminarCategoria(id: Long): ApiResult<Unit> =
        safeUnitCall(gson) { api.eliminarCategoria(id) }

    // --- Analytics (read-only) ---

    override suspend fun obtenerResumenCategorias(
        desde: String?,
        hasta: String?
    ): ApiResult<Map<String, Double>> =
        safeApiCall(gson) { api.obtenerResumenCategorias(desde, hasta) }

    override suspend fun obtenerTendencia(
        granularidad: String,
        ventana: Int
    ): ApiResult<Tendencia> {
        val result = safeApiCall(gson) { api.obtenerTendencia(granularidad, ventana) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun obtenerIngresosPorDiaSemana(
        ventana: Int?
    ): ApiResult<List<IngresoDiaSemana>> {
        val result = safeApiCall(gson) { api.obtenerIngresosPorDiaSemana(ventana) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.map { it.toDomain() }, result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun obtenerSaludFinanciera(): ApiResult<List<SaludFinancieraItem>> {
        val result = safeApiCall(gson) { api.obtenerSaludFinanciera() }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.map { it.toDomain() }, result.code)
            is ApiResult.Error -> result
        }
    }

    // --- Budgets, comparison & projection ---

    override suspend fun obtenerPresupuestos(): ApiResult<List<Presupuesto>> {
        val result = safeApiCall(gson) { api.listarPresupuestos() }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.map { it.toDomain() }, result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun guardarPresupuesto(
        categoriaId: Long,
        montoMensual: Double
    ): ApiResult<Presupuesto> {
        val result = safeApiCall(gson) {
            api.guardarPresupuesto(SetPresupuestoRequest(categoriaId, montoMensual))
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun eliminarPresupuesto(id: Long): ApiResult<Unit> =
        safeUnitCall(gson) { api.eliminarPresupuesto(id) }

    override suspend fun obtenerComparacionCategorias(
        desde: String?,
        hasta: String?,
        compararCon: CompararCon
    ): ApiResult<ComparacionCategorias> {
        val result = safeApiCall(gson) {
            api.obtenerComparacionCategorias(desde, hasta, compararCon.name)
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun obtenerProyeccionMensual(): ApiResult<ProyeccionMensual> {
        val result = safeApiCall(gson) { api.obtenerProyeccionMensual() }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
        }
    }
}
