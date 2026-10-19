package com.example.finanzas_independientes_app.presentation.profile

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.fakes.FakeAuthRepository
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

/** Pins that the profile loads on init and that a failed load never wipes or breaks the UI state. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

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
    fun `carga el perfil al crearse`() {
        val viewModel = ProfileViewModel(repo)

        assertEquals("Ana", viewModel.perfil.value?.nombre)
        assertEquals(1, repo.obtenerPerfilCalls)
    }

    @Test
    fun `un error deja el perfil en null sin romper`() {
        repo.obtenerPerfilResult = apiError(ApiCode.USUARIO_NO_ENCONTRADO)
        val viewModel = ProfileViewModel(repo)

        assertNull(viewModel.perfil.value)
        assertEquals(1, repo.obtenerPerfilCalls)
    }

    @Test
    fun `un fallo posterior no borra el perfil ya cargado`() {
        val viewModel = ProfileViewModel(repo)
        repo.obtenerPerfilResult = ApiResult.Error(AppError.Network)

        viewModel.cargar()

        assertEquals("Ana", viewModel.perfil.value?.nombre)
        assertEquals(2, repo.obtenerPerfilCalls)
    }
}
