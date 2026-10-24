package com.example.finanzas_independientes_app.presentation.business

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.fakes.FakeAuthRepository
import com.example.finanzas_independientes_app.fakes.apiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pins that choosing a business type signals navigation only on success. */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectBusinessViewModelTest {

    private val repo = FakeAuthRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `seleccion exitosa envia el tipo y senala navegacion`() {
        val viewModel = SelectBusinessViewModel(repo)

        viewModel.seleccionar("SERVICIOS")

        assertEquals(listOf("SERVICIOS"), repo.actualizarNegocioCalls)
        assertTrue(viewModel.negocioSeleccionado.value)
        assertFalse(viewModel.loading.value)
        assertNull(viewModel.mensajeError.value)
    }

    @Test
    fun `error de API no navega y muestra mensaje por codigo`() {
        repo.actualizarNegocioResult = apiError(ApiCode.FORBIDDEN, message = "texto arbitrario")
        val viewModel = SelectBusinessViewModel(repo)

        viewModel.seleccionar("SERVICIOS")

        assertFalse(viewModel.negocioSeleccionado.value)
        assertEquals("No tenés permiso para esta acción.", viewModel.mensajeError.value)
        assertFalse(viewModel.loading.value)
    }
}
