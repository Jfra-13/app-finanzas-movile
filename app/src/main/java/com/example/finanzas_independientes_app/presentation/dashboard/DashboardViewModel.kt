package com.example.finanzas_independientes_app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.ProgresoMetas
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val finanzasRepository: FinanzasRepository
) : ViewModel() {

    // --- Existing flows (kept as-is) ---

    private val _cuotaActual = MutableStateFlow("Calculando...")
    val cuotaActual: StateFlow<String> = _cuotaActual

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    // --- New flows ---

    private val _ingresoHoy = MutableStateFlow("--")
    val ingresoHoy: StateFlow<String> = _ingresoHoy

    private val _progresoMetas = MutableStateFlow<ProgresoMetas?>(null)
    val progresoMetas: StateFlow<ProgresoMetas?> = _progresoMetas

    private val _resumenSemanal = MutableStateFlow<List<ResumenSemanalDia>>(emptyList())
    val resumenSemanal: StateFlow<List<ResumenSemanalDia>> = _resumenSemanal

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- Orchestrator ---

    /** Loads all dashboard data in parallel. Call on screen create and after any mutation. */
    fun cargarResumen() {
        viewModelScope.launch {
            _isLoading.value = true

            val deferredHoy = async { finanzasRepository.obtenerHoy() }
            val deferredCuota = async { finanzasRepository.obtenerCuotaDiaria() }
            val deferredProgreso = async { finanzasRepository.obtenerProgresoMetas() }
            val deferredSemanal = async { finanzasRepository.obtenerResumenSemanal() }

            // today income
            when (val r = deferredHoy.await()) {
                is ApiResult.Success -> _ingresoHoy.value = String.format("%.2f", r.data)
                is ApiResult.Error -> _ingresoHoy.value = handleError(r.error, silent = true) ?: "--"
            }

            // daily quota (same logic as cargarCuotaDiaria)
            when (val r = deferredCuota.await()) {
                is ApiResult.Success -> {
                    val cuota = r.data
                    _cuotaActual.value = if (cuota <= 0) {
                        String.format("¡Meta superada por S/ %.2f! 🎉", kotlin.math.abs(cuota))
                    } else {
                        // Gauge headline — whole soles keep it clean and legible.
                        // Symbol trails the amount; the gauge shrinks the "S/" token.
                        String.format("%.0f S/", cuota)
                    }
                }
                is ApiResult.Error -> {
                    val err = r.error
                    if (err is AppError.Api && err.code == ApiCode.META_NO_ENCONTRADA) {
                        _cuotaActual.value = "Sin meta activa"
                        _mensajeUI.value = "Aún no tienes una meta mensual. ¡Fijá una para activar el motor de metas!"
                    } else {
                        _cuotaActual.value = "Error al calcular"
                        _mensajeUI.value = handleError(err)
                    }
                }
            }

            // goals progress
            when (val r = deferredProgreso.await()) {
                is ApiResult.Success -> _progresoMetas.value = r.data
                is ApiResult.Error -> {
                    val err = r.error
                    if (err is AppError.Api && err.code == ApiCode.META_NO_ENCONTRADA) {
                        _progresoMetas.value = null
                        // message already set above from cuota; avoid duplicate
                    } else {
                        _mensajeUI.value = handleError(err)
                    }
                }
            }

            // weekly summary
            when (val r = deferredSemanal.await()) {
                is ApiResult.Success -> _resumenSemanal.value = r.data
                is ApiResult.Error -> {
                    _mensajeUI.value = handleError(r.error)
                }
            }

            _isLoading.value = false
        }
    }

    /** Kept for backward compat; delegates to cargarResumen. */
    fun cargarCuotaDiaria() {
        cargarResumen()
    }

    /** Load categories for the transaction dialog spinner. */
    fun cargarCategorias() {
        viewModelScope.launch {
            when (val r = finanzasRepository.listarCategorias()) {
                is ApiResult.Success -> _categorias.value = r.data
                is ApiResult.Error -> { /* non-fatal; spinner will just be empty */ }
            }
        }
    }

    // --- Mutations ---

    /**
     * Register a transaction. tipo must be "INGRESO" or "EGRESO".
     * On success refreshes the full dashboard.
     */
    fun registrarTransaccion(
        monto: String,
        tipo: String,
        descripcion: String?,
        categoriaId: Long?
    ) {
        val montoDouble = monto.trim().toDoubleOrNull()
        if (montoDouble == null || montoDouble <= 0) {
            _mensajeUI.value = "Ingresá un monto válido (mayor a 0)"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = finanzasRepository.registrarTransaccion(
                monto = montoDouble,
                tipo = tipo,
                descripcion = descripcion?.takeIf { it.isNotBlank() },
                categoriaId = categoriaId
            )) {
                is ApiResult.Success -> {
                    _mensajeUI.value = if (tipo == "INGRESO") "¡Ingreso registrado!" else "Egreso registrado."
                    cargarResumen()
                }
                is ApiResult.Error -> {
                    _isLoading.value = false
                    _mensajeUI.value = handleError(r.error)
                }
            }
        }
    }

    /** Backward-compat wrapper used by the existing Activity binding. */
    fun registrarIngreso(montoTexto: String) {
        registrarTransaccion(montoTexto, "INGRESO", null, null)
    }

    /**
     * Create or replace the current month's goal.
     * On GOAL_SET refreshes quota + progress.
     */
    fun fijarMeta(montoObjetivo: String, diasLaborables: List<Int>) {
        val monto = montoObjetivo.trim().toDoubleOrNull()
        if (monto == null || monto <= 0) {
            _mensajeUI.value = "Ingresá un monto objetivo válido"
            return
        }
        if (diasLaborables.isEmpty()) {
            _mensajeUI.value = "Seleccioná al menos un día laboral"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = finanzasRepository.fijarMeta(monto, diasLaborables)) {
                is ApiResult.Success -> {
                    _mensajeUI.value = "¡Meta fijada correctamente!"
                    cargarResumen()
                }
                is ApiResult.Error -> {
                    _isLoading.value = false
                    _mensajeUI.value = handleError(r.error)
                }
            }
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }

    // --- Helpers ---

    /**
     * Maps an AppError to a Spanish user-facing string.
     * Returns null when [silent] = true and the error is non-actionable.
     */
    private fun handleError(error: AppError, silent: Boolean = false): String? {
        return when (error) {
            is AppError.Api -> when (error.code) {
                ApiCode.META_NO_ENCONTRADA -> if (silent) null else "Sin meta activa para este período"
                ApiCode.UNAUTHORIZED -> "Sesión expirada. Volvé a iniciar sesión."
                ApiCode.VALIDATION_ERROR -> "Datos inválidos. Revisá los campos."
                else -> if (silent) null else "Error del servidor: ${error.code.raw}"
            }
            is AppError.Network -> if (silent) null else "Sin conexión. Verificá tu internet."
            is AppError.Unexpected -> if (silent) null else "Error inesperado. Intentá de nuevo."
        }
    }
}
