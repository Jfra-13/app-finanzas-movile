package com.example.finanzas_independientes_app.presentation.dashboard

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.databinding.ActivityDashboardBinding
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // 1. CONECTAMOS CON LOS NUEVOS IDs DE TU DISEÑO MODULAR
        // El número gigante vive dentro del parcial de progreso diario (include con id)
        val tvMontoActual = binding.incProgresoDiario.tvMontoActual

        // El botón flotante "+" vive en el parcial de la barra inferior
        val btnAgregar = binding.incBottomNav.cardFabAdd

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