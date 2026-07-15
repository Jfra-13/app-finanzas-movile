package com.example.finanzas_independientes_app.presentation.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.databinding.ActivityRegisterBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegistroViewModel by lazy { ViewModelProvider(this)[RegistroViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBackArrow.setOnClickListener { finish() }

        binding.btnRegistrar.setOnClickListener {
            viewModel.registrarUsuario(
                nombre = binding.etRegisterName.text.toString(),
                email = binding.etEmail.text.toString(),
                pass = binding.etPassword.text.toString(),
                repetirPass = binding.etRegisterRepeatPassword.text.toString()
            )
        }

        lifecycleScope.launch {
            viewModel.mensajeUI.collect { mensaje ->
                if (mensaje != null) {
                    Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_SHORT).show()
                    if (mensaje.contains("éxito", ignoreCase = true)) {
                        finish()
                    }
                    viewModel.limpiarMensaje()
                }
            }
        }
    }
}
