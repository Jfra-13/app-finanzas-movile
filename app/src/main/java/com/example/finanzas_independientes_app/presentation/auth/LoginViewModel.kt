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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    // Non-null signals successful login; carries the user ID for Activity routing.
    // Routing is now done via SessionManager.tipoNegocio — no SharedPreferences needed.
    private val _loginExitoso = MutableStateFlow<Long?>(null)
    val loginExitoso: StateFlow<Long?> = _loginExitoso

    fun iniciarSesion(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _mensajeUI.value = "Ingresa tu correo y contraseña"
            return
        }

        viewModelScope.launch {
            when (val result = authRepository.login(email, pass)) {
                is ApiResult.Success -> _loginExitoso.value = result.data.usuario.usuarioId
                is ApiResult.Error -> _mensajeUI.value = result.error.toUserMessage()
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }
}
