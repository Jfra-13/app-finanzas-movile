package com.example.finanzas_independientes_app

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.presentation.DashboardViewModel
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // 1. CONECTAMOS CON LOS NUEVOS IDs DE TU DISEÑO MODULAR
        // Este es el número gigante dentro de tu gráfico semicircular
        val tvMontoActual = findViewById<TextView>(R.id.tvMontoActual)

        // Este es tu nuevo y flamante botón flotante "+"
        val btnAgregar = findViewById<MaterialCardView>(R.id.cardFabAdd)

        // 2. Leemos el ID guardado en la memoria del celular
        val sharedPref = getSharedPreferences("MisFinanzasApp", Context.MODE_PRIVATE)
        val miUsuarioId = sharedPref.getLong("USUARIO_ID", -1L)

        // Verificamos sesión
        if (miUsuarioId == -1L) {
            Toast.makeText(this, "Error: No se encontró sesión activa", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 3. Pedimos la cuota al backend
        viewModel.cargarCuotaDiaria(miUsuarioId)

        // 4. Observamos el número gigante en tiempo real y lo pintamos en el nuevo TextView
        lifecycleScope.launch {
            viewModel.cuotaActual.collect { cuota ->
                tvMontoActual?.text = cuota
            }
        }

        // 5. ACCIÓN DEL NUEVO BOTÓN "+"
        btnAgregar?.setOnClickListener {
            // COMO YA NO HAY CAJA DE TEXTO EN EL DASHBOARD:
            // Aquí deberás abrir un "BottomSheetDialog" o una "Nueva Activity"
            // para que el usuario escriba cuánto dinero quiere registrar.
            Toast.makeText(this, "Próximo paso: Abrir ventana para ingresar monto", Toast.LENGTH_SHORT).show()
        }

        // 6. Observamos los mensajes (Toasts)
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