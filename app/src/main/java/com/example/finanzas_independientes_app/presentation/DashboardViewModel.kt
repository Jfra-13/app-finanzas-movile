package com.example.finanzas_independientes_app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.data.remote.RetrofitClient
import com.example.finanzas_independientes_app.data.remote.dto.TransaccionRegistroDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    // Aquí guardaremos el número que se mostrará gigante en la pantalla
    private val _cuotaActual = MutableStateFlow("Calculando...")
    val cuotaActual: StateFlow<String> = _cuotaActual

    // Mensajes para el usuario (Toast)
    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    // Datos simulados para el MVP (Meta y días).
    // ¡Eliminamos el usuario estático!
    private val miMetaMensual: Double = 3000.00
    private val diasRestantes: Int = 10

    // Modificación 1: Ahora pedimos el usuarioId como parámetro
    fun cargarCuotaDiaria(usuarioId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Usamos el ID real para consultar al backend
                val response = RetrofitClient.apiService.obtenerCuotaDiaria(usuarioId, miMetaMensual, diasRestantes)

                if (response.isSuccessful && response.body() != null) {
                    val cuota = response.body()!!

                    if (cuota <= 0) {
                        // Si la cuota es 0 o negativa, sacamos el valor absoluto (positivo)
                        val gananciaExtra = kotlin.math.abs(cuota)
                        _cuotaActual.value = String.format("¡Meta superada por S/ %.2f! \uD83C\uDF89", gananciaExtra)
                    } else {
                        // Si aún falta dinero
                        _cuotaActual.value = String.format("S/ %.2f", cuota)
                    }
                } else {
                    _cuotaActual.value = "Error al calcular"
                }
            } catch (e: Exception) {
                _cuotaActual.value = "Sin conexión"
            }
        }
    }

    // Modificación 2: Ahora pedimos el usuarioId como parámetro al registrar
    fun registrarIngreso(montoTexto: String, usuarioId: Long) {
        val monto = montoTexto.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            _mensajeUI.value = "Ingresa un monto válido"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Asignamos el ID real a la transacción
                val dto = TransaccionRegistroDTO(monto, "INGRESO", usuarioId)
                val response = RetrofitClient.apiService.registrarTransaccion(dto)

                if (response.isSuccessful) {
                    _mensajeUI.value = "¡Ganancia registrada!"
                    // ¡La Magia! Recalculamos la cuota pasándole el mismo ID
                    cargarCuotaDiaria(usuarioId)
                } else {
                    _mensajeUI.value = "Error al guardar"
                }
            } catch (e: Exception) {
                _mensajeUI.value = "Error de red"
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }
}