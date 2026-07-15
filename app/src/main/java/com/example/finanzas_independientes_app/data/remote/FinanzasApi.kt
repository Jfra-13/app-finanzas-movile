package com.example.finanzas_independientes_app.data.remote

import com.example.finanzas_independientes_app.core.network.ApiResponse
import com.example.finanzas_independientes_app.data.remote.dto.AuthData
import com.example.finanzas_independientes_app.data.remote.dto.CategoriaDTO
import com.example.finanzas_independientes_app.data.remote.dto.CategoriaRequest
import com.example.finanzas_independientes_app.data.remote.dto.DeleteAccountRequest
import com.example.finanzas_independientes_app.data.remote.dto.ForgotPasswordRequest
import com.example.finanzas_independientes_app.data.remote.dto.IngresoDiaSemanaItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.LoginDTO
import com.example.finanzas_independientes_app.data.remote.dto.MetaDTO
import com.example.finanzas_independientes_app.data.remote.dto.MetaHistorialItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.MetaRequest
import com.example.finanzas_independientes_app.data.remote.dto.PaginatedTransaccionDTO
import com.example.finanzas_independientes_app.data.remote.dto.ComparacionCategoriasDTO
import com.example.finanzas_independientes_app.data.remote.dto.PerfilDTO
import com.example.finanzas_independientes_app.data.remote.dto.PresupuestoDTO
import com.example.finanzas_independientes_app.data.remote.dto.ProgresoMetasDTO
import com.example.finanzas_independientes_app.data.remote.dto.ProyeccionMensualDTO
import com.example.finanzas_independientes_app.data.remote.dto.SetPresupuestoRequest
import com.example.finanzas_independientes_app.data.remote.dto.RefreshRequest
import com.example.finanzas_independientes_app.data.remote.dto.UpdateCategoriaRequest
import com.example.finanzas_independientes_app.data.remote.dto.UpdatePerfilRequest
import com.example.finanzas_independientes_app.data.remote.dto.ResetPasswordRequest
import com.example.finanzas_independientes_app.data.remote.dto.ResumenDiarioItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.ResumenSemanalItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.SaludFinancieraItemDTO
import com.example.finanzas_independientes_app.data.remote.dto.TendenciaDTO
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

    /**
     * Revokes the refresh token server-side. Public: owning the refresh token IS
     * the credential (works with an expired access token). Idempotent — unknown
     * or already-revoked tokens still return 200 LOGGED_OUT.
     */
    @POST("api/v1/usuarios/logout")
    suspend fun logout(@Body body: RefreshRequest): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // Protected — user profile (Bearer token injected by AuthInterceptor)
    // -------------------------------------------------------------------------

    @GET("api/v1/usuarios/me")
    suspend fun obtenerPerfil(): Response<ApiResponse<PerfilDTO>>

    /** Partial update: only fields present in the body change. Returns the updated profile. */
    @PUT("api/v1/usuarios/me")
    suspend fun actualizarPerfil(@Body body: UpdatePerfilRequest): Response<ApiResponse<PerfilDTO>>

    @PUT("api/v1/usuarios/me/negocio")
    suspend fun actualizarNegocio(@Body body: UpdateNegocioRequest): Response<ApiResponse<Unit>>

    /**
     * Soft-deletes the account with a 30-day grace window. The password in the
     * body re-confirms identity. 200 ACCOUNT_DELETED on success (server revokes
     * all tokens and rejects the access token immediately); 401
     * CREDENCIALES_INVALIDAS on a wrong password; VALIDATION_ERROR if absent.
     * Logging in within the grace window reactivates the account.
     */
    @POST("api/v1/usuarios/me/eliminar")
    suspend fun eliminarCuenta(@Body body: DeleteAccountRequest): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // Protected — transactions
    // -------------------------------------------------------------------------

    @POST("api/v1/finanzas/transacciones")
    suspend fun registrarTransaccion(@Body body: TransaccionRegistroDTO): Response<ApiResponse<Unit>>

    /**
     * Optional `desde`/`hasta` (YYYY-MM-DD, inclusive) filter by date range.
     * `sinCategoria=true` returns only uncategorized transactions; it is mutually
     * exclusive with `categoriaId` (sending both yields 400 PARAMETRO_INVALIDO).
     */
    @GET("api/v1/finanzas/transacciones")
    suspend fun listarTransacciones(
        @Query("tipo") tipo: String? = null,
        @Query("categoriaId") categoriaId: Long? = null,
        @Query("sinCategoria") sinCategoria: Boolean? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
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

    /**
     * Per-day totals for a month (YYYY-MM; omitted = current month). Only days
     * with activity are returned, ascending by date.
     */
    @GET("api/v1/finanzas/resumen-diario")
    suspend fun obtenerResumenDiario(
        @Query("mes") mes: String? = null
    ): Response<ApiResponse<List<ResumenDiarioItemDTO>>>

    @GET("api/v1/finanzas/progreso-metas")
    suspend fun obtenerProgresoMetas(): Response<ApiResponse<ProgresoMetasDTO>>

    // -------------------------------------------------------------------------
    // Protected — goals
    // -------------------------------------------------------------------------

    @POST("api/v1/finanzas/metas")
    suspend fun fijarMeta(@Body body: MetaRequest): Response<ApiResponse<MetaDTO>>

    @GET("api/v1/finanzas/metas/actual")
    suspend fun obtenerMetaActual(): Response<ApiResponse<MetaDTO>>

    /**
     * Past goal periods, most recent window first. `meses` = how many periods
     * back (server default 6). Response code GOALS_HISTORY_OK.
     */
    @GET("api/v1/finanzas/metas/historial")
    suspend fun obtenerHistorialMetas(
        @Query("meses") meses: Int? = null
    ): Response<ApiResponse<List<MetaHistorialItemDTO>>>

    // -------------------------------------------------------------------------
    // Protected — categories
    // -------------------------------------------------------------------------

    @GET("api/v1/finanzas/categorias")
    suspend fun listarCategorias(): Response<ApiResponse<List<CategoriaDTO>>>

    @POST("api/v1/finanzas/categorias")
    suspend fun crearCategoria(@Body body: CategoriaRequest): Response<ApiResponse<CategoriaDTO>>

    /** Renames a category. `tipo` is immutable server-side (would corrupt historical analytics). */
    @PUT("api/v1/finanzas/categorias/{id}")
    suspend fun actualizarCategoria(
        @Path("id") id: Long,
        @Body body: UpdateCategoriaRequest
    ): Response<ApiResponse<Unit>>

    /** Deletes a category. Its transactions survive as "uncategorized"; its budgets are removed. */
    @DELETE("api/v1/finanzas/categorias/{id}")
    suspend fun eliminarCategoria(@Path("id") id: Long): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // Protected — analytics (read-only)
    // -------------------------------------------------------------------------

    /** Optional `desde`/`hasta` (YYYY-MM-DD, inclusive); without them = current month. */
    @GET("api/v1/finanzas/resumen-categorias")
    suspend fun obtenerResumenCategorias(
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null
    ): Response<ApiResponse<Map<String, Double>>>

    /**
     * Trend bucketed by week or month over a rolling window (`ventana` periods,
     * default 6, min 1). Parallel arrays, oldest first; MES labels `yyyy-MM`,
     * SEMANA labels the week's Monday `yyyy-MM-dd`.
     */
    @GET("api/v1/finanzas/tendencia")
    suspend fun obtenerTendencia(
        @Query("granularidad") granularidad: String,
        @Query("ventana") ventana: Int
    ): Response<ApiResponse<TendenciaDTO>>

    /**
     * Income aggregated by weekday over the last `ventana` weeks (default 4,
     * including the current one). Always 7 items, Monday first.
     */
    @GET("api/v1/finanzas/ingresos-por-dia-semana")
    suspend fun obtenerIngresosPorDiaSemana(
        @Query("ventana") ventana: Int? = null
    ): Response<ApiResponse<List<IngresoDiaSemanaItemDTO>>>

    @GET("api/v1/finanzas/salud-financiera")
    suspend fun obtenerSaludFinanciera(): Response<ApiResponse<List<SaludFinancieraItemDTO>>>

    // -------------------------------------------------------------------------
    // Protected — budgets, comparison & projection
    // -------------------------------------------------------------------------

    /** Upsert a category budget: re-sending the same categoriaId replaces the cap. */
    @POST("api/v1/finanzas/presupuestos")
    suspend fun guardarPresupuesto(
        @Body body: SetPresupuestoRequest
    ): Response<ApiResponse<PresupuestoDTO>>

    /** All budgets with current-month consumption. */
    @GET("api/v1/finanzas/presupuestos")
    suspend fun listarPresupuestos(): Response<ApiResponse<List<PresupuestoDTO>>>

    @DELETE("api/v1/finanzas/presupuestos/{id}")
    suspend fun eliminarPresupuesto(@Path("id") id: Long): Response<ApiResponse<Unit>>

    /**
     * Spend per category, current period vs a comparison window. Optional
     * `desde`/`hasta` (YYYY-MM-DD, inclusive; default = current month to date);
     * `compararCon` is PERIODO_ANTERIOR (default) or MISMO_PERIODO_ANIO_ANTERIOR.
     */
    @GET("api/v1/finanzas/analiticas/comparacion-categorias")
    suspend fun obtenerComparacionCategorias(
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
        @Query("compararCon") compararCon: String? = null
    ): Response<ApiResponse<ComparacionCategoriasDTO>>

    /** Linear run-rate projection for the current month. 404 META_NO_ENCONTRADA if no active goal. */
    @GET("api/v1/finanzas/proyeccion-mensual")
    suspend fun obtenerProyeccionMensual(): Response<ApiResponse<ProyeccionMensualDTO>>
}
