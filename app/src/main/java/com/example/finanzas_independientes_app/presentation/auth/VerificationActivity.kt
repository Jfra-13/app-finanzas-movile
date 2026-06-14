package com.example.finanzas_independientes_app.presentation.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.databinding.ActivityVerificationBinding

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBackArrowOTP.setOnClickListener { finish() }

        binding.btnVerify.setOnClickListener {
            startActivity(Intent(this, NewPasswordActivity::class.java))
        }
    }
}