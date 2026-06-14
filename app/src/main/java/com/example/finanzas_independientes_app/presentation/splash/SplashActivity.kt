package com.example.finanzas_independientes_app.presentation.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.core.session.SessionManager
import com.example.finanzas_independientes_app.databinding.ActivitySplashBinding
import com.example.finanzas_independientes_app.presentation.auth.LoginActivity
import com.example.finanzas_independientes_app.presentation.business.SelectBusinessActivity
import com.example.finanzas_independientes_app.presentation.dashboard.DashboardActivity
import com.example.finanzas_independientes_app.presentation.onboarding.OnboardingOneActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var sessionManager: SessionManager

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPref = getSharedPreferences("MisFinanzasApp", Context.MODE_PRIVATE)
            val vioOnboarding = sharedPref.getBoolean("VIO_ONBOARDING", false)

            val intent = if (sessionManager.isLoggedIn()) {
                if (sessionManager.tipoNegocio != null) {
                    Intent(this, DashboardActivity::class.java)
                } else {
                    Intent(this, SelectBusinessActivity::class.java)
                }
            } else if (vioOnboarding) {
                Intent(this, LoginActivity::class.java)
            } else {
                Intent(this, OnboardingOneActivity::class.java)
            }

            startActivity(intent)
            finish()
        }, 2000)
    }
}
