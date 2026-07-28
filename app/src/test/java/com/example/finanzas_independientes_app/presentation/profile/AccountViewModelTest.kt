package com.example.finanzas_independientes_app.presentation.profile

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.core.network.FieldError
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Account deletion is the one flow where a wrong password must NOT end the
 * session: mistyping it should show an inline field error, not throw the user
 * back to Login. These tests pin that distinction.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private class FakeAuthRepository(
        private val deleteResult: ApiResult<Unit> = ApiResult.Success(Unit, ApiCode.ACCOUNT_DELETED)
    ) : AuthRepository {

        var deleteCalls = 0
            private set
        var lastPassword: String? = null
            private set

        override suspend fun eliminarCuenta(password: String): ApiResult<Unit> {
            deleteCalls++
            lastPassword = password
            return deleteResult
        }

        override suspend fun obtenerPerfil(): ApiResult<Perfil> = ApiResult.Success(
            Perfil(1, "Ana", "ana@test.com", null, null, "SERVICIOS", null),
            ApiCode.PROFILE_OK
        )

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
            notNeeded()

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

        override suspend fun actualizarPerfil(
            nombre: String?,
            telefono: String?
        ): ApiResult<Perfil> = notNeeded()

        override suspend fun logout() = Unit

        private fun notNeeded(): Nothing =
            throw UnsupportedOperationException("Not used by AccountViewModelTest")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun apiError(code: ApiCode, fields: List<FieldError> = emptyList()) =
        ApiResult.Error(
            AppError.Api(
                code = code,
                rawCode = code.raw,
                httpStatus = 401,
                message = "mensaje del server",
                fieldErrors = fields
            )
        )

    @Test
    fun `baja exitosa cierra la sesion local`() {
        val repo = FakeAuthRepository()
        val viewModel = AccountViewModel(repo)

        viewModel.eliminarCuenta("mi-password")

        assertTrue(viewModel.logoutDone.value)
        assertNull(viewModel.deleteAccountError.value)
        assertEquals("mi-password", repo.lastPassword)
    }

    @Test
    fun `password incorrecta muestra error inline y NO cierra la sesion`() {
        val repo = FakeAuthRepository(apiError(ApiCode.CREDENCIALES_INVALIDAS))
        val viewModel = AccountViewModel(repo)

        viewModel.eliminarCuenta("equivocada")

        assertEquals("Contraseña incorrecta.", viewModel.deleteAccountError.value)
        // The whole point: a typo must not expel the user from the app.
        assertFalse(viewModel.logoutDone.value)
    }

    @Test
    fun `password vacia no llega a la red`() {
        val repo = FakeAuthRepository()
        val viewModel = AccountViewModel(repo)

        viewModel.eliminarCuenta("   ")

        assertEquals("Ingresá tu contraseña.", viewModel.deleteAccountError.value)
        assertEquals(0, repo.deleteCalls)
        assertFalse(viewModel.logoutDone.value)
    }

    @Test
    fun `VALIDATION_ERROR prefiere el mensaje del campo`() {
        val repo = FakeAuthRepository(
            apiError(ApiCode.VALIDATION_ERROR, listOf(FieldError("password", "Campo requerido")))
        )
        val viewModel = AccountViewModel(repo)

        viewModel.eliminarCuenta("x")

        assertEquals("Campo requerido", viewModel.deleteAccountError.value)
        assertFalse(viewModel.logoutDone.value)
    }

    @Test
    fun `un fallo de red se muestra inline sin cerrar la sesion`() {
        val repo = FakeAuthRepository(ApiResult.Error(AppError.Network))
        val viewModel = AccountViewModel(repo)

        viewModel.eliminarCuenta("mi-password")

        assertFalse(viewModel.logoutDone.value)
        assertTrue(viewModel.deleteAccountError.value!!.isNotBlank())
    }
}
