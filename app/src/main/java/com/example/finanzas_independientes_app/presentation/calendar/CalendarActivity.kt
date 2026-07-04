package com.example.finanzas_independientes_app.presentation.calendar

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.databinding.ActivityCalendarBinding
import com.example.finanzas_independientes_app.databinding.LayoutBottomNavigationBinding
import com.example.finanzas_independientes_app.presentation.common.AddTransactionDialog
import com.example.finanzas_independientes_app.presentation.common.BottomNav
import com.example.finanzas_independientes_app.presentation.common.QuickAddViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

@AndroidEntryPoint
class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private val bottomNavBinding: LayoutBottomNavigationBinding by lazy {
        LayoutBottomNavigationBinding.bind(binding.root)
    }
    private val viewModel: CalendarViewModel by lazy {
        ViewModelProvider(this)[CalendarViewModel::class.java]
    }
    private val quickAdd: QuickAddViewModel by lazy {
        ViewModelProvider(this)[QuickAddViewModel::class.java]
    }

    private val dateFormat = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyStatusBarInset()
        bindNav()
        bindCalendar()

        viewModel.cargar()
        quickAdd.cargarCategorias()
        lifecycleScope.launch {
            quickAdd.mensajeUI.collect { msg ->
                if (msg != null) {
                    Toast.makeText(this@CalendarActivity, msg, Toast.LENGTH_SHORT).show()
                    quickAdd.limpiarMensaje()
                }
            }
        }
    }

    private fun applyStatusBarInset() {
        val basePaddingTop = binding.calendarScroll.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.calendarScroll) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = basePaddingTop + bars.top)
            insets
        }
    }

    private fun bindCalendar() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            renderDetail(year, month, dayOfMonth)
        }
    }

    private fun renderDetail(year: Int, month: Int, dayOfMonth: Int) {
        val label = Calendar.getInstance().apply { clear(); set(year, month, dayOfMonth) }.time
        binding.tvDetailDate.text = dateFormat.format(label).replaceFirstChar { it.uppercase() }

        val detail = viewModel.detalleDe(year, month, dayOfMonth)
        if (detail.conDatos) {
            binding.tvDetailEmpty.visibility = View.GONE
            binding.detailStatsRow.visibility = View.VISIBLE
            binding.tvDetailIngreso.text = soles(detail.ingresos)
            binding.tvDetailEgreso.text = soles(detail.egresos)
            binding.tvDetailEstimado.text = soles(detail.estimado)
        } else {
            binding.detailStatsRow.visibility = View.GONE
            binding.tvDetailEmpty.visibility = View.VISIBLE
        }
        binding.dayDetailCard.visibility = View.VISIBLE
    }

    private fun bindNav() {
        BottomNav.setup(this, bottomNavBinding, BottomNav.Tab.CALENDAR) {
            AddTransactionDialog.show(this, quickAdd.categorias.value) { monto, tipo, desc, catId ->
                quickAdd.registrarTransaccion(monto, tipo, desc, catId)
            }
        }
    }

    private fun soles(value: Double): String = "S/ ${String.format(Locale.US, "%.0f", value)}"
}
