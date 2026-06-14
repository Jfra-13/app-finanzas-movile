package com.example.finanzas_independientes_app.presentation.auth

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
class RegistroViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    private val _registroExitoso = MutableStateFlow(false)
    val registroExitoso: StateFlow<Boolean> = _registroExitoso

    fun registrarUsuario(nombre: String, email: String, pass: String, repetirPass: String) {
        if (nombre.isBlank() || email.isBlank() || pass.isBlank() || repetirPass.isBlank()) {
            _mensajeUI.value = "Todos los campos son obligatorios"
            return
        }
        if (!email.contains("@")) {
            _mensajeUI.value = "Por favor ingresa un correo válido"
            return
        }
        if (pass != repetirPass) {
            _mensajeUI.value = "Las contraseñas no coinciden"
            return
        }
        if (pass.length < 6) {
            _mensajeUI.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        viewModelScope.launch {
            when (val result = authRepository.registro(nombre, email, pass)) {
                is ApiResult.Success -> {
                    _mensajeUI.value = "¡Cuenta creada con éxito!"
                    _registroExitoso.value = true
                }
                is ApiResult.Error -> _mensajeUI.value = result.error.toUserMessage()
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }

    fun resetRegistroExitoso() {
        _registroExitoso.value = false
    }
}
