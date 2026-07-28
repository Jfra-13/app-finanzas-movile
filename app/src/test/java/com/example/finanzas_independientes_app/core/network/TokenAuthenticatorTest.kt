package com.example.finanzas_independientes_app.core.network

import com.example.finanzas_independientes_app.core.session.SecureStorage
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * The refresh flow is the riskiest logic in the client: it runs off the main
 * thread, mutates the session, and decides whether the user stays logged in.
 * These tests pin the four outcomes that matter — rotate and retry, give up and
 * expire, reuse a token another thread already refreshed, and stop retrying.
 */
class TokenAuthenticatorTest {

    /** In-memory [SecureStorage]; the real one needs the Android Keystore. */
    private class FakeSecureStorage : SecureStorage {
        private val values = mutableMapOf<String, Any>()
        override fun putString(key: String, value: String) { values[key] = value }
        override fun getString(key: String): String? = values[key] as? String
        override fun putLong(key: String, value: Long) { values[key] = value }
        override fun getLong(key: String): Long? = values[key] as? Long
        override fun remove(key: String) { values.remove(key) }
        override fun clear() = values.clear()
    }

    private lateinit var server: MockWebServer
    private lateinit var session: SessionManager
    private lateinit var refreshApi: RefreshApi
    private var sessionExpired = false

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        session = SessionManager(FakeSecureStorage())
        sessionExpired = false
        refreshApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(RefreshApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun authenticator() =
        TokenAuthenticator(session, refreshApi) { sessionExpired = true }

    /** A 401 on a protected route, carrying the access token that was rejected. */
    private fun unauthorized(bearer: String?, prior: Response? = null): Response =
        Response.Builder()
            .request(
                Request.Builder()
                    .url(server.url("/api/v1/transacciones"))
                    .apply { bearer?.let { header("Authorization", "Bearer $it") } }
                    .build()
            )
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .apply { prior?.let { priorResponse(it) } }
            .build()

    private fun enqueueRefreshOk(token: String, refreshToken: String) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "status": 200,
                  "code": "TOKEN_REFRESHED",
                  "data": {
                    "token": "$token",
                    "refreshToken": "$refreshToken",
                    "usuarioId": 1,
                    "nombre": "Ana",
                    "email": "ana@test.com",
                    "tipoNegocio": "SERVICIOS"
                  }
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `401 con refresh valido reintenta con el token nuevo y persiste la rotacion`() {
        session.updateTokens(token = "viejo", refreshToken = "refresh-1")
        enqueueRefreshOk(token = "nuevo", refreshToken = "refresh-2")

        val retry = authenticator().authenticate(null, unauthorized("viejo"))

        assertEquals("Bearer nuevo", retry?.header("Authorization"))
        assertEquals("nuevo", session.accessToken)
        // The refresh token rotates too; keeping the old one would break the next refresh.
        assertEquals("refresh-2", session.refreshToken)
        assertFalse(sessionExpired)
    }

    @Test
    fun `refresh rechazado limpia la sesion y no reintenta`() {
        session.updateTokens(token = "viejo", refreshToken = "refresh-muerto")
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"status":401,"code":"REFRESH_TOKEN_INVALIDO"}""")
        )

        val retry = authenticator().authenticate(null, unauthorized("viejo"))

        assertNull(retry)
        assertNull(session.accessToken)
        assertTrue(sessionExpired)
    }

    @Test
    fun `sin refresh token se da la sesion por vencida sin llamar al server`() {
        // Empty session: nothing left to exchange, so the flow must expire it
        // instead of firing a doomed refresh request.

        val retry = authenticator().authenticate(null, unauthorized("viejo"))

        assertNull(retry)
        assertTrue(sessionExpired)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `si otro hilo ya refresco, se reusa ese token sin pedir otro refresh`() {
        // The stored token already differs from the one the failed request carried.
        session.updateTokens(token = "ya-refrescado", refreshToken = "refresh-2")

        val retry = authenticator().authenticate(null, unauthorized("viejo"))

        assertEquals("Bearer ya-refrescado", retry?.header("Authorization"))
        assertEquals(0, server.requestCount)
        assertFalse(sessionExpired)
    }

    @Test
    fun `tras el reintento permitido deja de insistir`() {
        session.updateTokens(token = "viejo", refreshToken = "refresh-1")
        val alreadyRetried = unauthorized("viejo", prior = unauthorized("mas-viejo"))

        val retry = authenticator().authenticate(null, alreadyRetried)

        assertNull(retry)
        assertEquals(0, server.requestCount)
    }
}
