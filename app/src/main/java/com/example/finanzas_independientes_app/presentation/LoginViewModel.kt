package com.example.finanzas_independientes_app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.data.remote.RetrofitClient
import com.example.finanzas_independientes_app.data.remote.dto.LoginDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.iniciarSesion(LoginDTO(email, pass))

                if (response.isSuccessful && response.body() != null) {
                    // ¡Éxito! El servidor nos devolvió el ID real del usuario
                    _loginExitoso.value = response.body()
                } else {
                    _mensajeUI.value = "Credenciales incorrectas"
                }
            } catch (e: Exception) {
                _mensajeUI.value = "Error al conectar con el servidor"
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }
}