package com.example.finanzas_independientes_app.presentation.dashboard

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * ingresoHoy is a numeric field: on any error it must fall back to "--". Before the
 * fix, UNAUTHORIZED and VALIDATION_ERROR put a full error sentence in it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardIngresoHoyTest {

    // Only the calls cargarResumen() makes are implemented; anything else throws.
    private class FakeRepo : FinanzasRepository by notNeeded() {
        var hoy: ApiResult<Double> = ApiResult.Success(0.0, ApiCode.TODAY_INCOME_OK)

        override suspend fun obtenerHoy(): ApiResult<Double> = hoy
        override suspend fun obtenerCuotaDiaria(): ApiResult<Double> =
            ApiResult.Error(AppError.Network)
        override suspend fun obtenerProgresoMetas(): ApiResult<ProgresoMetas> =
            ApiResult.Error(AppError.Network)
        override suspend fun obtenerResumenSemanal(): ApiResult<List<ResumenSemanalDia>> =
            ApiResult.Error(AppError.Network)

        companion object {
            private fun notNeeded(): FinanzasRepository = Proxy.newProxyInstance(
                FinanzasRepository::class.java.classLoader,
                arrayOf(FinanzasRepository::class.java)
            ) { _, method, _ ->
                throw UnsupportedOperationException("${method.name} not used by DashboardIngresoHoyTest")
            } as FinanzasRepository
        }
    }

    private val repo = FakeRepo()

    private fun apiError(code: ApiCode) =
        AppError.Api(code = code, rawCode = code.raw, httpStatus = 400, message = null, fieldErrors = emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cualquier error al cargar el ingreso de hoy muestra -- y nunca un mensaje`() {
        val vm = DashboardViewModel(repo)
        val errores = listOf(
            apiError(ApiCode.UNAUTHORIZED),
            apiError(ApiCode.VALIDATION_ERROR),
            apiError(ApiCode.META_NO_ENCONTRADA),
            apiError(ApiCode.UNKNOWN),
            AppError.Network
        )

        for (error in errores) {
            repo.hoy = ApiResult.Success(5.0, ApiCode.TODAY_INCOME_OK)
            vm.cargarResumen()
            assertEquals("5.00", vm.ingresoHoy.value)

            repo.hoy = ApiResult.Error(error)
            vm.cargarResumen()
            assertEquals("error $error", "--", vm.ingresoHoy.value)
        }
    }
}
