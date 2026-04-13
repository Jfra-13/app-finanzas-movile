package com.example.finanzas_independientes_app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.data.remote.RetrofitClient
import com.example.finanzas_independientes_app.data.remote.dto.UsuarioRegistroDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistroViewModel : ViewModel() {

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

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
        // NUEVA VALIDACIÓN: Confirmar contraseña
        if (pass != repetirPass) {
            _mensajeUI.value = "Las contraseñas no coinciden"
            return
        }
        if (pass.length < 6) {
            _mensajeUI.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        // 2. Ejecutar la llamada de red en segundo plano
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Creamos el DTO con los datos correctos (Nombre en vez de Oficio)
                val dto = UsuarioRegistroDTO(nombre, email, pass)
                val response = RetrofitClient.apiService.registrarUsuario(dto)

                if (response.isSuccessful) {
                    _mensajeUI.value = "¡Bienvenido! Cuenta creada con éxito."
                } else {
                    _mensajeUI.value = "Error del servidor: Código ${response.code()}"
                }
            } catch (e: Exception) {
                _mensajeUI.value = "Error de red: No se pudo conectar al servidor."
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }
}