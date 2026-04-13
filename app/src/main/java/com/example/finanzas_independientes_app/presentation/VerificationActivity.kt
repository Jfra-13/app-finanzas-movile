package com.example.finanzas_independientes_app.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.R

class VerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        val ivBack = findViewById<ImageView>(R.id.ivBackArrowOTP)
        val btnVerify = findViewById<Button>(R.id.btnVerify)

        ivBack.setOnClickListener { finish() }

        btnVerify.setOnClickListener {
            startActivity(Intent(this, NewPasswordActivity::class.java))
        }
    }
}