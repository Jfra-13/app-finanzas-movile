package com.example.finanzas_independientes_app.presentation.analytics

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.ComparacionCategoriaItem
import com.example.finanzas_independientes_app.domain.model.ComparacionCategorias
import com.example.finanzas_independientes_app.domain.model.CompararCon
import com.example.finanzas_independientes_app.domain.model.IngresoDiaSemana
import com.example.finanzas_independientes_app.domain.model.Meta
import com.example.finanzas_independientes_app.domain.model.PaginatedTransacciones
import com.example.finanzas_independientes_app.domain.model.Periodo
import com.example.finanzas_independientes_app.domain.model.ProyeccionMensual
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.Tendencia
import com.example.finanzas_independientes_app.domain.model.Transaccion
import com.example.finanzas_independientes_app.fakes.FakeFinanzasRepository
import com.example.finanzas_independientes_app.fakes.apiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Pins the parallel load and its error channel (charts only), the secondary-insight
 * fallbacks, granularity/period/window/comparison switches, the category drill-down
 * request shapes and the quick-add flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val repo = FakeFinanzasRepository()

    private val comida = Categoria(1, "Comida", "EGRESO")

    private fun tendencia(vararg ingresos: Double) =
        Tendencia(ingresos.indices.map { "2026-0${it + 1}" }, ingresos.toList(), ingresos.map { 0.0 })

    private fun salud(severidad: String, code: String) =
        SaludFinancieraItem("ALERTA", code, severidad, "msg $code", null)

    private fun proyeccion(brecha: Double) = ProyeccionMensual(
        "2026-03", 10, 31, 15, 100.0, 50.0, 50.0, 310.0, 155.0, 155.0, 200.0, brecha, false
    )

    private fun comparacion(totalActual: Double) = ComparacionCategorias(
        Periodo("2026-03-01", "2026-03-31"),
        Periodo("2026-02-01", "2026-02-28"),
        listOf(ComparacionCategoriaItem("Comida", totalActual, 50.0, totalActual - 50.0, null)),
        totalActual, 50.0, null
    )

    private fun tx(id: Long) = Transaccion(id, 5.0, "EGRESO", null, "2026-03-01", 1, "Comida", 1)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AnalyticsViewModel(repo)

    @Test
    fun `cargar pide todo con los valores por defecto, publica cada seccion y limpia un error previo`() {
        val dias = listOf(IngresoDiaSemana("LUNES", 120.0))
        val meta = Meta(1, 500.0, "2026-03", listOf(1, 2), true)
        val vm = viewModel()
        repo.obtenerIngresosPorDiaSemanaResult = apiError(ApiCode.UNAUTHORIZED)
        vm.cargar()
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.errorMessage.value)

        repo.obtenerIngresosPorDiaSemanaResult = ApiResult.Success(dias, ApiCode.WEEKDAY_INCOME_OK)
        repo.obtenerTendenciaResult = ApiResult.Success(tendencia(10.0, 20.0), ApiCode.TREND_OK)
        repo.obtenerResumenCategoriasResult =
            ApiResult.Success(mapOf("Comida" to 80.0), ApiCode.CATEGORY_SUMMARY_OK)
        repo.listarCategoriasResult = ApiResult.Success(listOf(comida), ApiCode.CATEGORIES_OK)
        repo.obtenerMetaActualResult = ApiResult.Success(meta, ApiCode.GOAL_OK)
        repo.obtenerProyeccionMensualResult =
            ApiResult.Success(proyeccion(-90.0), ApiCode.MONTHLY_PROJECTION_OK)
        repo.obtenerComparacionCategoriasResult =
            ApiResult.Success(comparacion(90.0), ApiCode.CATEGORY_COMPARISON_OK)
        vm.cargar()

        assertEquals(listOf<Int?>(4, 4), repo.obtenerIngresosPorDiaSemanaCalls)
        assertEquals(FakeFinanzasRepository.TendenciaCall("MES", 6), repo.obtenerTendenciaCalls.last())
        assertEquals(
            FakeFinanzasRepository.ComparacionCall(null, null, CompararCon.PERIODO_ANTERIOR),
            repo.obtenerComparacionCategoriasCalls.last()
        )
        assertNull(vm.errorMessage.value)
        assertEquals(dias, vm.ingresosDiaSemana.value)
        assertEquals(tendencia(10.0, 20.0), vm.tendencia.value)
        assertEquals(mapOf("Comida" to 80.0), vm.resumenCategorias.value)
        assertEquals(listOf(comida), vm.categorias.value)
        assertEquals(meta, vm.metaActual.value)
        assertEquals(ProyeccionState.Data(proyeccion(-90.0)), vm.proyeccion.value)
        assertEquals(comparacion(90.0), vm.comparacion.value)
    }

    @Test
    fun `las senales de salud se publican ordenadas de la mas urgente a la menos`() {
        repo.obtenerSaludFinancieraResult = ApiResult.Success(
            listOf(salud("BAJA", "a"), salud("ALTA", "b"), salud("MEDIA", "c"), salud("ALTA", "d")),
            ApiCode.FINANCIAL_HEALTH_OK
        )

        val vm = viewModel()
        vm.cargar()

        assertEquals(listOf("b", "d", "c", "a"), vm.salud.value.map { it.code })
    }

    @Test
    fun `un error en un grafico llega a errorMessage con el mapeo propio y gana el ultimo grafico`() {
        val vm = viewModel()
        val esperados = listOf(
            ApiCode.UNAUTHORIZED to "Sesión expirada. Volvé a iniciar sesión.",
            ApiCode.META_NO_ENCONTRADA to "Sin meta activa para este período.",
            ApiCode.VALIDATION_ERROR to "Error del servidor: ${ApiCode.VALIDATION_ERROR.raw}"
        )
        for ((code, mensaje) in esperados) {
            repo.obtenerIngresosPorDiaSemanaResult = apiError(code)
            vm.cargar()
            assertEquals(mensaje, vm.errorMessage.value)
        }

        // Trend and summary fail together; summary is handled last, so its error stands.
        repo.obtenerIngresosPorDiaSemanaResult =
            ApiResult.Success(emptyList(), ApiCode.WEEKDAY_INCOME_OK)
        repo.obtenerTendenciaResult = apiError(ApiCode.META_NO_ENCONTRADA)
        repo.obtenerResumenCategoriasResult = apiError(ApiCode.UNAUTHORIZED)
        vm.cargar()
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.errorMessage.value)

        vm.limpiarError()
        assertNull(vm.errorMessage.value)
    }

    @Test
    fun `los fallos secundarios resetean sus secciones sin usar el canal de error y la proyeccion distingue sin meta`() {
        repo.obtenerMetaActualResult =
            ApiResult.Success(Meta(1, 500.0, "2026-03", emptyList(), true), ApiCode.GOAL_OK)
        repo.obtenerProyeccionMensualResult =
            ApiResult.Success(proyeccion(-90.0), ApiCode.MONTHLY_PROJECTION_OK)
        repo.obtenerComparacionCategoriasResult =
            ApiResult.Success(comparacion(90.0), ApiCode.CATEGORY_COMPARISON_OK)
        repo.obtenerSaludFinancieraResult =
            ApiResult.Success(listOf(salud("ALTA", "a")), ApiCode.FINANCIAL_HEALTH_OK)
        val vm = viewModel()
        vm.cargar()
        assertEquals(1, vm.salud.value.size)

        // The weekday chart is the only chart failure; secondary failures must not replace it.
        repo.obtenerIngresosPorDiaSemanaResult = apiError(ApiCode.UNAUTHORIZED)
        repo.obtenerMetaActualResult = apiError(ApiCode.VALIDATION_ERROR)
        repo.obtenerProyeccionMensualResult = apiError(ApiCode.META_NO_ENCONTRADA)
        repo.obtenerComparacionCategoriasResult = apiError(ApiCode.VALIDATION_ERROR)
        repo.obtenerSaludFinancieraResult = apiError(ApiCode.VALIDATION_ERROR)
        vm.cargar()

        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.errorMessage.value)
        assertNull(vm.metaActual.value)
        assertNull(vm.comparacion.value)
        assertEquals(ProyeccionState.SinMeta, vm.proyeccion.value)
        assertEquals(emptyList<SaludFinancieraItem>(), vm.salud.value)

        repo.obtenerProyeccionMensualResult = apiError(ApiCode.VALIDATION_ERROR)
        vm.cargar()
        assertEquals(ProyeccionState.Idle, vm.proyeccion.value)
    }

    @Test
    fun `cambiar periodo y granularidad recarga solo la tendencia con la ventana equivalente`() {
        val vm = viewModel()
        repo.obtenerTendenciaResult = ApiResult.Success(tendencia(1.0, 2.0, 3.0), ApiCode.TREND_OK)

        vm.cambiarPeriodo(6)
        vm.cambiarGranularidad(Granularidad.MES)
        assertEquals(emptyList<FakeFinanzasRepository.TendenciaCall>(), repo.obtenerTendenciaCalls)

        vm.cambiarGranularidad(Granularidad.SEMANA)
        vm.cambiarPeriodo(3)
        vm.cambiarPeriodo(1)
        vm.cambiarPeriodo(12)
        vm.cambiarGranularidad(Granularidad.MES)
        vm.cambiarModo(ModoAnalytics.EGRESOS)

        assertEquals(
            listOf(
                FakeFinanzasRepository.TendenciaCall("SEMANA", 26),
                FakeFinanzasRepository.TendenciaCall("SEMANA", 13),
                FakeFinanzasRepository.TendenciaCall("SEMANA", 4),
                FakeFinanzasRepository.TendenciaCall("SEMANA", 52),
                FakeFinanzasRepository.TendenciaCall("MES", 12)
            ),
            repo.obtenerTendenciaCalls
        )
        assertEquals(Granularidad.MES, vm.granularidad.value)
        assertEquals(tendencia(1.0, 2.0, 3.0), vm.tendencia.value)
        // The lens is UI-only: switching it fetched nothing.
        assertEquals(ModoAnalytics.EGRESOS, vm.modo.value)
        assertEquals(5, repo.obtenerTendenciaCalls.size)
    }

    @Test
    fun `un error al recargar la tendencia llega a errorMessage y conserva la tendencia previa`() {
        repo.obtenerTendenciaResult = ApiResult.Success(tendencia(1.0), ApiCode.TREND_OK)
        val vm = viewModel()
        vm.cargar()

        repo.obtenerTendenciaResult = apiError(ApiCode.UNAUTHORIZED)
        vm.cambiarPeriodo(12)

        assertEquals(2, repo.obtenerTendenciaCalls.size)
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.errorMessage.value)
        assertEquals(tendencia(1.0), vm.tendencia.value)
    }

    @Test
    fun `cambiarVentanaSemanas pide solo el grafico de dias, ignora la ventana repetida y reporta errores`() {
        val vm = viewModel()
        vm.cambiarVentanaSemanas(4)
        assertEquals(emptyList<Int?>(), repo.obtenerIngresosPorDiaSemanaCalls)

        val dias = listOf(IngresoDiaSemana("MARTES", 75.0))
        repo.obtenerIngresosPorDiaSemanaResult = ApiResult.Success(dias, ApiCode.WEEKDAY_INCOME_OK)
        vm.cambiarVentanaSemanas(8)

        assertEquals(listOf<Int?>(8), repo.obtenerIngresosPorDiaSemanaCalls)
        assertEquals(dias, vm.ingresosDiaSemana.value)

        repo.obtenerIngresosPorDiaSemanaResult = apiError(ApiCode.UNAUTHORIZED)
        vm.cambiarVentanaSemanas(12)

        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.errorMessage.value)
        assertEquals(dias, vm.ingresosDiaSemana.value)
    }

    @Test
    fun `cambiarComparacion pide solo la comparacion con la nueva base y la anula si falla`() {
        val vm = viewModel()
        vm.cambiarComparacion(CompararCon.PERIODO_ANTERIOR)
        assertEquals(0, repo.obtenerComparacionCategoriasCalls.size)

        repo.obtenerComparacionCategoriasResult =
            ApiResult.Success(comparacion(90.0), ApiCode.CATEGORY_COMPARISON_OK)
        vm.cambiarComparacion(CompararCon.MISMO_PERIODO_ANIO_ANTERIOR)

        assertEquals(
            listOf(
                FakeFinanzasRepository.ComparacionCall(
                    null, null, CompararCon.MISMO_PERIODO_ANIO_ANTERIOR
                )
            ),
            repo.obtenerComparacionCategoriasCalls
        )
        assertEquals(CompararCon.MISMO_PERIODO_ANIO_ANTERIOR, vm.compararCon.value)
        assertEquals(comparacion(90.0), vm.comparacion.value)

        repo.obtenerComparacionCategoriasResult = apiError(ApiCode.VALIDATION_ERROR)
        vm.cambiarComparacion(CompararCon.PERIODO_ANTERIOR)

        assertNull(vm.comparacion.value)
    }

    @Test
    fun `una recarga completa usa la base de comparacion elegida`() {
        val vm = viewModel()
        vm.cambiarComparacion(CompararCon.MISMO_PERIODO_ANIO_ANTERIOR)

        vm.cargar()

        assertEquals(
            CompararCon.MISMO_PERIODO_ANIO_ANTERIOR,
            repo.obtenerComparacionCategoriasCalls.last().compararCon
        )
    }

    @Test
    fun `el detalle de una categoria conocida filtra por id y publica las transacciones`() {
        repo.listarCategoriasResult = ApiResult.Success(listOf(comida), ApiCode.CATEGORIES_OK)
        repo.listarTransaccionesResult = ApiResult.Success(
            PaginatedTransacciones(listOf(tx(1), tx(2)), 2, 1, 0, 50, true, true),
            ApiCode.TRANSACTIONS_OK
        )
        val vm = viewModel()
        vm.cargar()

        vm.cargarDetalleCategoria("Comida")

        assertEquals(
            listOf(
                FakeFinanzasRepository.ListarTransaccionesCall(
                    tipo = null, categoriaId = 1, sinCategoria = null, desde = null, hasta = null,
                    page = 0, size = 50, sort = null
                )
            ),
            repo.listarTransaccionesCalls
        )
        assertEquals(DetalleCategoriaState.Success("Comida", listOf(tx(1), tx(2))), vm.detalleCategoria.value)

        vm.limpiarDetalle()
        assertEquals(DetalleCategoriaState.Idle, vm.detalleCategoria.value)
    }

    @Test
    fun `una categoria desconocida o con categorias caidas usa el filtro sin categoria y nunca manda ambos`() {
        repo.listarCategoriasResult = ApiResult.Success(listOf(comida), ApiCode.CATEGORIES_OK)
        val vm = viewModel()
        vm.cargar()

        vm.cargarDetalleCategoria("Sin categoría")

        with(repo.listarTransaccionesCalls.last()) {
            assertNull(categoriaId)
            assertEquals(true, sinCategoria)
            assertEquals("EGRESO", tipo)
            assertEquals(50, size)
        }

        // A failed categories reload empties the name map, so a known name loses its id.
        repo.listarCategoriasResult = apiError(ApiCode.VALIDATION_ERROR)
        vm.cargar()
        vm.cargarDetalleCategoria("Comida")

        assertEquals(2, repo.listarTransaccionesCalls.size)
        with(repo.listarTransaccionesCalls.last()) {
            assertNull(categoriaId)
            assertEquals(true, sinCategoria)
        }
    }

    @Test
    fun `un error en el detalle publica Error con el nombre y el mensaje mapeado`() {
        repo.listarTransaccionesResult = apiError(ApiCode.UNAUTHORIZED)
        val vm = viewModel()
        vm.cargar()

        vm.cargarDetalleCategoria("Comida")

        assertEquals(
            DetalleCategoriaState.Error("Comida", "Sesión expirada. Volvé a iniciar sesión."),
            vm.detalleCategoria.value
        )
    }

    @Test
    fun `registrar con monto invalido avisa y no llama al repo`() {
        val vm = viewModel()

        for (monto in listOf("abc", "0", "-5", "   ", "")) {
            vm.limpiarMensaje()
            vm.registrarTransaccion(monto, "INGRESO", null, null)
            assertEquals("Ingresá un monto válido (mayor a 0)", vm.mensajeUI.value)
        }

        assertEquals(0, repo.registrarTransaccionCalls.size)
    }

    @Test
    fun `registrar con exito manda los datos normalizados, informa segun el tipo y recarga`() {
        val vm = viewModel()

        vm.registrarTransaccion(" 12.5 ", "INGRESO", "  ", 3)

        assertEquals("¡Ingreso registrado!", vm.mensajeUI.value)
        assertEquals(1, repo.obtenerProyeccionMensualCalls)

        vm.registrarTransaccion("40", "EGRESO", "Cafe", null)

        assertEquals("Egreso registrado.", vm.mensajeUI.value)
        assertEquals(2, repo.obtenerProyeccionMensualCalls)
        assertEquals(
            listOf(
                FakeFinanzasRepository.RegistrarTransaccionCall(12.5, "INGRESO", null, null, 3),
                FakeFinanzasRepository.RegistrarTransaccionCall(40.0, "EGRESO", "Cafe", null, null)
            ),
            repo.registrarTransaccionCalls
        )

        vm.limpiarMensaje()
        assertNull(vm.mensajeUI.value)
    }

    @Test
    fun `registrar con error informa el mensaje mapeado y no recarga`() {
        repo.registrarTransaccionResult = apiError(ApiCode.UNAUTHORIZED)
        val vm = viewModel()

        vm.registrarTransaccion("10", "EGRESO", null, null)

        assertEquals(1, repo.registrarTransaccionCalls.size)
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", vm.mensajeUI.value)
        assertEquals(0, repo.obtenerProyeccionMensualCalls)
    }
}
