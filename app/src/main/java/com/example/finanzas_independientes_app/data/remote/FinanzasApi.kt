package com.example.finanzas_independientes_app.data.remote

import com.example.finanzas_independientes_app.data.remote.dto.LoginDTO
import com.example.finanzas_independientes_app.data.remote.dto.TransaccionRegistroDTO
import com.example.finanzas_independientes_app.data.remote.dto.UsuarioRegistroDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FinanzasApi {

    @POST("/api/v1/usuarios/registro")
    suspend fun registrarUsuario(@Body usuario: UsuarioRegistroDTO): Response<Unit>

    @POST("/api/v1/usuarios/login")
    suspend fun iniciarSesion(@Body request: LoginDTO): Response<Long>

    @POST("/api/v1/finanzas/transacciones")
    suspend fun registrarTransaccion(@Body transaccion: TransaccionRegistroDTO): Response<Unit>

    @GET("/api/v1/finanzas/cuota-diaria/{usuarioId}")
    suspend fun obtenerCuotaDiaria(
        @Path("usuarioId") usuarioId: Long,
        @Query("meta") meta: Double,
        @Query("dias") dias: Int
    ): Response<Double>
}