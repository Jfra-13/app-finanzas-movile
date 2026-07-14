package com.example.finanzas_independientes_app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.core.network.toUserMessage
import com.example.finanzas_independientes_app.domain.model.Perfil
import com.example.finanzas_independientes_app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _perfil = MutableStateFlow<Perfil?>(null)
    val perfil: StateFlow<Perfil?> = _perfil

    /** One-shot: true once the session ended and the UI must navigate to login. */
    private val _logoutDone = MutableStateFlow(false)
    val logoutDone: StateFlow<Boolean> = _logoutDone

    /** One-shot messages; UI consumes and clears. */
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    init {
        cargarPerfil()
    }

    fun cargarPerfil() {
        viewModelScope.launch {
            when (val result = authRepository.obtenerPerfil()) {
                is ApiResult.Success -> _perfil.value = result.data
                is ApiResult.Error -> Unit // rows fall back to session values
            }
        }
    }

    fun cambiarNombre(nombre: String) {
        val trimmed = nombre.trim()
        if (trimmed.isBlank()) {
            _mensaje.value = "Ingresá un nombre."
            return
        }
        viewModelScope.launch {
            when (val result = authRepository.actualizarPerfil(nombre = trimmed)) {
                is ApiResult.Success -> {
                    _perfil.value = result.data
                    _mensaje.value = "Nombre actualizado."
                }
                is ApiResult.Error -> _mensaje.value = mapUpdateError(result.error)
            }
        }
    }

    fun cambiarTelefono(telefono: String) {
        val trimmed = telefono.trim()
        if (trimmed.isBlank()) {
            _mensaje.value = "Ingresá un número de teléfono."
            return
        }
        viewModelScope.launch {
            when (val result = authRepository.actualizarPerfil(telefono = trimmed)) {
                is ApiResult.Success -> {
                    _perfil.value = result.data
                    _mensaje.value = "Teléfono actualizado."
                }
                is ApiResult.Error -> _mensaje.value = mapUpdateError(result.error)
            }
        }
    }

    /** Revokes the refresh token server-side (best effort) and always clears the local session. */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _logoutDone.value = true
        }
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    private fun mapUpdateError(error: AppError): String = when (error) {
        is AppError.Api -> when (error.code) {
            ApiCode.VALIDATION_ERROR ->
                error.fieldErrors.firstOrNull()?.message ?: "Revisá el número ingresado."
            else -> error.toUserMessage()
        }
        else -> error.toUserMessage()
    }
}
