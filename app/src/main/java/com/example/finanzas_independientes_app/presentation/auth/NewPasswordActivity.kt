package com.example.finanzas_independientes_app.presentation.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.databinding.ActivityNewPasswordBinding

class NewPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBackArrowNewPass.setOnClickListener { finish() }

        binding.btnSaveNewPassword.setOnClickListener {
            // Tras cambiar la contraseña, lo devolvemos al Login y limpiamos la pila
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }
}