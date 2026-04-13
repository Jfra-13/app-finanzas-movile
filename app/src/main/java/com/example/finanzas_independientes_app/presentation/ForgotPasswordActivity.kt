package com.example.finanzas_independientes_app.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.R

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val ivBack = findViewById<ImageView>(R.id.ivBackArrowForgot)
        val btnSend = findViewById<Button>(R.id.btnSendResetCode)

        ivBack.setOnClickListener { finish() } // Vuelve atrás

        btnSend.setOnClickListener {
            // Aquí irá la lógica de red más adelante. Por ahora simulamos ir al OTP.
            startActivity(Intent(this, VerificationActivity::class.java))
        }
    }
}