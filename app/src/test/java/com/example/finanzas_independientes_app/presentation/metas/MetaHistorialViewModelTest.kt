package com.example.finanzas_independientes_app.presentation.metas

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.MetaHistorialItem
import com.example.finanzas_independientes_app.fakes.FakeFinanzasRepository
import com.example.finanzas_independientes_app.fakes.apiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pins that the history loads on init, and that load errors map to their own Spanish messages. */
@OptIn(ExperimentalCoroutinesApi::class)
class MetaHistorialViewModelTest {

    private val repo = FakeFinanzasRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `al crearse carga el historial y termina con loading apagado`() = runTest {
        // isLoading is set inside the launched coroutine, so true is not observable
        // from outside before it runs; only the final state is pinned.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val items = listOf(MetaHistorialItem("2026-01", 1000.0, 1200.0, true))
        repo.obtenerHistorialMetasResult = ApiResult.Success(items, ApiCode.GOALS_HISTORY_OK)

        val viewModel = MetaHistorialViewModel(repo)
        advanceUntilIdle()

        assertEquals(1, repo.obtenerHistorialMetasCalls.size)
        assertEquals(items, viewModel.items.value)
        assertNull(viewModel.mensajeError.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sesion expirada se decide por codigo y muestra el mensaje de sesion`() {
        repo.obtenerHistorialMetasResult = apiError(ApiCode.UNAUTHORIZED)

        val viewModel = MetaHistorialViewModel(repo)

        assertEquals(1, repo.obtenerHistorialMetasCalls.size)
        assertEquals("Sesión expirada. Volvé a iniciar sesión.", viewModel.mensajeError.value)
        assertTrue(viewModel.items.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `cualquier otro codigo de api cae en el mensaje generico`() {
        repo.obtenerHistorialMetasResult = apiError(ApiCode.INTERNAL_SERVER_ERROR)

        val viewModel = MetaHistorialViewModel(repo)

        assertEquals(1, repo.obtenerHistorialMetasCalls.size)
        assertEquals("No se pudo cargar el historial de metas.", viewModel.mensajeError.value)
    }

    @Test
    fun `recargar tras un error limpia el mensaje y reemplaza los items`() {
        repo.obtenerHistorialMetasResult = apiError(ApiCode.INTERNAL_SERVER_ERROR)
        val viewModel = MetaHistorialViewModel(repo)
        val items = listOf(MetaHistorialItem("2025-12", 500.0, 100.0, false))
        repo.obtenerHistorialMetasResult = ApiResult.Success(items, ApiCode.GOALS_HISTORY_OK)

        viewModel.cargar()

        assertEquals(2, repo.obtenerHistorialMetasCalls.size)
        assertNull(viewModel.mensajeError.value)
        assertEquals(items, viewModel.items.value)
    }

    @Test
    fun `si falla una recarga los items anteriores se conservan junto al error`() {
        val items = listOf(MetaHistorialItem("2026-01", 1000.0, 1200.0, true))
        repo.obtenerHistorialMetasResult = ApiResult.Success(items, ApiCode.GOALS_HISTORY_OK)
        val viewModel = MetaHistorialViewModel(repo)
        repo.obtenerHistorialMetasResult = apiError(ApiCode.INTERNAL_SERVER_ERROR)

        viewModel.cargar()

        assertEquals(2, repo.obtenerHistorialMetasCalls.size)
        assertEquals(items, viewModel.items.value)
        assertEquals("No se pudo cargar el historial de metas.", viewModel.mensajeError.value)
    }
}
