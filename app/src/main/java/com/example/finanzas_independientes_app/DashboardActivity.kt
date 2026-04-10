package com.example.finanzas_independientes_app

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.presentation.DashboardViewModel
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        val tvCuota = findViewById<TextView>(R.id.tvCuota)
        val etMonto = findViewById<EditText>(R.id.etMonto)
        val btnRegistrarIngreso = findViewById<Button>(R.id.btnRegistrarIngreso)

        // 1. Leemos el ID guardado en la memoria del celular (SharedPreferences)
        val sharedPref = getSharedPreferences("MisFinanzasApp", Context.MODE_PRIVATE)
        val miUsuarioId = sharedPref.getLong("USUARIO_ID", -1L)

        // Si por alguna razón alguien entra sin iniciar sesión (ID -1), lo sacamos por seguridad
        if (miUsuarioId == -1L) {
            Toast.makeText(this, "Error: No se encontró sesión activa", Toast.LENGTH_SHORT).show()
            finish() // Cierra esta pantalla inmediatamente
            return
        }

        // 2. Apenas se abre la pantalla, pedimos la cuota pasándole el ID REAL
        viewModel.cargarCuotaDiaria(miUsuarioId)

        // 3. Al hacer clic, enviamos la ganancia y limpiamos la caja de texto (pasando el ID)
        btnRegistrarIngreso.setOnClickListener {
            val monto = etMonto.text.toString()
            viewModel.registrarIngreso(monto, miUsuarioId)
            etMonto.text.clear()
        }

        // 4. Observamos el número gigante en tiempo real
        lifecycleScope.launch {
            viewModel.cuotaActual.collect { cuota ->
                tvCuota.text = cuota
            }
        }

        // 5. Observamos los mensajes (Toasts)
        lifecycleScope.launch {
            viewModel.mensajeUI.collect { mensaje ->
                if (mensaje != null) {
                    Toast.makeText(this@DashboardActivity, mensaje, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                }
            }
        }
    }
}