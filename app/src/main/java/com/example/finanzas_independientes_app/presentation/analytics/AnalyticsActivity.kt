package com.example.finanzas_independientes_app.presentation.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finanzas_independientes_app.databinding.ActivityAnalyticsBinding
import com.example.finanzas_independientes_app.presentation.common.ViewStateHelper
import com.example.finanzas_independientes_app.domain.model.ResumenSemanalDia
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.example.finanzas_independientes_app.domain.model.TendenciaMensual
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding

    private val viewModel: AnalyticsViewModel by lazy {
        ViewModelProvider(this)[AnalyticsViewModel::class.java]
    }

    private val healthAdapter = HealthSignalAdapter()
    private lateinit var stateHelper: ViewStateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupChartDefaults()
        stateHelper = ViewStateHelper(binding.viewState) { viewModel.cargar() }
        bindActions()
        bindFlows()

        viewModel.cargar()
    }

    private fun setupRecyclerView() {
        binding.rvHealthSignals.apply {
            layoutManager = LinearLayoutManager(this@AnalyticsActivity)
            adapter = healthAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupChartDefaults() {
        applyBarChartDefaults(binding.barChartWeekly)
        applyLineChartDefaults(binding.lineChartMonthly)
        applyPieChartDefaults(binding.pieChartCategories)
    }

    private fun bindActions() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun bindFlows() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                if (loading) stateHelper.showLoading() else stateHelper.showContent()
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { msg ->
                if (msg != null) {
                    stateHelper.showError(msg) { viewModel.cargar() }
                    Toast.makeText(this@AnalyticsActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarError()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.resumenSemanal.collect { lista ->
                renderWeeklyChart(lista)
            }
        }

        lifecycleScope.launch {
            viewModel.tendenciaMensual.collect { tendencia ->
                renderMonthlyChart(tendencia)
            }
        }

        lifecycleScope.launch {
            viewModel.resumenCategorias.collect { mapa ->
                renderPieChart(mapa)
            }
        }

        lifecycleScope.launch {
            viewModel.saludFinanciera.collect { signals ->
                renderHealthPanel(signals)
            }
        }
    }

    // --- Chart renderers ---

    private fun renderWeeklyChart(lista: List<ResumenSemanalDia>) {
        val nonEmpty = lista.any { it.ingresos > 0 || it.egresos > 0 }
        if (lista.isEmpty() || !nonEmpty) {
            binding.barChartWeekly.visibility = View.GONE
            binding.tvWeeklyEmpty.visibility = View.VISIBLE
            return
        }

        binding.barChartWeekly.visibility = View.VISIBLE
        binding.tvWeeklyEmpty.visibility = View.GONE

        val groupSpace = 0.3f
        val barSpace = 0.05f
        val barWidth = 0.3f

        val ingresosEntries = lista.mapIndexed { i, d -> BarEntry(i.toFloat(), d.ingresos.toFloat()) }
        val egresosEntries = lista.mapIndexed { i, d -> BarEntry(i.toFloat(), d.egresos.toFloat()) }

        val ingresoSet = BarDataSet(ingresosEntries, "Ingresos").apply {
            color = 0xFF4579BC.toInt()  // blue_primary
            valueFormatter = currencyFormatter()
            valueTextSize = 9f
        }

        val egresoSet = BarDataSet(egresosEntries, "Egresos").apply {
            color = 0xFFE57373.toInt()  // soft red
            valueFormatter = currencyFormatter()
            valueTextSize = 9f
        }

        val data = BarData(ingresoSet, egresoSet).apply {
            this.barWidth = barWidth
        }

        binding.barChartWeekly.apply {
            this.data = data
            groupBars(0f, groupSpace, barSpace)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = data.getGroupWidth(groupSpace, barSpace) * lista.size
            xAxis.valueFormatter = IndexAxisValueFormatter(lista.map { it.dia.take(2) })
            animateY(500)
            invalidate()
        }
    }

    private fun renderMonthlyChart(tendencia: TendenciaMensual?) {
        if (tendencia == null || tendencia.meses.isEmpty()) {
            binding.lineChartMonthly.visibility = View.GONE
            binding.tvMonthlyEmpty.visibility = View.VISIBLE
            return
        }

        binding.lineChartMonthly.visibility = View.VISIBLE
        binding.tvMonthlyEmpty.visibility = View.GONE

        val ingresosEntries = tendencia.ingresos.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
        val egresosEntries = tendencia.egresos.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }

        val ingresosSet = LineDataSet(ingresosEntries, "Ingresos").apply {
            color = 0xFF4579BC.toInt()
            setCircleColor(0xFF4579BC.toInt())
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val egresosSet = LineDataSet(egresosEntries, "Egresos").apply {
            color = 0xFFE57373.toInt()
            setCircleColor(0xFFE57373.toInt())
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.lineChartMonthly.apply {
            data = LineData(ingresosSet, egresosSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(tendencia.meses)
            animateX(500)
            invalidate()
        }
    }

    private fun renderPieChart(mapa: Map<String, Double>) {
        val filtered = mapa.filter { it.value > 0 }
        if (filtered.isEmpty()) {
            binding.pieChartCategories.visibility = View.GONE
            binding.tvCategoriesEmpty.visibility = View.VISIBLE
            return
        }

        binding.pieChartCategories.visibility = View.VISIBLE
        binding.tvCategoriesEmpty.visibility = View.GONE

        val pieColors = listOf(
            0xFF4579BC.toInt(), 0xFF2B558C.toInt(), 0xFF7BAFD4.toInt(),
            0xFF5C9BD6.toInt(), 0xFFA0C3D9.toInt(), 0xFF3A6FA8.toInt(),
            0xFF6FB0D8.toInt(), 0xFF8EC6E8.toInt()
        )

        val entries = filtered.entries.mapIndexed { _, (nombre, total) ->
            PieEntry(total.toFloat(), nombre)
        }

        val dataset = PieDataSet(entries, "").apply {
            colors = pieColors.take(entries.size).let {
                if (it.size < entries.size) it + List(entries.size - it.size) { 0xFF888888.toInt() } else it
            }
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            valueFormatter = currencyFormatter()
            sliceSpace = 2f
        }

        binding.pieChartCategories.apply {
            data = PieData(dataset)
            animateY(600)
            invalidate()
        }
    }

    private fun renderHealthPanel(signals: List<SaludFinancieraItem>) {
        if (signals.isEmpty()) {
            binding.rvHealthSignals.visibility = View.GONE
            binding.tvHealthEmpty.visibility = View.VISIBLE
        } else {
            binding.rvHealthSignals.visibility = View.VISIBLE
            binding.tvHealthEmpty.visibility = View.GONE
            healthAdapter.submitList(signals)
        }
    }

    // --- Chart setup helpers ---

    private fun applyBarChartDefaults(chart: BarChart) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
            }
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                valueFormatter = currencyFormatter()
            }
            axisRight.isEnabled = false
            setFitBars(true)
            setExtraOffsets(0f, 0f, 0f, 10f)
        }
    }

    private fun applyLineChartDefaults(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
                labelRotationAngle = -30f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                valueFormatter = currencyFormatter()
            }
            axisRight.isEnabled = false
            setExtraOffsets(0f, 0f, 0f, 10f)
        }
    }

    private fun applyPieChartDefaults(chart: PieChart) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            isDrawHoleEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 45f
            setHoleColor(Color.WHITE)
            centerText = "Egresos"
            setCenterTextSize(14f)
            setCenterTextColor(0xFF555555.toInt())
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            setExtraOffsets(10f, 10f, 10f, 10f)
        }
    }

    private fun currencyFormatter(): ValueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value == 0f) "" else "S/ ${String.format("%.0f", value)}"
        }
    }
}
