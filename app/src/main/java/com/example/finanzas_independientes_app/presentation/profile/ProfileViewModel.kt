package com.example.finanzas_independientes_app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.Perfil
import com.example.finanzas_independientes_app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Serves the profile header with fresh server data (GET /usuarios/me).
 * On failure the UI keeps the session-derived values it already shows,
 * so there is no error state to surface here.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _perfil = MutableStateFlow<Perfil?>(null)
    val perfil: StateFlow<Perfil?> = _perfil

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            when (val result = authRepository.obtenerPerfil()) {
                is ApiResult.Success -> _perfil.value = result.data
                is ApiResult.Error -> Unit
            }
        }
    }
}
