package com.example.finanzas_independientes_app.presentation.transacciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.Transaccion
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransaccionesViewModel @Inject constructor(
    private val finanzasRepository: FinanzasRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val DEFAULT_SORT = "fecha,desc"
    }

    // --- Transacciones state ---

    private val _transacciones = MutableStateFlow<List<Transaccion>>(emptyList())
    val transacciones: StateFlow<List<Transaccion>> = _transacciones

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError

    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty

    // Paging
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage

    // --- Date range filter (ISO yyyy-MM-dd, inclusive; null = no filter) ---

    private val _filtroFechas = MutableStateFlow<Pair<String, String>?>(null)
    val filtroFechas: StateFlow<Pair<String, String>?> = _filtroFechas

    // --- Categories (for the edit dialog spinner) ---

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias

    // --- Init ---

    init {
        cargar(refresh = true)
        cargarCategorias()
    }

    // --- Loading ---

    /** Load first page; pass refresh=true to reset the accumulated list. */
    fun cargar(refresh: Boolean = false) {
        if (_isLoading.value) return
        if (!refresh && _isLastPage.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _mensajeError.value = null

            val page = if (refresh) 0 else _currentPage.value

            val rango = _filtroFechas.value
            when (val result = finanzasRepository.listarTransacciones(
                desde = rango?.first,
                hasta = rango?.second,
                page = page,
                size = PAGE_SIZE,
                sort = DEFAULT_SORT
            )) {
                is ApiResult.Success -> {
                    val paginated = result.data
                    _currentPage.value = paginated.number + 1
                    _isLastPage.value = paginated.last

                    val updated = if (refresh) {
                        paginated.content
                    } else {
                        _transacciones.value + paginated.content
                    }
                    _transacciones.value = updated
                    _isEmpty.value = updated.isEmpty()
                }
                is ApiResult.Error -> {
                    _mensajeError.value = mapError(result.error)
                    if (refresh) _isEmpty.value = _transacciones.value.isEmpty()
                }
            }

            _isLoading.value = false
        }
    }

    /** Append next page (no-op if already on last page or loading). */
    fun cargarMas() {
        if (_isLastPage.value || _isLoading.value) return
        cargar(refresh = false)
    }

    /** Applies a date range filter (ISO yyyy-MM-dd) and reloads from page 0. */
    fun aplicarFiltroFechas(desde: String, hasta: String) {
        // ISO dates compare lexicographically; guard before hitting the server.
        if (desde > hasta) {
            _mensajeError.value = "El rango de fechas no es válido."
            return
        }
        _filtroFechas.value = desde to hasta
        cargar(refresh = true)
    }

    fun limpiarFiltroFechas() {
        if (_filtroFechas.value == null) return
        _filtroFechas.value = null
        cargar(refresh = true)
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            when (val r = finanzasRepository.listarCategorias()) {
                is ApiResult.Success -> _categorias.value = r.data
                is ApiResult.Error -> { /* non-fatal; spinner will just be empty */ }
            }
        }
    }

    // --- Mutations ---

    fun eliminar(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = finanzasRepository.eliminarTransaccion(id)) {
                is ApiResult.Success -> {
                    // Remove locally first, then refresh
                    _transacciones.value = _transacciones.value.filter { it.id != id }
                    _isEmpty.value = _transacciones.value.isEmpty()
                    // Full refresh to keep pagination consistent
                    cargar(refresh = true)
                }
                is ApiResult.Error -> {
                    _isLoading.value = false
                    _mensajeError.value = mapDeleteError(result.error)
                }
            }
        }
    }

    fun actualizar(
        id: Long,
        monto: Double,
        tipo: String,
        descripcion: String?,
        categoriaId: Long?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = finanzasRepository.actualizarTransaccion(
                id = id,
                monto = monto,
                tipo = tipo,
                descripcion = descripcion?.takeIf { it.isNotBlank() },
                categoriaId = categoriaId
            )) {
                is ApiResult.Success -> {
                    cargar(refresh = true)
                }
                is ApiResult.Error -> {
                    _isLoading.value = false
                    _mensajeError.value = mapUpdateError(result.error)
                }
            }
        }
    }

    fun limpiarError() {
        _mensajeError.value = null
    }

    // --- Error mapping ---

    private fun mapError(error: AppError): String = when (error) {
        is AppError.Api -> when (error.code) {
            ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
            ApiCode.RANGO_FECHAS_INVALIDO -> "El rango de fechas no es válido."
            ApiCode.PARAMETRO_INVALIDO -> "Hay un parámetro inválido en la consulta."
            else -> "Error al cargar transacciones."
        }
        is AppError.Network -> "Sin conexión. Verificá tu internet."
        is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
    }

    private fun mapDeleteError(error: AppError): String = when (error) {
        is AppError.Api -> when (error.code) {
            ApiCode.ACCESO_DENEGADO ->
                "No tenés permiso para eliminar esta transacción."
            ApiCode.TRANSACCION_NO_ENCONTRADA ->
                "La transacción ya no existe."
            ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
            else -> "Error al eliminar la transacción."
        }
        is AppError.Network -> "Sin conexión. Verificá tu internet."
        is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
    }

    private fun mapUpdateError(error: AppError): String = when (error) {
        is AppError.Api -> when (error.code) {
            ApiCode.ACCESO_DENEGADO ->
                "No tenés permiso para editar esta transacción."
            ApiCode.TRANSACCION_NO_ENCONTRADA ->
                "La transacción ya no existe."
            ApiCode.VALIDATION_ERROR -> "Datos inválidos. Revisá los campos."
            ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
            else -> "Error al actualizar la transacción."
        }
        is AppError.Network -> "Sin conexión. Verificá tu internet."
        is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
    }
}
