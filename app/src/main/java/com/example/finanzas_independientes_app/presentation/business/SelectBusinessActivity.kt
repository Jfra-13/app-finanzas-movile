package com.example.finanzas_independientes_app.presentation.business

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.databinding.ActivitySelectBusinessBinding
import com.example.finanzas_independientes_app.presentation.dashboard.DashboardActivity

class SelectBusinessActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectBusinessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectBusinessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Función que se ejecutará al elegir cualquier tarjeta
        val onBusinessSelected = { negocio: String ->
            // 1. Guardamos en memoria que YA eligió un negocio
            val sharedPref = getSharedPreferences("MisFinanzasApp", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("ELIGIO_NEGOCIO", true).apply()

            // (Aquí a futuro enviaremos el dato al Backend)
            Toast.makeText(this, "Plantilla: $negocio configurada", Toast.LENGTH_SHORT).show()

            // 2. Saltamos al Dashboard y cerramos esta pantalla
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finishAffinity() // Limpia la pila para que no pueda volver atrás
        }

        // Asignamos los clics a las tarjetas
        binding.cardBodega.setOnClickListener { onBusinessSelected("Bodega") }
        binding.cardTaxi.setOnClickListener { onBusinessSelected("Taxi") }
        binding.cardServicios.setOnClickListener { onBusinessSelected("Servicios") }
        binding.cardPersonalizado.setOnClickListener { onBusinessSelected("Personalizado") }
    }
}