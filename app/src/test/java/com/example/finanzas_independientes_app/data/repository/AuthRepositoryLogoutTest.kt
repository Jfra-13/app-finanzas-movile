package com.example.finanzas_independientes_app.data.repository

import com.example.finanzas_independientes_app.core.session.SecureStorage
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.data.remote.FinanzasApi
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Logout must drop the local session even when the coroutine is cancelled
 * while the best-effort revocation is in flight (the user leaves the screen).
 */
class AuthRepositoryLogoutTest {

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
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val gson = Gson()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(FinanzasApi::class.java)
        session = SessionManager(FakeSecureStorage())
        session.updateTokens("acceso", "refresco")
        repository = AuthRepositoryImpl(api, gson, session)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `logout cancelado durante la revocacion igual cierra la sesion local`() = runTest {
        // The server never answers, so logout stays suspended on the revocation call.
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val job = launch { repository.logout() }
        val request = withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) }
        assertNotNull("logout never reached the server", request)
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertFalse(session.isLoggedIn())
        assertNull(session.refreshToken)
    }
}
