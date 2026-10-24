package com.example.finanzas_independientes_app.presentation.auth

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

/** Pins that an invalid email never reaches the network and that a sent OTP forwards the trimmed email. */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

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
    fun `email sin arroba no llega a la red`() {
        val viewModel = ForgotPasswordViewModel(repo)

        viewModel.enviarOtp("ana.test.com")

        assertTrue(repo.forgotPasswordCalls.isEmpty())
        assertNull(viewModel.otpEnviado.value)
        assertEquals("Ingresá un correo electrónico válido.", viewModel.mensajeError.value)
    }

    @Test
    fun `envio exitoso emite el email recortado y apaga loading`() {
        val viewModel = ForgotPasswordViewModel(repo)

        viewModel.enviarOtp("  ana@test.com ")

        assertEquals("ana@test.com", viewModel.otpEnviado.value)
        assertEquals(listOf("ana@test.com"), repo.forgotPasswordCalls)
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `el error se decide por codigo y no por el mensaje del server`() {
        repo.forgotPasswordResult = apiError(ApiCode.OTP_BLOQUEADO, message = "cualquier cosa")
        val viewModel = ForgotPasswordViewModel(repo)

        viewModel.enviarOtp("ana@test.com")

        assertEquals("Demasiados intentos. Esperá unos minutos.", viewModel.mensajeError.value)
        assertNull(viewModel.otpEnviado.value)
        assertFalse(viewModel.loading.value)
    }
}
