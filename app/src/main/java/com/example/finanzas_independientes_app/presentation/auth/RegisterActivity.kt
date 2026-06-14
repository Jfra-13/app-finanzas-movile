package com.example.finanzas_independientes_app.presentation.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: RegistroViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[RegistroViewModel::class.java]

        // 2. Botón de retroceso
        binding.ivBackArrow.setOnClickListener { finish() }

        // 3. Capturamos los datos y los enviamos al ViewModel
        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.etRegisterName.text.toString()
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            val repeatPass = binding.etRegisterRepeatPassword.text.toString()

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