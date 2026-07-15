package com.example.finanzas_independientes_app.presentation.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.MetaHistorialItem
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MetaHistorialViewModel @Inject constructor(
    private val finanzasRepository: FinanzasRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<MetaHistorialItem>>(emptyList())
    val items: StateFlow<List<MetaHistorialItem>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _isLoading.value = true
            _mensajeError.value = null
            when (val result = finanzasRepository.obtenerHistorialMetas()) {
                is ApiResult.Success -> _items.value = result.data
                is ApiResult.Error -> _mensajeError.value = mapError(result.error)
            }
            _isLoading.value = false
        }
    }

    fun limpiarError() {
        _mensajeError.value = null
    }

    private fun mapError(error: AppError): String = when (error) {
        is AppError.Api -> when (error.code) {
            ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
            else -> "No se pudo cargar el historial de metas."
        }
        is AppError.Network -> "Sin conexión. Verificá tu internet."
        is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
    }
}
