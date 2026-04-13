package com.example.finanzas_independientes_app.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.DashboardActivity
import com.example.finanzas_independientes_app.R
import com.google.android.material.card.MaterialCardView

class SelectBusinessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_business)

        val cardBodega = findViewById<MaterialCardView>(R.id.cardBodega)
        val cardTaxi = findViewById<MaterialCardView>(R.id.cardTaxi)
        val cardServicios = findViewById<MaterialCardView>(R.id.cardServicios)
        val cardPersonalizado = findViewById<MaterialCardView>(R.id.cardPersonalizado)

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
        cardBodega.setOnClickListener { onBusinessSelected("Bodega") }
        cardTaxi.setOnClickListener { onBusinessSelected("Taxi") }
        cardServicios.setOnClickListener { onBusinessSelected("Servicios") }
        cardPersonalizado.setOnClickListener { onBusinessSelected("Personalizado") }
    }
}