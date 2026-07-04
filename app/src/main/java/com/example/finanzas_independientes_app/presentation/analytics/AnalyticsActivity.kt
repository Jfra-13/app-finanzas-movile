package com.example.finanzas_independientes_app.presentation.analytics

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ActivityAnalyticsBinding
import com.example.finanzas_independientes_app.databinding.BottomSheetCategoriaDetalleBinding
import com.example.finanzas_independientes_app.databinding.LayoutBottomNavigationBinding
import com.example.finanzas_independientes_app.presentation.common.AddTransactionDialog
import com.example.finanzas_independientes_app.presentation.common.BottomNav
import com.example.finanzas_independientes_app.presentation.common.ViewStateHelper
import com.example.finanzas_independientes_app.presentation.transacciones.TransaccionAdapter
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
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private val bottomNavBinding: LayoutBottomNavigationBinding by lazy {
        LayoutBottomNavigationBinding.bind(binding.root)
    }

    private val viewModel: AnalyticsViewModel by lazy {
        ViewModelProvider(this)[AnalyticsViewModel::class.java]
    }

    private val healthAdapter = HealthSignalAdapter()
    private val detalleAdapter = TransaccionAdapter()  // read-only: no edit/delete
    private lateinit var stateHelper: ViewStateHelper

    private var detalleDialog: BottomSheetDialog? = null
    private var detalleBinding: BottomSheetCategoriaDetalleBinding? = null

    /** Looping pulse on the celebratory "all good" badge; started only while shown. */
    private var healthPulse: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupChartDefaults()
        stateHelper = ViewStateHelper(binding.viewState) { viewModel.cargar() }
        applyWindowInsets()
        bindGlassScrim()
        bindActions()
        bindFlows()

        viewModel.cargar()
    }

    override fun onDestroy() {
        healthPulse?.cancel()
        dismissDetalle()
        super.onDestroy()
    }

    // Edge-to-edge draws the status bar over the fixed top bar, which pushed the
    // back button under the status bar (unreachable). Push the status-bar inset
    // into the bar's paddingTop, and start the scroll content below the bar's
    // full height so nothing is ever hidden behind it.
    private fun applyWindowInsets() {
        val baseTop = binding.topBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = baseTop + bars.top)
            insets
        }
        binding.topBar.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val h = v.height
            if (binding.contentScroll.paddingTop != h) {
                binding.contentScroll.updatePadding(top = h)
            }
            if (binding.viewState.root.paddingTop != h) {
                binding.viewState.root.updatePadding(top = h)
            }
        }
    }

    // Glass scrim: the top bar's background starts transparent and fades in as the
    // user scrolls, so content slides cleanly under a frosted bar. (View system has
    // no real backdrop blur — this is an alpha scrim, same trick as the dashboard.)
    private fun bindGlassScrim() {
        binding.topBar.background?.mutate()?.alpha = 0
        val threshold = dp(48f)
        binding.contentScroll.setOnScrollChangeListener(
            androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                val fraction = (scrollY / threshold).coerceIn(0f, 1f)
                binding.topBar.background?.alpha = (fraction * GLASS_ALPHA * 255).toInt()
            }
        )
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

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

        // Shared bottom nav: this screen is the STATS tab; FAB does the quick-add.
        BottomNav.setup(this, bottomNavBinding, BottomNav.Tab.STATS) {
            AddTransactionDialog.show(this, viewModel.categorias.value) { monto, tipo, descripcion, categoriaId ->
                viewModel.registrarTransaccion(monto, tipo, descripcion, categoriaId)
            }
        }

        // Monthly-trend months selector — check default before wiring to avoid a spurious reload.
        binding.toggleMeses.check(R.id.btnMeses6)
        binding.toggleMeses.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val meses = when (checkedId) {
                R.id.btnMeses3 -> 3
                R.id.btnMeses12 -> 12
                else -> 6
            }
            viewModel.cambiarMesesTendencia(meses)
        }

        // Tap a pie slice -> drill down to that category's expense transactions.
        binding.pieChartCategories.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                (e as? PieEntry)?.label?.let { viewModel.cargarDetalleCategoria(it) }
            }

            override fun onNothingSelected() {}
        })
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

        lifecycleScope.launch {
            viewModel.detalleCategoria.collect { state ->
                renderDetalle(state)
            }
        }

        lifecycleScope.launch {
            viewModel.mensajeUI.collect { msg ->
                if (msg != null) {
                    Toast.makeText(this@AnalyticsActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                }
            }
        }
    }

    // --- Category drill-down bottom sheet ---

    private fun renderDetalle(state: DetalleCategoriaState) {
        when (state) {
            is DetalleCategoriaState.Idle -> dismissDetalle()
            is DetalleCategoriaState.Loading -> {
                val b = ensureDetalleSheet(state.nombre)
                b.tvSheetTitle.text = state.nombre
                b.sheetProgress.visibility = View.VISIBLE
                b.rvSheetTransacciones.visibility = View.GONE
                b.tvSheetState.visibility = View.GONE
            }
            is DetalleCategoriaState.Success -> {
                val b = ensureDetalleSheet(state.nombre)
                b.sheetProgress.visibility = View.GONE
                if (state.transacciones.isEmpty()) {
                    b.rvSheetTransacciones.visibility = View.GONE
                    b.tvSheetState.visibility = View.VISIBLE
                    b.tvSheetState.text = "Sin movimientos en esta categoría"
                } else {
                    b.tvSheetState.visibility = View.GONE
                    b.rvSheetTransacciones.visibility = View.VISIBLE
                    b.tvSheetSubtitle.text = "${state.transacciones.size} movimiento(s)"
                    detalleAdapter.submitList(state.transacciones)
                }
            }
            is DetalleCategoriaState.Error -> {
                val b = ensureDetalleSheet(state.nombre)
                b.sheetProgress.visibility = View.GONE
                b.rvSheetTransacciones.visibility = View.GONE
                b.tvSheetState.visibility = View.VISIBLE
                b.tvSheetState.text = state.mensaje
            }
        }
    }

    private fun ensureDetalleSheet(nombre: String): BottomSheetCategoriaDetalleBinding {
        detalleBinding?.let { existing ->
            existing.tvSheetTitle.text = nombre
            detalleDialog?.show()
            return existing
        }
        val b = BottomSheetCategoriaDetalleBinding.inflate(layoutInflater)
        b.tvSheetTitle.text = nombre
        b.rvSheetTransacciones.apply {
            layoutManager = LinearLayoutManager(this@AnalyticsActivity)
            adapter = detalleAdapter
        }
        val dialog = BottomSheetDialog(this).apply {
            setContentView(b.root)
            setOnDismissListener { viewModel.limpiarDetalle() }
        }
        detalleBinding = b
        detalleDialog = dialog
        dialog.show()
        return b
    }

    private fun dismissDetalle() {
        detalleDialog?.let { d ->
            // Avoid re-triggering limpiarDetalle() through the dismiss listener.
            d.setOnDismissListener(null)
            if (d.isShowing) d.dismiss()
        }
        detalleDialog = null
        detalleBinding = null
        // Clear the slice highlight so tapping the same slice re-opens the sheet.
        binding.pieChartCategories.highlightValues(null)
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
            color = color(R.color.blue_primary)
            valueTextColor = color(R.color.on_surface)
            valueFormatter = currencyFormatter()
            valueTextSize = 9f
        }

        val egresoSet = BarDataSet(egresosEntries, "Egresos").apply {
            color = color(R.color.chart_egreso)
            valueTextColor = color(R.color.on_surface)
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
            animateY(800, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
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
            color = color(R.color.blue_primary)
            setCircleColor(color(R.color.blue_primary))
            setDrawCircleHole(false)
            lineWidth = 2.5f
            circleRadius = 3.5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@AnalyticsActivity, R.drawable.fade_chart_blue)
        }

        val egresosSet = LineDataSet(egresosEntries, "Egresos").apply {
            color = color(R.color.chart_egreso)
            setCircleColor(color(R.color.chart_egreso))
            setDrawCircleHole(false)
            lineWidth = 2.5f
            circleRadius = 3.5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@AnalyticsActivity, R.drawable.fade_chart_red)
        }

        binding.lineChartMonthly.apply {
            data = LineData(ingresosSet, egresosSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(tendencia.meses)
            animateX(700)
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
            valueTextColor = color(R.color.on_surface)
            valueFormatter = currencyFormatter()
            sliceSpace = 3f
            selectionShift = 6f
            // Thin donut ring can't hold currency labels — draw them outside with connectors.
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.5f
            valueLineColor = color(R.color.outline)
        }

        binding.pieChartCategories.apply {
            data = PieData(dataset)
            animateY(800, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun renderHealthPanel(signals: List<SaludFinancieraItem>) {
        if (signals.isEmpty()) {
            binding.rvHealthSignals.visibility = View.GONE
            binding.healthCelebration.visibility = View.VISIBLE
            startHealthPulse()
        } else {
            binding.rvHealthSignals.visibility = View.VISIBLE
            binding.healthCelebration.visibility = View.GONE
            stopHealthPulse()
            healthAdapter.submitList(signals)
        }
    }

    // Gentle looping heartbeat on the celebration emoji — the reward moment (Peak-End).
    private fun startHealthPulse() {
        if (healthPulse?.isRunning == true) return
        healthPulse = ObjectAnimator.ofPropertyValuesHolder(
            binding.tvHealthEmoji,
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.18f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.18f)
        ).apply {
            duration = 700
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopHealthPulse() {
        healthPulse?.cancel()
        healthPulse = null
        binding.tvHealthEmoji.scaleX = 1f
        binding.tvHealthEmoji.scaleY = 1f
    }

    // --- Chart setup helpers ---

    private fun applyBarChartDefaults(chart: BarChart) {
        val onSurface = color(R.color.on_surface)
        val gridColor = color(R.color.outline_variant)
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = onSurface
            setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
                textColor = onSurface
            }
            axisLeft.apply {
                setDrawGridLines(true)
                this.gridColor = gridColor
                axisMinimum = 0f
                textColor = onSurface
                valueFormatter = currencyFormatter()
            }
            axisRight.isEnabled = false
            setFitBars(true)
            setExtraOffsets(0f, 0f, 0f, 10f)
        }
    }

    private fun applyLineChartDefaults(chart: LineChart) {
        val onSurface = color(R.color.on_surface)
        val gridColor = color(R.color.outline_variant)
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = onSurface
            setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
                labelRotationAngle = -30f
                textColor = onSurface
            }
            axisLeft.apply {
                setDrawGridLines(true)
                this.gridColor = gridColor
                axisMinimum = 0f
                textColor = onSurface
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
            legend.textColor = color(R.color.on_surface)
            legend.isWordWrapEnabled = true
            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 62f
            setHoleColor(color(R.color.surface))
            setTransparentCircleColor(color(R.color.surface))
            setTransparentCircleAlpha(90)
            centerText = "Egresos"
            setCenterTextSize(14f)
            setCenterTextColor(color(R.color.on_surface_variant))
            // Slice labels clutter a small donut — the legend already names them.
            setDrawEntryLabels(false)
            setUsePercentValues(false)
            setExtraOffsets(12f, 8f, 12f, 8f)
            isRotationEnabled = false
        }
    }

    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    private fun currencyFormatter(): ValueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value == 0f) "" else "S/ ${String.format("%.0f", value)}"
        }
    }

    private companion object {
        /** Peak opacity of the frosted top-bar scrim once fully scrolled. */
        const val GLASS_ALPHA = 0.92f
    }
}
