package com.example.finanzas_independientes_app.core.network

import com.example.finanzas_independientes_app.BuildConfig
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.data.remote.FinanzasApi
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the network stack. Wires the AuthInterceptor + TokenAuthenticator into
 * the main client, and keeps a separate bare client for token refresh so a 401
 * during refresh can never recurse. BASE_URL comes from BuildConfig per variant.
 */
object NetworkModule {

    private const val TIMEOUT_SECONDS = 30L

    fun provideGson(): Gson = Gson()

    fun provideApi(
        gson: Gson,
        session: SessionManager,
        onSessionExpired: () -> Unit
    ): FinanzasApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Bare client for refresh: no auth, no authenticator -> no recursion.
        val refreshClient = baseClientBuilder(logging).build()
        val refreshApi = retrofit(refreshClient, gson).create(RefreshApi::class.java)
        val authenticator = TokenAuthenticator(session, refreshApi, onSessionExpired)

        val mainClient = baseClientBuilder(logging)
            .addInterceptor(AuthInterceptor(session))
            .authenticator(authenticator)
            .build()

        return retrofit(mainClient, gson).create(FinanzasApi::class.java)
    }

    private fun baseClientBuilder(logging: HttpLoggingInterceptor): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)

    private fun retrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
}
