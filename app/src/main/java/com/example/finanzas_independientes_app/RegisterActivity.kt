package com.example.finanzas_independientes_app.presentation

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.R
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: RegistroViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        viewModel = ViewModelProvider(this)[RegistroViewModel::class.java]

        // 1. Enlazamos TODOS los campos del nuevo XML
        val ivBack = findViewById<ImageView>(R.id.ivBackArrow)
        val etNombre = findViewById<EditText>(R.id.etRegisterName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etRepeatPassword = findViewById<EditText>(R.id.etRegisterRepeatPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        // 2. Botón de retroceso
        ivBack.setOnClickListener { finish() }

        // 3. Capturamos los datos y los enviamos al ViewModel
        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString()
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()
            val repeatPass = etRepeatPassword.text.toString()

            // Le pasamos los 4 datos exactos que espera el cerebro
            viewModel.registrarUsuario(nombre, email, pass, repeatPass)
        }

        // 4. Escuchamos las respuestas (Magia reactiva)
        lifecycleScope.launch {
            viewModel.mensajeUI.collect { mensaje ->
                if (mensaje != null) {
                    Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_SHORT).show()

                    if (mensaje.contains("éxito", ignoreCase = true)) {
                        finish() // Vuelve al login tras registro exitoso
                    }

                    viewModel.limpiarMensaje()
                }
            }
        }
    }
}