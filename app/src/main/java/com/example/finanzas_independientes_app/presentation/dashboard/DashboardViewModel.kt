package com.example.finanzas_independientes_app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.safeApiCall
import com.example.finanzas_independientes_app.core.network.safeUnitCall
import com.example.finanzas_independientes_app.data.remote.dto.TransaccionRegistroDTO
import com.example.finanzas_independientes_app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val api = ServiceLocator.api
    private val gson = ServiceLocator.gson

    // Aquí guardaremos el número que se mostrará gigante en la pantalla
    private val _cuotaActual = MutableStateFlow("Calculando...")
    val cuotaActual: StateFlow<String> = _cuotaActual

    // Mensajes para el usuario (Toast)
    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    // Datos simulados para el MVP (Meta y días).
    // Se reemplazan por la meta persistida en Fase 4.
    private val miMetaMensual: Double = 3000.00
    private val diasRestantes: Int = 10

    // Identity now comes from the JWT; usuarioId is kept only for the caller's
    // signature and will be dropped when the dashboard is reworked in Fase 4.
    fun cargarCuotaDiaria(usuarioId: Long) {
        viewModelScope.launch {
            val result = safeApiCall(gson) {
                api.obtenerCuotaDiaria(miMetaMensual, diasRestantes)
            }
            when (result) {
                is ApiResult.Success -> {
                    val cuota = result.data
                    _cuotaActual.value = if (cuota <= 0) {
                        String.format("¡Meta superada por S/ %.2f! 🎉", kotlin.math.abs(cuota))
                    } else {
                        String.format("S/ %.2f", cuota)
                    }
                }
                is ApiResult.Error -> _cuotaActual.value = "Error al calcular"
            }
        }
    }

    fun registrarIngreso(montoTexto: String, usuarioId: Long) {
        val monto = montoTexto.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            _mensajeUI.value = "Ingresa un monto válido"
            return
        }

        viewModelScope.launch {
            val dto = TransaccionRegistroDTO(monto = monto, tipo = "INGRESO")
            when (safeUnitCall(gson) { api.registrarTransaccion(dto) }) {
                is ApiResult.Success -> {
                    _mensajeUI.value = "¡Ganancia registrada!"
                    cargarCuotaDiaria(usuarioId)
                }
                is ApiResult.Error -> _mensajeUI.value = "Error al guardar"
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }
}
