package com.example.finanzas_independientes_app.presentation.calendar

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ResumenDiarioDia
import com.example.finanzas_independientes_app.fakes.FakeFinanzasRepository
import com.example.finanzas_independientes_app.fakes.apiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale

/** Pins day-detail resolution: month index conversion, zeros for idle days, per-month caching and failed fetches. */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val repo = FakeFinanzasRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Same computation as production, so the expectation tracks the real clock. */
    private fun mesActual(): String {
        val hoy = Calendar.getInstance()
        return String.format(
            Locale.US, "%04d-%02d", hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH) + 1
        )
    }

    private fun conMetaDiaria(meta: Double) {
        repo.obtenerProgresoMetasResult = ApiResult.Success(
            ProgresoMetas(0.0, meta, 0.0, 0.0, 0.0, 0.0),
            ApiCode.GOALS_PROGRESS_OK
        )
    }

    @Test
    fun `seleccionar convierte el mes base 0 y devuelve los totales del dia`() {
        conMetaDiaria(50.0)
        repo.obtenerResumenDiarioResult = ApiResult.Success(
            listOf(ResumenDiarioDia("2026-03-05", 100.0, 30.0)),
            ApiCode.DAILY_SUMMARY_OK
        )
        val viewModel = CalendarViewModel(repo)
        viewModel.cargar()

        viewModel.seleccionar(2026, 2, 5)

        assertTrue("2026-03" in repo.obtenerResumenDiarioCalls)
        assertEquals(DayDetail(100.0, 30.0, 50.0, true), viewModel.detalle.value)
    }

    @Test
    fun `dia sin actividad en un mes cargado da ceros con datos y la meta diaria`() {
        conMetaDiaria(50.0)
        val viewModel = CalendarViewModel(repo)
        viewModel.cargar()

        viewModel.seleccionar(2026, 0, 9)

        assertEquals(DayDetail(0.0, 0.0, 50.0, true), viewModel.detalle.value)
    }

    @Test
    fun `un mes ya cargado no se vuelve a pedir`() {
        val viewModel = CalendarViewModel(repo)

        viewModel.seleccionar(2026, 0, 1)
        viewModel.seleccionar(2026, 0, 2)
        viewModel.seleccionar(2026, 1, 2)

        assertEquals(1, repo.obtenerResumenDiarioCalls.count { it == "2026-01" })
        assertEquals(1, repo.obtenerResumenDiarioCalls.count { it == "2026-02" })
    }

    @Test
    fun `si falla el mes el detalle queda sin datos y el reintento vuelve a pedirlo`() {
        repo.obtenerResumenDiarioResult = apiError(ApiCode.INTERNAL_SERVER_ERROR)
        val viewModel = CalendarViewModel(repo)

        viewModel.seleccionar(2026, 0, 1)
        assertEquals(DayDetail(conDatos = false), viewModel.detalle.value)

        repo.obtenerResumenDiarioResult = ApiResult.Success(emptyList(), ApiCode.DAILY_SUMMARY_OK)
        viewModel.seleccionar(2026, 0, 1)

        assertEquals(2, repo.obtenerResumenDiarioCalls.count { it == "2026-01" })
        assertEquals(DayDetail(conDatos = true), viewModel.detalle.value)
    }

    @Test
    fun `cargar pide el resumen del mes actual`() {
        val viewModel = CalendarViewModel(repo)

        viewModel.cargar()

        assertEquals(listOf<String?>(mesActual()), repo.obtenerResumenDiarioCalls)
    }

    @Test
    fun `si falla el progreso de metas la meta diaria vuelve a cero`() {
        conMetaDiaria(50.0)
        val viewModel = CalendarViewModel(repo)
        viewModel.cargar()
        viewModel.seleccionar(2026, 0, 1)
        assertEquals(50.0, viewModel.detalle.value!!.estimado, 0.0)

        repo.obtenerProgresoMetasResult = apiError(ApiCode.INTERNAL_SERVER_ERROR)
        viewModel.cargar()
        viewModel.seleccionar(2026, 0, 1)

        assertEquals(2, repo.obtenerProgresoMetasCalls)
        assertEquals(DayDetail(0.0, 0.0, 0.0, true), viewModel.detalle.value)
    }
}
