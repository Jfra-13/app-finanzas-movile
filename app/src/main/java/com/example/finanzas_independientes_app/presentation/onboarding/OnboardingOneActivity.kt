package com.example.finanzas_independientes_app.presentation.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.databinding.ActivityOnboardingOneBinding
import com.example.finanzas_independientes_app.presentation.auth.LoginActivity

class OnboardingOneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingOneBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingOneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnNext.setOnClickListener {
            startActivity(Intent(this, OnboardingTwoActivity::class.java))
        }

        binding.tvSkip.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity() // Limpia la pila para ir directo al login
        }
    }
}