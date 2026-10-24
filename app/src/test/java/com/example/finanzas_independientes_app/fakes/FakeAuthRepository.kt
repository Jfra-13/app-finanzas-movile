package com.example.finanzas_independientes_app.fakes

import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.core.network.FieldError
import com.example.finanzas_independientes_app.domain.model.AuthSession
import com.example.finanzas_independientes_app.domain.model.Perfil
import com.example.finanzas_independientes_app.domain.repository.AuthRepository

/** Builds an API error result; the server message is arbitrary on purpose to prove the UI ignores it. */
fun apiError(
    code: ApiCode,
    fields: List<FieldError> = emptyList(),
    message: String = "mensaje del server"
): ApiResult.Error = ApiResult.Error(
    AppError.Api(
        code = code,
        rawCode = code.raw,
        httpStatus = 400,
        message = message,
        fieldErrors = fields
    )
)

/**
 * Configurable [AuthRepository] for ViewModel tests. Set the `*Result` vars to
 * choose what each call returns; calls and arguments are recorded.
 * `login` and `actualizarPerfil` throw unless their result is set.
 */
class FakeAuthRepository : AuthRepository {

    var loginResult: ApiResult<AuthSession>? = null
    var registroResult: ApiResult<Unit> = ApiResult.Success(Unit, ApiCode.USER_REGISTERED)
    var forgotPasswordResult: ApiResult<Unit> = ApiResult.Success(Unit, ApiCode.OTP_SENT)
    var verifyOtpResult: ApiResult<Unit> = ApiResult.Success(Unit, ApiCode.OTP_VERIFIED)
    var resetPasswordResult: ApiResult<Unit> = ApiResult.Success(Unit, ApiCode.PASSWORD_RESET_SUCCESS)
    var actualizarNegocioResult: ApiResult<Unit> = ApiResult.Success(Unit, ApiCode.BUSINESS_UPDATED)
    var obtenerPerfilResult: ApiResult<Perfil> = ApiResult.Success(
        Perfil(1, "Ana", "ana@test.com", null, null, "SERVICIOS", null),
        ApiCode.PROFILE_OK
    )
    var actualizarPerfilResult: ApiResult<Perfil>? = null
    var eliminarCuentaResult: ApiResult<Unit> = ApiResult.Success(Unit, ApiCode.ACCOUNT_DELETED)

    val loginCalls = mutableListOf<Pair<String, String>>()
    val registroCalls = mutableListOf<List<String?>>()
    val forgotPasswordCalls = mutableListOf<String>()
    val verifyOtpCalls = mutableListOf<Pair<String, String>>()
    val resetPasswordCalls = mutableListOf<Triple<String, String, String>>()
    val actualizarPerfilCalls = mutableListOf<Pair<String?, String?>>()
    val actualizarNegocioCalls = mutableListOf<String>()
    val eliminarCuentaCalls = mutableListOf<String>()
    var obtenerPerfilCalls = 0
    var logoutCalls = 0

    override suspend fun login(email: String, password: String): ApiResult<AuthSession> {
        loginCalls += email to password
        return loginResult ?: throw UnsupportedOperationException("Set loginResult first")
    }

    override suspend fun registro(
        nombre: String,
        email: String,
        password: String,
        tipoNegocio: String?
    ): ApiResult<Unit> {
        registroCalls += listOf(nombre, email, password, tipoNegocio)
        return registroResult
    }

    override suspend fun forgotPassword(email: String): ApiResult<Unit> {
        forgotPasswordCalls += email
        return forgotPasswordResult
    }

    override suspend fun verifyOtp(email: String, otp: String): ApiResult<Unit> {
        verifyOtpCalls += email to otp
        return verifyOtpResult
    }

    override suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String
    ): ApiResult<Unit> {
        resetPasswordCalls += Triple(email, otp, newPassword)
        return resetPasswordResult
    }

    override suspend fun actualizarNegocio(tipoNegocio: String): ApiResult<Unit> {
        actualizarNegocioCalls += tipoNegocio
        return actualizarNegocioResult
    }

    override suspend fun obtenerPerfil(): ApiResult<Perfil> {
        obtenerPerfilCalls++
        return obtenerPerfilResult
    }

    override suspend fun actualizarPerfil(nombre: String?, telefono: String?): ApiResult<Perfil> {
        actualizarPerfilCalls += nombre to telefono
        return actualizarPerfilResult
            ?: throw UnsupportedOperationException("Set actualizarPerfilResult first")
    }

    override suspend fun logout() {
        logoutCalls++
    }

    override suspend fun eliminarCuenta(password: String): ApiResult<Unit> {
        eliminarCuentaCalls += password
        return eliminarCuentaResult
    }
}
