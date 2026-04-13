package com.example.finanzas_independientes_app.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.LoginActivity
import com.example.finanzas_independientes_app.R

class NewPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_password)

        val ivBack = findViewById<ImageView>(R.id.ivBackArrowNewPass)
        val btnSave = findViewById<Button>(R.id.btnSaveNewPassword)

        ivBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            // Tras cambiar la contraseña, lo devolvemos al Login y limpiamos la pila
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }
}