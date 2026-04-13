package com.example.finanzas_independientes_app.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.LoginActivity
import com.example.finanzas_independientes_app.R

class OnboardingOneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_one)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        btnNext.setOnClickListener {
            startActivity(Intent(this, OnboardingTwoActivity::class.java))
        }

        tvSkip.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity() // Limpia la pila para ir directo al login
        }
    }
}