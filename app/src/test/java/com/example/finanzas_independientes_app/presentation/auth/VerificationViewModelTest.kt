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

/** Pins OTP format validation and that each OTP failure is mapped by ApiCode, not by server message. */
@OptIn(ExperimentalCoroutinesApi::class)
class VerificationViewModelTest {

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
    fun `otp que no son 4 digitos no llega a la red`() {
        val viewModel = VerificationViewModel(repo)

        viewModel.verificar("ana@test.com", "123")
        viewModel.verificar("ana@test.com", "12a4")

        assertTrue(repo.verifyOtpCalls.isEmpty())
        assertEquals("El código debe tener exactamente 4 dígitos.", viewModel.mensajeError.value)
    }

    @Test
    fun `otp valido emite email y codigo`() {
        val viewModel = VerificationViewModel(repo)

        viewModel.verificar("ana@test.com", "1234")

        assertEquals("ana@test.com" to "1234", viewModel.otpVerificado.value)
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `cada codigo de OTP tiene su propio mensaje`() {
        val expected = mapOf(
            ApiCode.OTP_INVALIDO to "El código ingresado no es válido. Revisalo e intentá de nuevo.",
            ApiCode.OTP_EXPIRADO to "El código venció. Volvé atrás y pedí uno nuevo.",
            ApiCode.OTP_BLOQUEADO to
                "Demasiados intentos fallidos. Esperá unos minutos antes de intentar de nuevo."
        )
        expected.forEach { (code, message) ->
            repo.verifyOtpResult = apiError(code, message = "mismo texto para todos")
            val viewModel = VerificationViewModel(repo)

            viewModel.verificar("ana@test.com", "1234")

            assertEquals(message, viewModel.mensajeError.value)
            assertNull(viewModel.otpVerificado.value)
        }
    }

    @Test
    fun `un codigo sin mapeo especifico usa el mensaje generico`() {
        repo.verifyOtpResult = apiError(ApiCode.UNKNOWN, message = "detalle interno")
        val viewModel = VerificationViewModel(repo)

        viewModel.verificar("ana@test.com", "1234")

        assertEquals("No se pudo verificar el código. Intentá de nuevo.", viewModel.mensajeError.value)
        assertNull(viewModel.otpVerificado.value)
    }
}
