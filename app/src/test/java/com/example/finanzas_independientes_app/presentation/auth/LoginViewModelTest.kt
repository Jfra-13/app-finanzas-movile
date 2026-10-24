package com.example.finanzas_independientes_app.presentation.auth

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.AuthSession
import com.example.finanzas_independientes_app.domain.model.Perfil
import com.example.finanzas_independientes_app.domain.repository.AuthRepository
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
 * The backend locks login for 15 minutes after 5 failed attempts and answers
 * 429 LOGIN_BLOQUEADO. The user must see why, chosen by code, not by message.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private class FakeAuthRepository(
        private val loginResult: ApiResult<AuthSession>
    ) : AuthRepository {

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
            loginResult

        override suspend fun registro(
            nombre: String,
            email: String,
            password: String,
            tipoNegocio: String?
        ): ApiResult<Unit> = notNeeded()

        override suspend fun forgotPassword(email: String): ApiResult<Unit> = notNeeded()

        override suspend fun verifyOtp(email: String, otp: String): ApiResult<Unit> = notNeeded()

        override suspend fun resetPassword(
            email: String,
            otp: String,
            newPassword: String
        ): ApiResult<Unit> = notNeeded()

        override suspend fun actualizarNegocio(tipoNegocio: String): ApiResult<Unit> = notNeeded()

        override suspend fun obtenerPerfil(): ApiResult<Perfil> = notNeeded()

        override suspend fun actualizarPerfil(
            nombre: String?,
            telefono: String?
        ): ApiResult<Perfil> = notNeeded()

        override suspend fun logout() = Unit

        override suspend fun eliminarCuenta(password: String): ApiResult<Unit> = notNeeded()

        private fun notNeeded(): Nothing =
            throw UnsupportedOperationException("Not used by LoginViewModelTest")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `LOGIN_BLOQUEADO avisa que hay que esperar 15 minutos`() {
        val repo = FakeAuthRepository(
            ApiResult.Error(
                AppError.Api(
                    code = ApiCode.LOGIN_BLOQUEADO,
                    rawCode = ApiCode.LOGIN_BLOQUEADO.raw,
                    httpStatus = 429,
                    // A different server message proves the UI ignores it.
                    message = "mensaje del server"
                )
            )
        )
        val viewModel = LoginViewModel(repo)

        viewModel.iniciarSesion("ana@test.com", "password")

        assertEquals(
            "Demasiados intentos. Probá de nuevo en 15 minutos.",
            viewModel.mensajeUI.value
        )
        assertNull(viewModel.loginExitoso.value)
    }
}
