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
class VerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError

    /**
     * Non-null signals OTP verified; first = email, second = otp, forwarded to NewPasswordActivity.
     */
    private val _otpVerificado = MutableStateFlow<Pair<String, String>?>(null)
    val otpVerificado: StateFlow<Pair<String, String>?> = _otpVerificado

    fun verificar(email: String, otp: String) {
        if (otp.length != 4 || !otp.all { it.isDigit() }) {
            _mensajeError.value = "El código debe tener exactamente 4 dígitos."
            return
        }

        viewModelScope.launch {
            _loading.value = true
            when (val result = authRepository.verifyOtp(email, otp)) {
                is ApiResult.Success -> _otpVerificado.value = Pair(email, otp)
                is ApiResult.Error -> _mensajeError.value = otpErrorMessage(result.error)
            }
            _loading.value = false
        }
    }

    fun limpiarError() { _mensajeError.value = null }
    fun limpiarEvento() { _otpVerificado.value = null }

    private fun otpErrorMessage(error: AppError): String = when {
        error is AppError.Api && error.code == ApiCode.OTP_INVALIDO ->
            "El código ingresado no es válido. Revisalo e intentá de nuevo."
        error is AppError.Api && error.code == ApiCode.OTP_EXPIRADO ->
            "El código venció. Volvé atrás y pedí uno nuevo."
        error is AppError.Api && error.code == ApiCode.OTP_BLOQUEADO ->
            "Demasiados intentos fallidos. Esperá unos minutos antes de intentar de nuevo."
        error is AppError.Network ->
            "Sin conexión. Revisá tu internet e intentá de nuevo."
        else -> "No se pudo verificar el código. Intentá de nuevo."
    }
}
