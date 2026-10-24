package com.example.finanzas_independientes_app.presentation.auth

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pins local validation (no network on bad input) and code-driven registration errors. */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {

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
    fun `campos vacios no llegan a la red`() {
        val viewModel = RegistroViewModel(repo)

        viewModel.registrarUsuario("Ana", "", "123456", "123456")

        assertEquals("Todos los campos son obligatorios", viewModel.mensajeUI.value)
        assertTrue(repo.registroCalls.isEmpty())
    }

    @Test
    fun `email sin arroba no llega a la red`() {
        val viewModel = RegistroViewModel(repo)

        viewModel.registrarUsuario("Ana", "ana.test.com", "123456", "123456")

        assertEquals("Por favor ingresa un correo válido", viewModel.mensajeUI.value)
        assertTrue(repo.registroCalls.isEmpty())
        assertFalse(viewModel.registroExitoso.value)
    }

    @Test
    fun `passwords distintas o cortas no llegan a la red`() {
        val viewModel = RegistroViewModel(repo)

        viewModel.registrarUsuario("Ana", "ana@test.com", "123456", "654321")
        assertEquals("Las contraseñas no coinciden", viewModel.mensajeUI.value)

        viewModel.registrarUsuario("Ana", "ana@test.com", "123", "123")
        assertEquals("La contraseña debe tener al menos 6 caracteres", viewModel.mensajeUI.value)

        assertTrue(repo.registroCalls.isEmpty())
        assertFalse(viewModel.registroExitoso.value)
    }

    @Test
    fun `registro exitoso marca exito y envia los datos`() {
        val viewModel = RegistroViewModel(repo)

        viewModel.registrarUsuario("Ana", "ana@test.com", "123456", "123456")

        assertTrue(viewModel.registroExitoso.value)
        assertEquals(listOf(listOf("Ana", "ana@test.com", "123456", null)), repo.registroCalls)
    }

    @Test
    fun `EMAIL_DUPLICADO se mapea por codigo ignorando el mensaje del server`() {
        repo.registroResult = apiError(ApiCode.EMAIL_DUPLICADO, message = "texto arbitrario")
        val viewModel = RegistroViewModel(repo)

        viewModel.registrarUsuario("Ana", "ana@test.com", "123456", "123456")

        assertEquals("Ese correo ya está registrado.", viewModel.mensajeUI.value)
        assertFalse(viewModel.registroExitoso.value)
    }

    @Test
    fun `fallo de red no marca exito`() {
        repo.registroResult = ApiResult.Error(AppError.Network)
        val viewModel = RegistroViewModel(repo)

        viewModel.registrarUsuario("Ana", "ana@test.com", "123456", "123456")

        assertFalse(viewModel.registroExitoso.value)
        assertEquals("Sin conexión. Revisá tu internet e intentá de nuevo.", viewModel.mensajeUI.value)
    }
}
