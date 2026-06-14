package com.example.finanzas_independientes_app.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.databinding.ActivityOnboardingTwoBinding
import com.example.finanzas_independientes_app.presentation.auth.LoginActivity

class OnboardingTwoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingTwoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingTwoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
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