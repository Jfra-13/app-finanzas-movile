package com.example.finanzas_independientes_app.core.network

import com.example.finanzas_independientes_app.data.remote.dto.AuthData
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 * Covers the single funnel every API call goes through. Uses the real Retrofit
 * and Gson stack against MockWebServer instead of mocking the response, so a
 * broken envelope shape or a renamed DTO field fails here rather than at
 * runtime on a screen.
 */
class SafeApiCallTest {

    private interface TestApi {
        @GET("auth")
        suspend fun auth(): Response<ApiResponse<AuthData>>

        @GET("unit")
        suspend fun unit(): Response<ApiResponse<Unit>>
    }

    private lateinit var server: MockWebServer
    private lateinit var api: TestApi
    private val gson = Gson()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TestApi::class.java)
    }

    @After
    fun tearDown() {
        // The network-failure test shuts the server down itself; shutting down
        // twice must not fail the run.
        runCatching { server.shutdown() }
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    @Test
    fun `envelope exitoso mapea data y code al dominio`() = runTest {
        enqueue(
            200,
            """
            {
              "status": 200,
              "code": "LOGIN_SUCCESS",
              "message": "ok",
              "data": {
                "token": "acceso",
                "refreshToken": "refresco",
                "usuarioId": 7,
                "nombre": "Ana",
                "email": "ana@test.com",
                "tipoNegocio": "SERVICIOS",
                "cuentaReactivada": true
              }
            }
            """.trimIndent()
        )

        val result = safeApiCall(gson) { api.auth() }

        assertTrue(result is ApiResult.Success)
        val success = result as ApiResult.Success
        assertEquals(ApiCode.LOGIN_SUCCESS, success.code)
        assertEquals("acceso", success.data.token)
        assertEquals(7L, success.data.usuarioId)
        assertEquals("SERVICIOS", success.data.tipoNegocio)
        assertTrue(success.data.cuentaReactivada)
    }

    @Test
    fun `un code que el cliente no conoce cae en UNKNOWN sin romper`() = runTest {
        enqueue(
            200,
            """{"status":200,"code":"CODE_QUE_NO_EXISTE_TODAVIA","data":{"token":"a",
               "refreshToken":"b","usuarioId":1,"nombre":"x","email":"y","tipoNegocio":null}}"""
        )

        val result = safeApiCall(gson) { api.auth() }

        assertEquals(ApiCode.UNKNOWN, (result as ApiResult.Success).code)
    }

    @Test
    fun `200 sin data es un error, no un exito vacio`() = runTest {
        enqueue(200, """{"status":200,"code":"LOGIN_SUCCESS","data":null}""")

        val result = safeApiCall(gson) { api.auth() }

        assertTrue((result as ApiResult.Error).error is AppError.Unexpected)
    }

    @Test
    fun `VALIDATION_ERROR expone los errores por campo`() = runTest {
        enqueue(
            400,
            """
            {
              "status": 400,
              "code": "VALIDATION_ERROR",
              "message": "Datos invalidos",
              "details": [
                {"field": "email", "rejectedValue": "no-es-mail", "message": "Email invalido"},
                {"field": "password", "message": "Minimo 8 caracteres"}
              ]
            }
            """.trimIndent()
        )

        val result = safeApiCall(gson) { api.auth() }

        val error = (result as ApiResult.Error).error as AppError.Api
        assertEquals(ApiCode.VALIDATION_ERROR, error.code)
        assertEquals(400, error.httpStatus)
        assertEquals(2, error.fieldErrors.size)
        assertEquals("email", error.fieldErrors[0].field)
        assertEquals("Email invalido", error.fieldErrors[0].message)
        assertEquals("password", error.fieldErrors[1].field)
    }

    @Test
    fun `un detail sin field se descarta en vez de inventar un campo vacio`() = runTest {
        enqueue(
            400,
            """{"status":400,"code":"VALIDATION_ERROR",
               "details":[{"message":"algo global"},{"field":"email","message":"invalido"}]}"""
        )

        val result = safeApiCall(gson) { api.auth() }

        val error = (result as ApiResult.Error).error as AppError.Api
        assertEquals(1, error.fieldErrors.size)
        assertEquals("email", error.fieldErrors[0].field)
    }

    @Test
    fun `error con envelope conserva code y status http`() = runTest {
        enqueue(404, """{"status":404,"code":"META_NO_ENCONTRADA","message":"Sin meta activa"}""")

        val result = safeApiCall(gson) { api.auth() }

        val error = (result as ApiResult.Error).error as AppError.Api
        assertEquals(ApiCode.META_NO_ENCONTRADA, error.code)
        assertEquals("META_NO_ENCONTRADA", error.rawCode)
        assertEquals(404, error.httpStatus)
        assertEquals("Sin meta activa", error.message)
        assertTrue(error.fieldErrors.isEmpty())
    }

    @Test
    fun `un cuerpo de error que no es JSON no tumba el parseo`() = runTest {
        // A proxy or gateway can answer HTML instead of the envelope.
        enqueue(502, "<html><body>Bad Gateway</body></html>")

        val result = safeApiCall(gson) { api.auth() }

        val error = (result as ApiResult.Error).error as AppError.Api
        assertEquals(ApiCode.UNKNOWN, error.code)
        assertEquals(502, error.httpStatus)
        assertNull(error.message)
    }

    @Test
    fun `un fallo de red se traduce a AppError Network`() = runTest {
        server.shutdown()

        val result = safeApiCall(gson) { api.auth() }

        assertEquals(AppError.Network, (result as ApiResult.Error).error)
    }

    @Test
    fun `safeUnitCall acepta data null y devuelve el code de exito`() = runTest {
        enqueue(200, """{"status":200,"code":"LOGGED_OUT","message":"Sesion cerrada","data":null}""")

        val result = safeUnitCall(gson) { api.unit() }

        assertEquals(ApiCode.LOGGED_OUT, (result as ApiResult.Success).code)
    }
}
