package com.example.finanzas_independientes_app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.safeUnitCall
import com.example.finanzas_independientes_app.core.network.toUserMessage
import com.example.finanzas_independientes_app.data.remote.dto.UsuarioRegistroDTO
import com.example.finanzas_independientes_app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistroViewModel : ViewModel() {

    private val api = ServiceLocator.api
    private val gson = ServiceLocator.gson

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    // 👇 NUEVO: Esta variable le avisará a la Activity cuándo cambiar al Login
    private val _registroExitoso = MutableStateFlow(false)
    val registroExitoso: StateFlow<Boolean> = _registroExitoso

    // Actualizamos los parámetros para que coincidan con la nueva pantalla
    fun registrarUsuario(nombre: String, email: String, pass: String, repetirPass: String) {

        // 1. Validaciones de QA básicas (Frontend)
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

        // 2. Ejecutar la llamada de red
        viewModelScope.launch {
            val dto = UsuarioRegistroDTO(nombre, email, pass)
            when (val result = safeUnitCall(gson) { api.registrar(dto) }) {
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

    // Limpiar la bandera de navegación
    fun resetRegistroExitoso() {
        _registroExitoso.value = false
    }
}