package com.example.finanzas_independientes_app.presentation.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.toUserMessage
import com.example.finanzas_independientes_app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectBusinessViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError

    /** Non-null signals that the business type was updated — navigate to Dashboard. */
    private val _negocioSeleccionado = MutableStateFlow(false)
    val negocioSeleccionado: StateFlow<Boolean> = _negocioSeleccionado

    fun seleccionar(tipoNegocio: String) {
        viewModelScope.launch {
            _loading.value = true
            when (val result = authRepository.actualizarNegocio(tipoNegocio)) {
                is ApiResult.Success -> _negocioSeleccionado.value = true
                is ApiResult.Error -> _mensajeError.value = result.error.toUserMessage()
            }
            _loading.value = false
        }
    }

    fun limpiarError() { _mensajeError.value = null }
    fun limpiarEvento() { _negocioSeleccionado.value = false }
}
