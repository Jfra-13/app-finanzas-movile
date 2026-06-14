package com.example.finanzas_independientes_app.presentation.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBackArrowForgot.setOnClickListener { finish() } // Vuelve atrás

        binding.btnSendResetCode.setOnClickListener {
            // Aquí irá la lógica de red más adelante. Por ahora simulamos ir al OTP.
            startActivity(Intent(this, VerificationActivity::class.java))
        }
    }
}