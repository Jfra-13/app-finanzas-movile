package com.example.finanzas_independientes_app.data.repository

import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.safeApiCall
import com.example.finanzas_independientes_app.core.network.safeUnitCall
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.data.mapper.toAuthSession
import com.example.finanzas_independientes_app.data.remote.FinanzasApi
import com.example.finanzas_independientes_app.data.remote.dto.ForgotPasswordRequest
import com.example.finanzas_independientes_app.data.remote.dto.LoginDTO
import com.example.finanzas_independientes_app.data.remote.dto.ResetPasswordRequest
import com.example.finanzas_independientes_app.data.remote.dto.UpdateNegocioRequest
import com.example.finanzas_independientes_app.data.remote.dto.UsuarioRegistroDTO
import com.example.finanzas_independientes_app.data.remote.dto.VerifyOtpRequest
import com.example.finanzas_independientes_app.domain.model.AuthSession
import com.example.finanzas_independientes_app.domain.repository.AuthRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: FinanzasApi,
    private val gson: Gson,
    private val session: SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): ApiResult<AuthSession> {
        val result = safeApiCall(gson) { api.login(LoginDTO(email, password)) }
        if (result is ApiResult.Success) {
            session.saveSession(result.data)
            return ApiResult.Success(result.data.toAuthSession(), result.code)
        }
        @Suppress("UNCHECKED_CAST")
        return result as ApiResult<AuthSession>
    }

    override suspend fun registro(
        nombre: String,
        email: String,
        password: String,
        tipoNegocio: String?
    ): ApiResult<Unit> = safeUnitCall(gson) {
        api.registrar(UsuarioRegistroDTO(nombre, email, password, tipoNegocio))
    }

    override suspend fun forgotPassword(email: String): ApiResult<Unit> =
        safeUnitCall(gson) { api.forgotPassword(ForgotPasswordRequest(email)) }

    override suspend fun verifyOtp(email: String, otp: String): ApiResult<Unit> =
        safeUnitCall(gson) { api.verifyOtp(VerifyOtpRequest(email, otp)) }

    override suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String
    ): ApiResult<Unit> = safeUnitCall(gson) {
        api.resetPassword(ResetPasswordRequest(email, otp, newPassword))
    }

    override suspend fun actualizarNegocio(tipoNegocio: String): ApiResult<Unit> {
        val result = safeUnitCall(gson) {
            api.actualizarNegocio(UpdateNegocioRequest(tipoNegocio))
        }
        if (result is ApiResult.Success) {
            session.updateTipoNegocio(tipoNegocio)
        }
        return result
    }
}
