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

    // Manejador de estados: ¿Cargando?, ¿Éxito?, ¿Error?
    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    fun registrarUsuario(email: String, pass: String, oficio: String) {
        // 1. Validaciones de QA básicas (Frontend)
        if (email.isBlank() || pass.isBlank() || oficio.isBlank()) {
            _mensajeUI.value = "Todos los campos son obligatorios"
            return
        }
        if (!email.contains("@")) {
            _mensajeUI.value = "Por favor ingresa un correo válido"
            return
        }

        // 2. Ejecutar la llamada de red en segundo plano
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dto = UsuarioRegistroDTO(email, pass, oficio)
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

    // Función para limpiar el mensaje después de mostrarlo
    fun limpiarMensaje() {
        _mensajeUI.value = null
    }
}