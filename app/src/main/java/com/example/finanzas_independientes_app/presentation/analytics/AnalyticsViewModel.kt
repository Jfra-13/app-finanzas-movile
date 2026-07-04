package com.example.finanzas_independientes_app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.TendenciaMensual
import com.example.finanzas_independientes_app.domain.model.Transaccion
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val finanzasRepository: FinanzasRepository
) : ViewModel() {

    private val _resumenSemanal = MutableStateFlow<List<ResumenSemanalDia>>(emptyList())
    val resumenSemanal: StateFlow<List<ResumenSemanalDia>> = _resumenSemanal

    private val _tendenciaMensual = MutableStateFlow<TendenciaMensual?>(null)
    val tendenciaMensual: StateFlow<TendenciaMensual?> = _tendenciaMensual

    private val _resumenCategorias = MutableStateFlow<Map<String, Double>>(emptyMap())
    val resumenCategorias: StateFlow<Map<String, Double>> = _resumenCategorias

    private val _saludFinanciera = MutableStateFlow<List<SaludFinancieraItem>>(emptyList())
    val saludFinanciera: StateFlow<List<SaludFinancieraItem>> = _saludFinanciera

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Categories + quick-add, so the shared FAB works from this screen too.
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    /** Months window for the monthly-trend chart; changed by the segmented toggle. */
    private val _mesesTendencia = MutableStateFlow(DEFAULT_MESES)
    val mesesTendencia: StateFlow<Int> = _mesesTendencia

    /** Category name -> id, resolved from the categories list to enable pie drill-down. */
    private var categoriasMap: Map<String, Long> = emptyMap()

    /** Drill-down detail state for the tapped pie slice. */
    private val _detalleCategoria = MutableStateFlow<DetalleCategoriaState>(DetalleCategoriaState.Idle)
    val detalleCategoria: StateFlow<DetalleCategoriaState> = _detalleCategoria

    fun cargar() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val deferredSemanal = async { finanzasRepository.obtenerResumenSemanal() }
            val deferredTendencia = async { finanzasRepository.obtenerTendenciaMensual(_mesesTendencia.value) }
            val deferredCategorias = async { finanzasRepository.obtenerResumenCategorias() }
            val deferredSalud = async { finanzasRepository.obtenerSaludFinanciera() }
            val deferredCats = async { finanzasRepository.listarCategorias() }

            when (val r = deferredSemanal.await()) {
                is ApiResult.Success -> _resumenSemanal.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            when (val r = deferredTendencia.await()) {
                is ApiResult.Success -> _tendenciaMensual.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            when (val r = deferredCategorias.await()) {
                is ApiResult.Success -> _resumenCategorias.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            when (val r = deferredSalud.await()) {
                is ApiResult.Success -> _saludFinanciera.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            // Categories power the pie drill-down and the quick-add spinner; a failure
            // here just disables those, not the screen.
            when (val r = deferredCats.await()) {
                is ApiResult.Success -> {
                    _categorias.value = r.data
                    categoriasMap = r.data.associate { it.nombre to it.id }
                }
                is ApiResult.Error -> categoriasMap = emptyMap()
            }

            _isLoading.value = false
        }
    }

    /** Reloads only the monthly-trend chart with a new months window. */
    fun cambiarMesesTendencia(meses: Int) {
        if (meses == _mesesTendencia.value) return
        _mesesTendencia.value = meses
        viewModelScope.launch {
            when (val r = finanzasRepository.obtenerTendenciaMensual(meses)) {
                is ApiResult.Success -> _tendenciaMensual.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }
        }
    }

    /**
     * Loads the expense transactions behind a pie slice.
     * For a real category we filter server-side by id; for the "Sin categoría"
     * bucket the API has no such filter, so we pull expenses and filter client-side.
     */
    fun cargarDetalleCategoria(nombre: String) {
        val categoriaId = categoriasMap[nombre]
        _detalleCategoria.value = DetalleCategoriaState.Loading(nombre)
        viewModelScope.launch {
            val result = if (categoriaId != null) {
                finanzasRepository.listarTransacciones(categoriaId = categoriaId, size = DETALLE_PAGE_SIZE)
            } else {
                // ponytail: "Sin categoría" filtered client-side on the first page; the API
                // can't filter uncategorized. Add a server filter if this needs full paging.
                finanzasRepository.listarTransacciones(tipo = "EGRESO", size = DETALLE_PAGE_SIZE)
            }
            when (result) {
                is ApiResult.Success -> {
                    val items = if (categoriaId != null) {
                        result.data.content
                    } else {
                        result.data.content.filter { it.categoriaId == null }
                    }
                    _detalleCategoria.value = DetalleCategoriaState.Success(nombre, items)
                }
                is ApiResult.Error ->
                    _detalleCategoria.value = DetalleCategoriaState.Error(nombre, handleError(result.error))
            }
        }
    }

    fun limpiarDetalle() {
        _detalleCategoria.value = DetalleCategoriaState.Idle
    }

    fun limpiarError() {
        _errorMessage.value = null
    }

    /** Quick-add from the shared FAB. On success refreshes the analytics data. */
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
                is ApiResult.Success -> {
                    _mensajeUI.value = if (tipo == "INGRESO") "¡Ingreso registrado!" else "Egreso registrado."
                    cargar()
                }
                is ApiResult.Error -> _mensajeUI.value = handleError(r.error)
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }

    private fun handleError(error: AppError): String {
        return when (error) {
            is AppError.Api -> when (error.code) {
                ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
                ApiCode.META_NO_ENCONTRADA -> "Sin meta activa para este período."
                else -> "Error del servidor: ${error.code.raw}"
            }
            is AppError.Network -> "Sin conexión. Verificá tu internet."
            is AppError.Unexpected -> "Error inesperado. Intentá de nuevo."
        }
    }

    private companion object {
        const val DEFAULT_MESES = 6
        const val DETALLE_PAGE_SIZE = 50
    }
}

/** UI state for the category drill-down bottom sheet. */
sealed interface DetalleCategoriaState {
    data object Idle : DetalleCategoriaState
    data class Loading(val nombre: String) : DetalleCategoriaState
    data class Success(val nombre: String, val transacciones: List<Transaccion>) : DetalleCategoriaState
    data class Error(val nombre: String, val mensaje: String) : DetalleCategoriaState
}
