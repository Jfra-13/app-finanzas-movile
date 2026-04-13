package com.example.finanzas_independientes_app.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.LoginActivity
import com.example.finanzas_independientes_app.R

class OnboardingTwoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_two)

        val btnStart = findViewById<Button>(R.id.btnStart)

        btnStart.setOnClickListener {
            // Guardamos en memoria que ya vio el Onboarding
            val sharedPref = getSharedPreferences("MisFinanzasApp", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("VIO_ONBOARDING", true).apply()

            // Lo mandamos al Login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }
}