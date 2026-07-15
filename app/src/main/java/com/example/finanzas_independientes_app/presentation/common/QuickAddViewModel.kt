package com.example.finanzas_independientes_app.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the shared FAB quick-add on screens that don't own transaction data
 * (Profile, Calendar). Screens that DO own that data (Dashboard, Analytics) keep
 * their own registration so they can refresh their own view on success — this
 * exists so the data-less screens don't have to.
 */
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val finanzasRepository: FinanzasRepository
) : ViewModel() {

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    fun cargarCategorias() {
        viewModelScope.launch {
            when (val r = finanzasRepository.listarCategorias()) {
                is ApiResult.Success -> _categorias.value = r.data
                is ApiResult.Error -> { /* non-fatal; spinner falls back to "Sin categoría" */ }
            }
        }
    }

    fun registrarTransaccion(monto: String, tipo: String, descripcion: String?, categoriaId: Long?) {
        val montoDouble = monto.trim().toDoubleOrNull()
        if (montoDouble == null || montoDouble <= 0) {
            _mensajeUI.value = "Ingresá un monto válido (mayor a 0)"
            return
        }
        viewModelScope.launch {
            when (val r = finanzasRepository.registrarTransaccion(
                monto = montoDouble,
                tipo = tipo,
                descripcion = descripcion?.takeIf { it.isNotBlank() },
                categoriaId = categoriaId
            )) {
                is ApiResult.Success ->
                    _mensajeUI.value = if (tipo == "INGRESO") "¡Ingreso registrado!" else "Egreso registrado."
                is ApiResult.Error -> _mensajeUI.value = handleError(r.error)
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }

    private fun handleError(error: AppError): String = when (error) {
        is AppError.Api -> when (error.code) {
            ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
            ApiCode.VALIDATION_ERROR -> "Datos inválidos. Revisá los campos."
            else -> "Error del servidor: ${error.code.raw}"
        }
        is AppError.Network -> "Sin conexión. Verificá tu internet."
        is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
    }
}
