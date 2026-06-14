package com.example.finanzas_independientes_app.data.remote

import com.example.finanzas_independientes_app.core.network.ApiResponse
import com.example.finanzas_independientes_app.data.remote.dto.AuthData
import com.example.finanzas_independientes_app.data.remote.dto.ForgotPasswordRequest
import com.example.finanzas_independientes_app.data.remote.dto.LoginDTO
import com.example.finanzas_independientes_app.data.remote.dto.RefreshRequest
import com.example.finanzas_independientes_app.data.remote.dto.ResetPasswordRequest
import com.example.finanzas_independientes_app.data.remote.dto.TransaccionRegistroDTO
import com.example.finanzas_independientes_app.data.remote.dto.UpdateNegocioRequest
import com.example.finanzas_independientes_app.data.remote.dto.UsuarioRegistroDTO
import com.example.finanzas_independientes_app.data.remote.dto.VerifyOtpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * Retrofit contract aligned with the live backend (see README-FRONTEND.md).
 * Every response is wrapped in the uniform [ApiResponse] envelope; the client
 * decides flow by `code`. Identity comes from the JWT, never from the body.
 */
interface FinanzasApi {

    // --- Public (no token) ---

    @POST("api/v1/usuarios/registro")
    suspend fun registrar(@Body body: UsuarioRegistroDTO): Response<ApiResponse<Unit>>

    @POST("api/v1/usuarios/login")
    suspend fun login(@Body body: LoginDTO): Response<ApiResponse<AuthData>>

    @POST("api/v1/usuarios/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<ApiResponse<AuthData>>

    @POST("api/v1/usuarios/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<ApiResponse<Unit>>

    @POST("api/v1/usuarios/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): Response<ApiResponse<Unit>>

    @POST("api/v1/usuarios/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<ApiResponse<Unit>>

    // --- Protected (Bearer token, injected by AuthInterceptor) ---

    @PUT("api/v1/usuarios/me/negocio")
    suspend fun actualizarNegocio(@Body body: UpdateNegocioRequest): Response<ApiResponse<Unit>>

    @POST("api/v1/finanzas/transacciones")
    suspend fun registrarTransaccion(@Body body: TransaccionRegistroDTO): Response<ApiResponse<Unit>>

    @GET("api/v1/finanzas/cuota-diaria")
    suspend fun obtenerCuotaDiaria(
        @Query("meta") meta: Double? = null,
        @Query("dias") dias: Int? = null
    ): Response<ApiResponse<Double>>
}
