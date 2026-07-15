package com.example.finanzas_independientes_app.presentation.business

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.finanzas_independientes_app.databinding.ActivitySelectBusinessBinding
import com.example.finanzas_independientes_app.presentation.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectBusinessActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectBusinessBinding
    private val viewModel: SelectBusinessViewModel by lazy {
        ViewModelProvider(this)[SelectBusinessViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectBusinessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardBodega.setOnClickListener { viewModel.seleccionar("BODEGA") }
        binding.cardTaxi.setOnClickListener { viewModel.seleccionar("TAXI") }
        binding.cardServicios.setOnClickListener { viewModel.seleccionar("FREELANCE") }
        binding.cardPersonalizado.setOnClickListener { viewModel.seleccionar("PERSONALIZADO") }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.mensajeError.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(this@SelectBusinessActivity, msg, Toast.LENGTH_SHORT).show()
                            viewModel.limpiarError()
                        }
                    }
                }
                launch {
                    viewModel.negocioSeleccionado.collect { seleccionado ->
                        if (seleccionado) {
                            val intent = Intent(this@SelectBusinessActivity, DashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            viewModel.limpiarEvento()
                        }
                    }
                }
            }
        }
    }
}
