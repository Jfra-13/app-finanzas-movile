package com.example.finanzas_independientes_app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError

    /** Non-null signals that the password was reset successfully — navigate to Login. */
    private val _resetExitoso = MutableStateFlow(false)
    val resetExitoso: StateFlow<Boolean> = _resetExitoso

    fun restablecer(email: String, otp: String, newPassword: String, repeat: String) {
        if (newPassword != repeat) {
            _mensajeError.value = "Las contraseñas no coinciden."
            return
        }
        if (newPassword.length < 6) {
            _mensajeError.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }

        viewModelScope.launch {
            _loading.value = true
            when (val result = authRepository.resetPassword(email, otp, newPassword)) {
                is ApiResult.Success -> _resetExitoso.value = true
                is ApiResult.Error -> _mensajeError.value = otpErrorMessage(result.error)
            }
            _loading.value = false
        }
    }

    fun limpiarError() { _mensajeError.value = null }
    fun limpiarEvento() { _resetExitoso.value = false }

    private fun otpErrorMessage(error: AppError): String = when {
        error is AppError.Api && error.code == ApiCode.OTP_INVALIDO ->
            "El código no es válido. Puede que ya fue usado. Pedí uno nuevo."
        error is AppError.Api && error.code == ApiCode.OTP_EXPIRADO ->
            "El código venció. Volvé al inicio y pedí uno nuevo."
        error is AppError.Api && error.code == ApiCode.OTP_BLOQUEADO ->
            "Demasiados intentos fallidos. Esperá unos minutos e iniciá el proceso de nuevo."
        error is AppError.Network ->
            "Sin conexión. Revisá tu internet e intentá de nuevo."
        else -> "No se pudo restablecer la contraseña. Intentá de nuevo."
    }
}
