package com.example.finanzas_independientes_app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzas_independientes_app.core.network.ApiResult
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.repository.FinanzasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Backs the calendar day-detail. The backend has no per-date transaction endpoint,
 * so real figures exist only for the current week (weekly summary) with the daily
 * goal as the "estimate". Any other day resolves to [DayDetail.conDatos] = false.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val finanzasRepository: FinanzasRepository
) : ViewModel() {

    private val _cargado = MutableStateFlow(false)
    val cargado: StateFlow<Boolean> = _cargado

    private var resumenSemanal: List<ResumenSemanalDia> = emptyList()
    private var metaDiaria: Double = 0.0

    fun cargar() {
        viewModelScope.launch {
            when (val r = finanzasRepository.obtenerResumenSemanal()) {
                is ApiResult.Success -> resumenSemanal = r.data
                is ApiResult.Error -> resumenSemanal = emptyList()
            }
            when (val r = finanzasRepository.obtenerProgresoMetas()) {
                is ApiResult.Success -> metaDiaria = r.data.metaDiaria
                is ApiResult.Error -> metaDiaria = 0.0
            }
            _cargado.value = true
        }
    }

    /** Resolves the detail for a picked date (CalendarView gives 0-based month). */
    fun detalleDe(year: Int, month: Int, dayOfMonth: Int): DayDetail {
        val selected = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            clear()
            set(year, month, dayOfMonth)
        }
        val today = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }

        val mismaSemana = selected.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            selected.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR)

        // Calendar.DAY_OF_WEEK: Sunday=1..Saturday=7 → Monday-based index 0..6.
        val dayIndex = (selected.get(Calendar.DAY_OF_WEEK) + 5) % 7

        return if (mismaSemana && dayIndex < resumenSemanal.size) {
            val dia = resumenSemanal[dayIndex]
            DayDetail(
                ingresos = dia.ingresos,
                egresos = dia.egresos,
                estimado = metaDiaria,
                conDatos = true
            )
        } else {
            DayDetail(conDatos = false)
        }
    }
}

data class DayDetail(
    val ingresos: Double = 0.0,
    val egresos: Double = 0.0,
    val estimado: Double = 0.0,
    val conDatos: Boolean
)
