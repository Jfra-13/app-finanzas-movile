package com.example.finanzas_independientes_app.presentation.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.presentation.auth.LoginActivity
import com.example.finanzas_independientes_app.presentation.business.SelectBusinessActivity
import com.example.finanzas_independientes_app.presentation.dashboard.DashboardActivity
import com.example.finanzas_independientes_app.presentation.onboarding.OnboardingOneActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Routing-only entry point. Uses the Splash Screen API for a fast, themed
 * cold start: the system shows the splash, routing is resolved synchronously
 * (no artificial delay, no main-thread work), and we hand off immediately.
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        startActivity(resolveDestination())
        finish()
    }

    private fun resolveDestination(): Intent {
        val sharedPref = getSharedPreferences("MisFinanzasApp", Context.MODE_PRIVATE)
        val vioOnboarding = sharedPref.getBoolean("VIO_ONBOARDING", false)

        return when {
            sessionManager.isLoggedIn() && sessionManager.tipoNegocio != null ->
                Intent(this, DashboardActivity::class.java)
            sessionManager.isLoggedIn() ->
                Intent(this, SelectBusinessActivity::class.java)
            vioOnboarding ->
                Intent(this, LoginActivity::class.java)
            else ->
                Intent(this, OnboardingOneActivity::class.java)
        }
    }
}
