package com.example.finanzas_independientes_app.core.network

import com.example.finanzas_independientes_app.data.remote.dto.AuthData
import com.example.finanzas_independientes_app.data.remote.dto.RefreshRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Minimal, synchronous refresh contract used exclusively by
 * [TokenAuthenticator]. Lives on a bare OkHttp client (no AuthInterceptor, no
 * Authenticator) to avoid recursive 401 handling.
 */
interface RefreshApi {

    @POST("api/v1/usuarios/refresh")
    fun refresh(@Body body: RefreshRequest): Call<ApiResponse<AuthData>>
}
