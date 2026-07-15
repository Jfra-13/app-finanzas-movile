package com.example.finanzas_independientes_app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiCode
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.core.network.AppError
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.ComparacionCategorias
import com.example.finanzas_independientes_app.domain.model.CompararCon
import com.example.finanzas_independientes_app.domain.model.IngresoDiaSemana
import com.example.finanzas_independientes_app.domain.model.Meta
import com.example.finanzas_independientes_app.domain.model.ProyeccionMensual
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.Tendencia
import com.example.finanzas_independientes_app.domain.model.Transaccion
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import com.example.finanzas_independientes_app.domain.usecase.OrdenarSenalesSaludUseCase
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

    private val _ingresosDiaSemana = MutableStateFlow<List<IngresoDiaSemana>>(emptyList())
    val ingresosDiaSemana: StateFlow<List<IngresoDiaSemana>> = _ingresosDiaSemana

    private val _tendencia = MutableStateFlow<Tendencia?>(null)
    val tendencia: StateFlow<Tendencia?> = _tendencia

    private val _resumenCategorias = MutableStateFlow<Map<String, Double>>(emptyMap())
    val resumenCategorias: StateFlow<Map<String, Double>> = _resumenCategorias

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Categories + quick-add, so the shared FAB works from this screen too.
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    /** Period window in months (1/3/6/12) for the trend charts; segmented toggle. */
    private val _periodoMeses = MutableStateFlow(DEFAULT_MESES)

    /** Trend bucketing; SEMANA translates the month window into weeks. */
    private val _granularidad = MutableStateFlow(Granularidad.MES)
    val granularidad: StateFlow<Granularidad> = _granularidad

    /** Weeks window for the weekday-earnings chart (default 4). */
    private val _ventanaSemanas = MutableStateFlow(DEFAULT_VENTANA_SEMANAS)

    /**
     * Active lens: which series the whole screen shows. Pure UI state — the data
     * already carries ingresos and egresos separately, so switching modes only
     * re-renders, it never re-fetches.
     */
    private val _modo = MutableStateFlow(ModoAnalytics.JAM)
    val modo: StateFlow<ModoAnalytics> = _modo

    /** Active monthly goal, drawn as a target line on the income view. Null = none set. */
    private val _metaActual = MutableStateFlow<Meta?>(null)
    val metaActual: StateFlow<Meta?> = _metaActual

    /** Run-rate projection for the month. Distinguishes "no goal" from a real error. */
    private val _proyeccion = MutableStateFlow<ProyeccionState>(ProyeccionState.Idle)
    val proyeccion: StateFlow<ProyeccionState> = _proyeccion

    /** Per-category spend vs the comparison window. Null until loaded / on failure. */
    private val _comparacion = MutableStateFlow<ComparacionCategorias?>(null)
    val comparacion: StateFlow<ComparacionCategorias?> = _comparacion

    /** Comparison basis for the current-vs-previous view. */
    private val _compararCon = MutableStateFlow(CompararCon.PERIODO_ANTERIOR)
    val compararCon: StateFlow<CompararCon> = _compararCon

    /** Health signals, already ordered most-urgent first. */
    private val _salud = MutableStateFlow<List<SaludFinancieraItem>>(emptyList())
    val salud: StateFlow<List<SaludFinancieraItem>> = _salud

    private val ordenarSalud = OrdenarSenalesSaludUseCase()

    /** Category name -> id, resolved from the categories list to enable pie drill-down. */
    private var categoriasMap: Map<String, Long> = emptyMap()

    /** Drill-down detail state for the tapped pie slice. */
    private val _detalleCategoria = MutableStateFlow<DetalleCategoriaState>(DetalleCategoriaState.Idle)
    val detalleCategoria: StateFlow<DetalleCategoriaState> = _detalleCategoria

    fun cargar() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val deferredDiaSemana =
                async { finanzasRepository.obtenerIngresosPorDiaSemana(_ventanaSemanas.value) }
            val deferredTendencia = async {
                finanzasRepository.obtenerTendencia(_granularidad.value.name, ventanaTendencia())
            }
            val deferredCategorias = async { finanzasRepository.obtenerResumenCategorias() }
            val deferredCats = async { finanzasRepository.listarCategorias() }
            val deferredMeta = async { finanzasRepository.obtenerMetaActual() }
            val deferredProyeccion = async { finanzasRepository.obtenerProyeccionMensual() }
            val deferredComparacion =
                async { finanzasRepository.obtenerComparacionCategorias(compararCon = _compararCon.value) }
            val deferredSalud = async { finanzasRepository.obtenerSaludFinanciera() }

            when (val r = deferredDiaSemana.await()) {
                is ApiResult.Success -> _ingresosDiaSemana.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            when (val r = deferredTendencia.await()) {
                is ApiResult.Success -> _tendencia.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }

            when (val r = deferredCategorias.await()) {
                is ApiResult.Success -> _resumenCategorias.value = r.data
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

            // Optional target line. "No goal set" is a normal state, not an error,
            // so it never reaches the error channel — it just leaves the line off.
            _metaActual.value = when (val r = deferredMeta.await()) {
                is ApiResult.Success -> r.data
                is ApiResult.Error -> null
            }

            // Secondary insights: a failure here shows an empty section, it never
            // takes over the screen's error channel (the charts own that).
            _proyeccion.value = when (val r = deferredProyeccion.await()) {
                is ApiResult.Success -> ProyeccionState.Data(r.data)
                is ApiResult.Error ->
                    if (r.error is AppError.Api && r.error.code == ApiCode.META_NO_ENCONTRADA)
                        ProyeccionState.SinMeta else ProyeccionState.Idle
            }
            _comparacion.value = when (val r = deferredComparacion.await()) {
                is ApiResult.Success -> r.data
                is ApiResult.Error -> null
            }
            _salud.value = when (val r = deferredSalud.await()) {
                is ApiResult.Success -> ordenarSalud(r.data)
                is ApiResult.Error -> emptyList()
            }

            _isLoading.value = false
        }
    }

    /** Switches the active lens. UI-only: re-renders from data already in memory. */
    fun cambiarModo(modo: ModoAnalytics) {
        _modo.value = modo
    }

    /** Reloads only the trend charts with a new period window (months: 1/3/6/12). */
    fun cambiarPeriodo(meses: Int) {
        if (meses == _periodoMeses.value) return
        _periodoMeses.value = meses
        recargarTendencia()
    }

    /** Switches trend bucketing between MES and SEMANA and refetches. */
    fun cambiarGranularidad(granularidad: Granularidad) {
        if (granularidad == _granularidad.value) return
        _granularidad.value = granularidad
        recargarTendencia()
    }

    /** Reloads only the weekday-earnings chart with a new weeks window. */
    fun cambiarVentanaSemanas(semanas: Int) {
        if (semanas == _ventanaSemanas.value) return
        _ventanaSemanas.value = semanas
        viewModelScope.launch {
            when (val r = finanzasRepository.obtenerIngresosPorDiaSemana(semanas)) {
                is ApiResult.Success -> _ingresosDiaSemana.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }
        }
    }

    /** Switches the comparison basis and reloads only the comparison section. */
    fun cambiarComparacion(base: CompararCon) {
        if (base == _compararCon.value) return
        _compararCon.value = base
        viewModelScope.launch {
            _comparacion.value = when (
                val r = finanzasRepository.obtenerComparacionCategorias(compararCon = base)
            ) {
                is ApiResult.Success -> r.data
                is ApiResult.Error -> null
            }
        }
    }

    private fun recargarTendencia() {
        viewModelScope.launch {
            when (val r =
                finanzasRepository.obtenerTendencia(_granularidad.value.name, ventanaTendencia())
            ) {
                is ApiResult.Success -> _tendencia.value = r.data
                is ApiResult.Error -> _errorMessage.value = handleError(r.error)
            }
        }
    }

    /** The month window expressed in the active granularity's periods. */
    private fun ventanaTendencia(): Int = when (_granularidad.value) {
        Granularidad.MES -> _periodoMeses.value
        Granularidad.SEMANA -> SEMANAS_POR_MES.getValue(_periodoMeses.value)
    }

    /**
     * Loads the expense transactions behind a pie slice.
     * A real category filters server-side by id; the "Sin categoría" bucket uses
     * the server's `sinCategoria=true` filter. The two are mutually exclusive, so
     * the branch guarantees they are never sent together (400 PARAMETRO_INVALIDO).
     */
    fun cargarDetalleCategoria(nombre: String) {
        val categoriaId = categoriasMap[nombre]
        _detalleCategoria.value = DetalleCategoriaState.Loading(nombre)
        viewModelScope.launch {
            val result = if (categoriaId != null) {
                finanzasRepository.listarTransacciones(categoriaId = categoriaId, size = DETALLE_PAGE_SIZE)
            } else {
                finanzasRepository.listarTransacciones(
                    tipo = "EGRESO",
                    sinCategoria = true,
                    size = DETALLE_PAGE_SIZE
                )
            }
            when (result) {
                is ApiResult.Success ->
                    _detalleCategoria.value = DetalleCategoriaState.Success(nombre, result.data.content)
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
        const val DEFAULT_VENTANA_SEMANAS = 4
        const val DETALLE_PAGE_SIZE = 50

        /** 1M/3M/6M/1Y expressed in whole weeks for weekly bucketing. */
        val SEMANAS_POR_MES = mapOf(1 to 4, 3 to 13, 6 to 26, 12 to 52)
    }
}

/** Trend bucketing accepted by GET /tendencia; enum names match the API values. */
enum class Granularidad { SEMANA, MES }

/**
 * The active lens for the analytics screen.
 * INGRESOS / EGRESOS show a single series; JAM ("juntos") overlays both so the
 * gap between them reads as the margin.
 */
enum class ModoAnalytics { INGRESOS, EGRESOS, JAM }

/** UI state for the monthly projection card. Separates "no goal" from a load failure. */
sealed interface ProyeccionState {
    data object Idle : ProyeccionState
    data object SinMeta : ProyeccionState
    data class Data(val proyeccion: ProyeccionMensual) : ProyeccionState
}

/** UI state for the category drill-down bottom sheet. */
sealed interface DetalleCategoriaState {
    data object Idle : DetalleCategoriaState
    data class Loading(val nombre: String) : DetalleCategoriaState
    data class Success(val nombre: String, val transacciones: List<Transaccion>) : DetalleCategoriaState
    data class Error(val nombre: String, val mensaje: String) : DetalleCategoriaState
}
