package com.example.finanzas_independientes_app.di

import android.content.Context
import android.content.Intent
import com.example.finanzas_independientes_app.core.network.NetworkModule
import com.example.finanzas_independientes_app.core.session.EncryptedSecureStorage
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.data.remote.FinanzasApi
import com.example.finanzas_independientes_app.presentation.auth.LoginActivity
import com.google.gson.Gson

/**
 * Manual composition root. Initialized once from [com.example.finanzas_independientes_app.FinanzasApp].
 * This is the seam Hilt replaces in Fase 3 — ViewModels read collaborators from
 * here, so swapping the DI mechanism later won't touch them.
 */
object ServiceLocator {

    lateinit var gson: Gson
        private set
    lateinit var sessionManager: SessionManager
        private set
    lateinit var api: FinanzasApi
        private set

    fun init(context: Context) {
        val appContext = context.applicationContext
        gson = NetworkModule.provideGson()
        sessionManager = SessionManager(EncryptedSecureStorage(appContext))
        api = NetworkModule.provideApi(
            gson = gson,
            session = sessionManager,
            onSessionExpired = { redirectToLogin(appContext) }
        )
    }

    private fun redirectToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }
}
