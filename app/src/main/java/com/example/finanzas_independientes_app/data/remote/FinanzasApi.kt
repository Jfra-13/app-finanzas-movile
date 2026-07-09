package com.example.finanzas_independientes_app.data.remote

import com.example.finanzas_independientes_app.core.network.ApiResponse
import com.example.finanzas_independientes_app.data.remote.dto.AuthData
import com.example.finanzas_independientes_app.data.remote.dto.CategoriaDTO
import com.example.finanzas_independientes_app.data.remote.dto.CategoriaRequest
import com.example.finanzas_independientes_app.data.remote.dto.ForgotPasswordRequest
import com.example.finanzas_independientes_app.data.remote.dto.LoginDTO
import com.example.finanzas_independientes_app.data.remote.dto.MetaDTO
import com.example.finanzas_independientes_app.data.remote.dto.MetaRequest
import com.example.finanzas_independientes_app.data.remote.dto.PaginatedTransaccionDTO
import com.example.finanzas_independientes_app.data.remote.dto.ProgresoMetasDTO
import com.example.finanzas_independientes_app.data.remote.dto.RefreshRequest
import com.example.finanzas_independientes_app.data.remote.dto.ResetPasswordRequest
import com.example.finanzas_independientes_app.data.remote.dto.ResumenSemanalItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.SaludFinancieraItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.TendenciaMensualDTO
import com.example.finanzas_independientes_app.data.remote.dto.TransaccionRegistroDTO
import com.example.finanzas_independientes_app.data.remote.dto.UpdateNegocioRequest
import com.example.finanzas_independientes_app.data.remote.dto.UsuarioRegistroDTO
import com.example.finanzas_independientes_app.data.remote.dto.VerifyOtpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract aligned with the live backend (see README-FRONTEND.md).
 * Every response is wrapped in the uniform [ApiResponse] envelope; the client
 * decides flow by `code`. Identity comes from the JWT, never from the body.
 */
interface FinanzasApi {

    // -------------------------------------------------------------------------
    // Public (no token)
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Protected — user profile (Bearer token injected by AuthInterceptor)
    // -------------------------------------------------------------------------

    @PUT("api/v1/usuarios/me/negocio")
    suspend fun actualizarNegocio(@Body body: UpdateNegocioRequest): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // Protected — transactions
    // -------------------------------------------------------------------------

    @POST("api/v1/finanzas/transacciones")
    suspend fun registrarTransaccion(@Body body: TransaccionRegistroDTO): Response<ApiResponse<Unit>>

    @GET("api/v1/finanzas/transacciones")
    suspend fun listarTransacciones(
        @Query("tipo") tipo: String? = null,
        @Query("categoriaId") categoriaId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<ApiResponse<PaginatedTransaccionDTO>>

    @PUT("api/v1/finanzas/transacciones/{id}")
    suspend fun actualizarTransaccion(
        @Path("id") id: Long,
        @Body body: TransaccionRegistroDTO
    ): Response<ApiResponse<Unit>>

    @DELETE("api/v1/finanzas/transacciones/{id}")
    suspend fun eliminarTransaccion(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // Protected — daily income & quota
    // -------------------------------------------------------------------------

    @GET("api/v1/finanzas/hoy")
    suspend fun obtenerHoy(): Response<ApiResponse<Double>>

    /** No-param form uses the persisted meta. Optional params override for ad-hoc calculations. */
    @GET("api/v1/finanzas/cuota-diaria")
    suspend fun obtenerCuotaDiaria(
        @Query("meta") meta: Double? = null,
        @Query("dias") dias: Int? = null
    ): Response<ApiResponse<Double>>

    // -------------------------------------------------------------------------
    // Protected — analytics & summaries
    // -------------------------------------------------------------------------

    @GET("api/v1/finanzas/resumen-semanal")
    suspend fun obtenerResumenSemanal(): Response<ApiResponse<List<ResumenSemanalItemDTO>>>

    @GET("api/v1/finanzas/progreso-metas")
    suspend fun obtenerProgresoMetas(): Response<ApiResponse<ProgresoMetasDTO>>

    // -------------------------------------------------------------------------
    // Protected — goals
    // -------------------------------------------------------------------------

    @POST("api/v1/finanzas/metas")
    suspend fun fijarMeta(@Body body: MetaRequest): Response<ApiResponse<MetaDTO>>

    @GET("api/v1/finanzas/metas/actual")
    suspend fun obtenerMetaActual(): Response<ApiResponse<MetaDTO>>

    // -------------------------------------------------------------------------
    // Protected — categories
    // -------------------------------------------------------------------------

    @GET("api/v1/finanzas/categorias")
    suspend fun listarCategorias(): Response<ApiResponse<List<CategoriaDTO>>>

    @POST("api/v1/finanzas/categorias")
    suspend fun crearCategoria(@Body body: CategoriaRequest): Response<ApiResponse<CategoriaDTO>>

    // -------------------------------------------------------------------------
    // Protected — analytics (read-only)
    // -------------------------------------------------------------------------

    @GET("api/v1/finanzas/resumen-categorias")
    suspend fun obtenerResumenCategorias(): Response<ApiResponse<Map<String, Double>>>

    @GET("api/v1/finanzas/tendencia-mensual")
    suspend fun obtenerTendenciaMensual(
        @Query("meses") meses: Int? = null
    ): Response<ApiResponse<TendenciaMensualDTO>>

    @GET("api/v1/finanzas/salud-financiera")
    suspend fun obtenerSaludFinanciera(): Response<ApiResponse<List<SaludFinancieraItemDTO>>>

    // -------------------------------------------------------------------------
    // Fase 2 — EN DESARROLLO (bloqueado por backend)
    //
    // The analytics screen exposes weekly granularity ("Semana") and a 1M window,
    // plus a weekday-earnings breakdown over the selected period. None of these
    // can be served today: `tendencia-mensual` only buckets by month, and
    // `resumen-semanal` only covers the current week. The UI ships these controls
    // disabled until the backend adds:
    //
    //   GET api/v1/finanzas/tendencia?granularidad=SEMANA|MES&ventana=N
    //       -> same shape as TendenciaMensualDTO but bucketed weekly, enabling
    //          the "Semana" toggle and the 1M window.
    //   GET api/v1/finanzas/ingresos-por-dia-semana?ventana=N
    //       -> income aggregated by weekday over the selected window (not just the
    //          current week), for the "¿Qué día ganás más?" chart.
    //
    // When these land: add the endpoints here, DTOs + mappers, repository methods,
    // then enable btnGranSemana / btnPeriodo1M and wire the window param.
    // -------------------------------------------------------------------------
}
