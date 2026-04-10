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
import com.example.finanzas_independientes_app.presentation.LoginViewModel
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

        // 1. Al hacer clic en Iniciar Sesión
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()
            viewModel.iniciarSesion(email, pass)
        }

        // 2. Si no tiene cuenta, lo mandamos a la pantalla de Registro (MainActivity)
        tvIrARegistro.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 3. Observamos los mensajes de error
        lifecycleScope.launch {
            viewModel.mensajeUI.collect { mensaje ->
                if (mensaje != null) {
                    Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                }
            }
        }

        // 4. ¡LA MAGIA DE LA MEMORIA! Observamos si el login fue exitoso
        lifecycleScope.launch {
            viewModel.loginExitoso.collect { usuarioId ->
                if (usuarioId != null) {
                    // Guardamos el ID en la memoria del celular (SharedPreferences)
                    val sharedPref = getSharedPreferences("MisFinanzasApp", Context.MODE_PRIVATE)
                    sharedPref.edit().putLong("USUARIO_ID", usuarioId).apply()

                    // Saltamos al Dashboard y cerramos el Login
                    val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}