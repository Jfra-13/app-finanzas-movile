package com.example.finanzas_independientes_app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.ResumenDiarioDia
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Backs the calendar day-detail with GET /finanzas/resumen-diario, cached per
 * month for the lifetime of the screen. A day absent from the summary simply
 * had no activity (zeros); [DayDetail.conDatos] = false only when the month
 * could not be fetched.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val finanzasRepository: FinanzasRepository
) : ViewModel() {

    private val _detalle = MutableStateFlow<DayDetail?>(null)
    val detalle: StateFlow<DayDetail?> = _detalle

    // "yyyy-MM" -> day totals keyed by "yyyy-MM-dd". Failed fetches are not cached.
    private val meses = mutableMapOf<String, Map<String, ResumenDiarioDia>>()
    private var metaDiaria: Double = 0.0

    fun cargar() {
        viewModelScope.launch {
            when (val r = finanzasRepository.obtenerProgresoMetas()) {
                is ApiResult.Success -> metaDiaria = r.data.metaDiaria
                is ApiResult.Error -> metaDiaria = 0.0
            }
            cargarMes(mesActual())
        }
    }

    /** Resolves the detail for a picked date (CalendarView gives 0-based month). */
    fun seleccionar(year: Int, month: Int, dayOfMonth: Int) {
        val mes = String.format(Locale.US, "%04d-%02d", year, month + 1)
        val fecha = String.format(Locale.US, "%s-%02d", mes, dayOfMonth)
        viewModelScope.launch {
            val dias = cargarMes(mes)
            _detalle.value = if (dias != null) {
                val dia = dias[fecha]
                DayDetail(
                    ingresos = dia?.ingresos ?: 0.0,
                    egresos = dia?.egresos ?: 0.0,
                    estimado = metaDiaria,
                    conDatos = true
                )
            } else {
                DayDetail(conDatos = false)
            }
        }
    }

    private suspend fun cargarMes(mes: String): Map<String, ResumenDiarioDia>? {
        meses[mes]?.let { return it }
        return when (val r = finanzasRepository.obtenerResumenDiario(mes)) {
            is ApiResult.Success -> r.data.associateBy { it.fecha }.also { meses[mes] = it }
            is ApiResult.Error -> null
        }
    }

    private fun mesActual(): String {
        val hoy = Calendar.getInstance()
        return String.format(
            Locale.US, "%04d-%02d", hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH) + 1
        )
    }
}

data class DayDetail(
    val ingresos: Double = 0.0,
    val egresos: Double = 0.0,
    val estimado: Double = 0.0,
    val conDatos: Boolean
)
