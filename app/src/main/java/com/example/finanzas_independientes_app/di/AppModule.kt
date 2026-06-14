package com.example.finanzas_independientes_app.di

import android.content.Context
import android.content.Intent
import com.example.finanzas_independientes_app.core.network.NetworkModule
import com.example.finanzas_independientes_app.core.session.EncryptedSecureStorage
import com.example.finanzas_independientes_app.core.session.SecureStorage
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.data.remote.FinanzasApi
import com.example.finanzas_independientes_app.presentation.auth.LoginActivity
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-wide bindings. Replaces the manual ServiceLocator: the network
 * stack, secure storage and session are provided as process singletons. The
 * session-expired callback routes back to login from the application context.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = NetworkModule.provideGson()

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage =
        EncryptedSecureStorage(context)

    @Provides
    @Singleton
    fun provideSessionManager(storage: SecureStorage): SessionManager =
        SessionManager(storage)

    @Provides
    @Singleton
    fun provideApi(
        gson: Gson,
        session: SessionManager,
        @ApplicationContext context: Context
    ): FinanzasApi = NetworkModule.provideApi(
        gson = gson,
        session = session,
        onSessionExpired = { redirectToLogin(context) }
    )

    private fun redirectToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }
}
