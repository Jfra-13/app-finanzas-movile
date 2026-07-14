package com.example.finanzas_independientes_app.presentation.categorias

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

@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val finanzasRepository: FinanzasRepository
) : ViewModel() {

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError

    /** One-shot event: non-null string = success message, UI should consume and reset to null. */
    private val _createSuccess = MutableStateFlow<String?>(null)
    val createSuccess: StateFlow<String?> = _createSuccess

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _isLoading.value = true
            _mensajeError.value = null

            when (val result = finanzasRepository.listarCategorias()) {
                is ApiResult.Success -> {
                    _categorias.value = result.data
                }
                is ApiResult.Error -> {
                    _mensajeError.value = mapError(result.error)
                }
            }

            _isLoading.value = false
        }
    }

    fun crear(nombre: String, tipo: String) {
        val nombreTrimmed = nombre.trim()
        if (nombreTrimmed.isBlank()) {
            _mensajeError.value = "El nombre de la categoría no puede estar vacío."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _mensajeError.value = null

            when (val result = finanzasRepository.crearCategoria(nombreTrimmed, tipo)) {
                is ApiResult.Success -> {
                    _createSuccess.value = "Categoría \"${result.data.nombre}\" creada."
                    cargar()
                }
                is ApiResult.Error -> {
                    _isLoading.value = false
                    _mensajeError.value = mapCreateError(result.error)
                }
            }
        }
    }

    fun renombrar(id: Long, nombre: String) {
        val nombreTrimmed = nombre.trim()
        if (nombreTrimmed.isBlank()) {
            _mensajeError.value = "El nombre de la categoría no puede estar vacío."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _mensajeError.value = null

            when (val result = finanzasRepository.actualizarCategoria(id, nombreTrimmed)) {
                is ApiResult.Success -> {
                    _createSuccess.value = "Categoría renombrada."
                    cargar()
                }
                is ApiResult.Error -> {
                    _isLoading.value = false
                    _mensajeError.value = mapMutateError(result.error)
                }
            }
        }
    }

    fun eliminar(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _mensajeError.value = null

            when (val result = finanzasRepository.eliminarCategoria(id)) {
                is ApiResult.Success -> {
                    _createSuccess.value = "Categoría eliminada. Sus movimientos quedan sin categoría."
                    cargar()
                }
                is ApiResult.Error -> {
                    _isLoading.value = false
                    _mensajeError.value = mapMutateError(result.error)
                }
            }
        }
    }

    fun limpiarError() {
        _mensajeError.value = null
    }

    fun limpiarCreateSuccess() {
        _createSuccess.value = null
    }

    // --- Error mapping ---

    private fun mapError(error: AppError): String = when (error) {
        is AppError.Api -> when (error.code) {
            ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
            else -> "Error al cargar categorías."
        }
        is AppError.Network -> "Sin conexión. Verificá tu internet."
        is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
    }

    private fun mapCreateError(error: AppError): String = when (error) {
        is AppError.Api -> when (error.code) {
            ApiCode.VALIDATION_ERROR -> "Datos inválidos. Revisá los campos."
            ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
            else -> "Error al crear la categoría."
        }
        is AppError.Network -> "Sin conexión. Verificá tu internet."
        is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
    }

    private fun mapMutateError(error: AppError): String = when (error) {
        is AppError.Api -> when (error.code) {
            ApiCode.ACCESO_DENEGADO -> "Las categorías del sistema no se pueden modificar."
            ApiCode.CATEGORIA_NO_ENCONTRADA -> "La categoría ya no existe. Actualizá la lista."
            ApiCode.VALIDATION_ERROR -> "Datos inválidos. Revisá los campos."
            ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
            else -> "No se pudo completar la operación."
        }
        is AppError.Network -> "Sin conexión. Verificá tu internet."
        is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
    }
}
