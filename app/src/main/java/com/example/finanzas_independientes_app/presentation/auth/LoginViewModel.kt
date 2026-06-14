package com.example.finanzas_independientes_app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.safeApiCall
import com.example.finanzas_independientes_app.core.network.toUserMessage
import com.example.finanzas_independientes_app.data.remote.dto.LoginDTO
import com.example.finanzas_independientes_app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val api = ServiceLocator.api
    private val session = ServiceLocator.sessionManager
    private val gson = ServiceLocator.gson

    // Mensajes de error o validación
    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    // Si el login es exitoso, guardaremos el ID del usuario aquí para que la pantalla lo lea
    private val _loginExitoso = MutableStateFlow<Long?>(null)
    val loginExitoso: StateFlow<Long?> = _loginExitoso

    fun iniciarSesion(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _mensajeUI.value = "Ingresa tu correo y contraseña"
            return
        }

        viewModelScope.launch {
            when (val result = safeApiCall(gson) { api.login(LoginDTO(email, pass)) }) {
                is ApiResult.Success -> {
                    session.saveSession(result.data)
                    _loginExitoso.value = result.data.usuarioId
                }
                is ApiResult.Error -> _mensajeUI.value = result.error.toUserMessage()
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }
}