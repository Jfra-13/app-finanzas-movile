package com.example.finanzas_independientes_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.presentation.RegistroViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: RegistroViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializamos nuestro "cerebro" (ViewModel)
        viewModel = ViewModelProvider(this)[RegistroViewModel::class.java]

        // 1. Enlazamos los elementos del diseño XML
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etOficio = findViewById<EditText>(R.id.etOficio)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        // 2. ¿Qué pasa al hacer clic en el botón?
        btnRegistrar.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val oficio = etOficio.text.toString()

            // Le pasamos el trabajo sucio al ViewModel (Él validará y enviará al backend)
            viewModel.registrarUsuario(email, password, oficio)
        }

        // 3. ¡Magia Reactiva! Escuchamos las respuestas del ViewModel
        lifecycleScope.launch {
            viewModel.mensajeUI.collect { mensaje ->
                if (mensaje != null) {
                    Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_LONG).show()

                    // Si el mensaje dice "éxito", saltamos a la nueva pantalla
                    if (mensaje.contains("éxito")) {
                        val intent = android.content.Intent(this@MainActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish() // Cerramos el registro para que el botón de "Atrás" no lo regrese aquí
                    }

                    viewModel.limpiarMensaje()
                }
            }
        }
    }
}