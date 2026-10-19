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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pins password validation before the reset call and OTP-code-driven error mapping. */
@OptIn(ExperimentalCoroutinesApi::class)
class NewPasswordViewModelTest {

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
    fun `passwords distintas o cortas no llegan a la red`() {
        val viewModel = NewPasswordViewModel(repo)

        viewModel.restablecer("ana@test.com", "1234", "123456", "654321")
        assertEquals("Las contraseñas no coinciden.", viewModel.mensajeError.value)

        viewModel.restablecer("ana@test.com", "1234", "123", "123")
        assertEquals("La contraseña debe tener al menos 6 caracteres.", viewModel.mensajeError.value)

        assertTrue(repo.resetPasswordCalls.isEmpty())
        assertFalse(viewModel.resetExitoso.value)
    }

    @Test
    fun `reset exitoso envia email otp y password`() {
        val viewModel = NewPasswordViewModel(repo)

        viewModel.restablecer("ana@test.com", "1234", "123456", "123456")

        assertTrue(viewModel.resetExitoso.value)
        assertEquals(listOf(Triple("ana@test.com", "1234", "123456")), repo.resetPasswordCalls)
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `cada codigo de OTP se mapea por codigo y no por mensaje`() {
        val expected = mapOf(
            ApiCode.OTP_INVALIDO to "El código no es válido. Puede que ya fue usado. Pedí uno nuevo.",
            ApiCode.OTP_EXPIRADO to "El código venció. Volvé al inicio y pedí uno nuevo.",
            ApiCode.OTP_BLOQUEADO to
                "Demasiados intentos fallidos. Esperá unos minutos e iniciá el proceso de nuevo."
        )
        expected.forEach { (code, message) ->
            repo.resetPasswordResult = apiError(code, message = "mismo texto para todos")
            val viewModel = NewPasswordViewModel(repo)

            viewModel.restablecer("ana@test.com", "1234", "123456", "123456")

            assertEquals(message, viewModel.mensajeError.value)
            assertFalse(viewModel.resetExitoso.value)
        }
    }

    @Test
    fun `un codigo sin mapeo especifico usa el mensaje generico`() {
        repo.resetPasswordResult = apiError(ApiCode.UNKNOWN, message = "detalle interno")
        val viewModel = NewPasswordViewModel(repo)

        viewModel.restablecer("ana@test.com", "1234", "123456", "123456")

        assertEquals(
            "No se pudo restablecer la contraseña. Intentá de nuevo.",
            viewModel.mensajeError.value
        )
        assertFalse(viewModel.resetExitoso.value)
    }
}
