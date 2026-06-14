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
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError

    /** Non-null signals that the OTP was sent; carries the email to forward to VerificationActivity. */
    private val _otpEnviado = MutableStateFlow<String?>(null)
    val otpEnviado: StateFlow<String?> = _otpEnviado

    fun enviarOtp(email: String) {
        if (email.isBlank() || !email.contains('@')) {
            _mensajeError.value = "Ingresá un correo electrónico válido."
            return
        }

        viewModelScope.launch {
            _loading.value = true
            when (val result = authRepository.forgotPassword(email.trim())) {
                is ApiResult.Success -> _otpEnviado.value = email.trim()
                is ApiResult.Error -> _mensajeError.value = result.error.toUserMessage()
            }
            _loading.value = false
        }
    }

    fun limpiarError() { _mensajeError.value = null }
    fun limpiarEvento() { _otpEnviado.value = null }
}
