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

        // 2. Ejecutar la llamada de red en segundo plano
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Creamos el DTO con una sola contraseña
                val dto = UsuarioRegistroDTO(nombre, email, pass)
                val response = RetrofitClient.apiService.registrarUsuario(dto)

                if (response.isSuccessful) {
                    // EXITO: Leemos el texto de tu Spring Boot ("¡Usuario registrado con exito!")
                    val mensajeServidor = response.body()?.string() ?: "¡Cuenta creada con éxito!"
                    _mensajeUI.value = mensajeServidor

                    // 👇 ¡La magia! Le avisamos a la vista que el registro terminó
                    _registroExitoso.value = true
                } else {
                    // ERROR 400: Leemos por qué Spring Boot lo rechazó ("El email ya está registrado")
                    val errorReal = response.errorBody()?.string() ?: "Error del servidor: ${response.code()}"
                    _mensajeUI.value = errorReal
                }
            } catch (e: Exception) {
                _mensajeUI.value = "Error de red: No se pudo conectar al servidor."
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