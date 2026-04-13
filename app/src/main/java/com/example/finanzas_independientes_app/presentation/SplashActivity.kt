package com.example.finanzas_independientes_app.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.DashboardActivity
import com.example.finanzas_independientes_app.LoginActivity
import com.example.finanzas_independientes_app.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Esperamos 2 segundos y decidimos a dónde ir
        Handler(Looper.getMainLooper()).postDelayed({

            // Abrimos la memoria del celular
            val sharedPref = getSharedPreferences("MisFinanzasApp", Context.MODE_PRIVATE)
            val usuarioId = sharedPref.getLong("USUARIO_ID", -1L)
            val vioOnboarding = sharedPref.getBoolean("VIO_ONBOARDING", false)
            val eligioNegocio = sharedPref.getBoolean("ELIGIO_NEGOCIO", false) // NUEVO

            val intent = if (usuarioId != -1L) {
                // CASO 1: Está logueado. ¿Tiene negocio?
                if (eligioNegocio) {
                    Intent(this, DashboardActivity::class.java)
                } else {
                    Intent(this, SelectBusinessActivity::class.java)
                }
            } else if (vioOnboarding) {
                // CASO 2: Ya vio la presentación pero no está logueado -> Al Login
                Intent(this, LoginActivity::class.java)
            } else {
                // CASO 3: App recién instalada -> Al Onboarding
                Intent(this, OnboardingOneActivity::class.java)
            }

            startActivity(intent)
            finish() // Destruimos el Splash para que no vuelva con el botón atrás

        }, 2000)
    }
}