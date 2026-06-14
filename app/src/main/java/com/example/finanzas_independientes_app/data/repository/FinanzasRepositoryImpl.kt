package com.example.finanzas_independientes_app.data.repository

import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.safeApiCall
import com.example.finanzas_independientes_app.core.network.safeUnitCall
import com.example.finanzas_independientes_app.data.mapper.toDomain
import com.example.finanzas_independientes_app.data.remote.FinanzasApi
import com.example.finanzas_independientes_app.data.remote.dto.CategoriaRequest
import com.example.finanzas_independientes_app.data.remote.dto.MetaRequest
import com.example.finanzas_independientes_app.data.remote.dto.TransaccionRegistroDTO
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.Meta
import com.example.finanzas_independientes_app.domain.model.PaginatedTransacciones
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.TendenciaMensual
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanzasRepositoryImpl @Inject constructor(
    private val api: FinanzasApi,
    private val gson: Gson
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
        page: Int,
        size: Int,
        sort: String?
    ): ApiResult<PaginatedTransacciones> {
        val result = safeApiCall(gson) {
            api.listarTransacciones(tipo, categoriaId, page, size, sort)
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
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
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
        }
    }

    // --- Categories ---

    override suspend fun listarCategorias(): ApiResult<List<Categoria>> {
        val result = safeApiCall(gson) { api.listarCategorias() }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.map { it.toDomain() }, result.code)
            is ApiResult.Error -> result
        }
    }

    override suspend fun crearCategoria(nombre: String, tipo: String): ApiResult<Categoria> {
        val result = safeApiCall(gson) { api.crearCategoria(CategoriaRequest(nombre, tipo)) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
            is ApiResult.Error -> result
        }
    }

    // --- Analytics (read-only) ---

    override suspend fun obtenerResumenCategorias(): ApiResult<Map<String, Double>> =
        safeApiCall(gson) { api.obtenerResumenCategorias() }

    override suspend fun obtenerTendenciaMensual(meses: Int?): ApiResult<TendenciaMensual> {
        val result = safeApiCall(gson) { api.obtenerTendenciaMensual(meses) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(), result.code)
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
}
