package com.example.finanzas_independientes_app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.TendenciaMensual
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val finanzasRepository: FinanzasRepository
) : ViewModel() {

    private val _resumenSemanal = MutableStateFlow<List<ResumenSemanalDia>>(emptyList())
    val resumenSemanal: StateFlow<List<ResumenSemanalDia>> = _resumenSemanal

    private val _tendenciaMensual = MutableStateFlow<TendenciaMensual?>(null)
    val tendenciaMensual: StateFlow<TendenciaMensual?> = _tendenciaMensual

    private val _resumenCategorias = MutableStateFlow<Map<String, Double>>(emptyMap())
    val resumenCategorias: StateFlow<Map<String, Double>> = _resumenCategorias

    private val _saludFinanciera = MutableStateFlow<List<SaludFinancieraItem>>(emptyList())
    val saludFinanciera: StateFlow<List<SaludFinancieraItem>> = _saludFinanciera

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun cargar() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val deferredSemanal = async { finanzasRepository.obtenerResumenSemanal() }
            val deferredTendencia = async { finanzasRepository.obtenerTendenciaMensual() }
            val deferredCategorias = async { finanzasRepository.obtenerResumenCategorias() }
            val deferredSalud = async { finanzasRepository.obtenerSaludFinanciera() }

            when (val r = deferredSemanal.await()) {
                is ApiResult.Success -> _resumenSemanal.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            when (val r = deferredTendencia.await()) {
                is ApiResult.Success -> _tendenciaMensual.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            when (val r = deferredCategorias.await()) {
                is ApiResult.Success -> _resumenCategorias.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            when (val r = deferredSalud.await()) {
                is ApiResult.Success -> _saludFinanciera.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            _isLoading.value = false
        }
    }

    fun limpiarError() {
        _errorMessage.value = null
    }

    private fun handleError(error: AppError): String {
        return when (error) {
            is AppError.Api -> when (error.code) {
                ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
                ApiCode.META_NO_ENCONTRADA -> "Sin meta activa para este período."
                else -> "Error del servidor: ${error.code.raw}"
            }
            is AppError.Network -> "Sin conexión. Verificá tu internet."
            is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
        }
    }
}
