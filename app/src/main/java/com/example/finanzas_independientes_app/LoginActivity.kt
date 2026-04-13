package com.example.finanzas_independientes_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.presentation.ForgotPasswordActivity
import com.example.finanzas_independientes_app.presentation.LoginViewModel
import com.example.finanzas_independientes_app.presentation.RegisterActivity
import com.example.finanzas_independientes_app.presentation.SelectBusinessActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        val etEmail = findViewById<EditText>(R.id.etLoginEmail)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvIrARegistro = findViewById<TextView>(R.id.tvIrARegistro)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword) // Nuevo ID del XML

        // 1. Al hacer clic en Iniciar Sesión
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()
            viewModel.iniciarSesion(email, pass)
        }

        // 2. Navegación a Registro (Actualizado para apuntar a la nueva clase)
        tvIrARegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // 3. Navegación a Recuperar Contraseña
        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        // 4. Observamos los mensajes de error
        lifecycleScope.launch {
            viewModel.mensajeUI.collect { mensaje ->
                if (mensaje != null) {
                    Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                }
            }
        }

        // 5. ¡LA MAGIA DE LA MEMORIA! Observamos si el login fue exitoso
        lifecycleScope.launch {
            viewModel.loginExitoso.collect { usuarioId ->
                if (usuarioId != null) {
                    // Guardamos el ID del usuario
                    val sharedPref = getSharedPreferences("MisFinanzasApp", Context.MODE_PRIVATE)
                    sharedPref.edit().putLong("USUARIO_ID", usuarioId).apply()

                    // PREGUNTAMOS: ¿Ya eligió su negocio en este celular?
                    val eligioNegocio = sharedPref.getBoolean("ELIGIO_NEGOCIO", false)

                    val intent = if (eligioNegocio) {
                        Intent(this@LoginActivity, DashboardActivity::class.java)
                    } else {
                        Intent(this@LoginActivity, SelectBusinessActivity::class.java)
                    }

                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}