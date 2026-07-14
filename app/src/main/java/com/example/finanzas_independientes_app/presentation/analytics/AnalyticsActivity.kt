package com.example.finanzas_independientes_app.presentation.analytics

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
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
import com.example.finanzas_independientes_app.domain.model.IngresoDiaSemana
import com.example.finanzas_independientes_app.domain.model.Meta
import com.example.finanzas_independientes_app.domain.model.Tendencia
import com.example.finanzas_independientes_app.domain.usecase.ProyectarTendenciaUseCase
import com.patrykandpatrick.vico.views.cartesian.CartesianChart
import com.patrykandpatrick.vico.views.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.views.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.views.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.views.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.views.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.views.cartesian.data.columnSeries
import com.patrykandpatrick.vico.views.cartesian.data.lineSeries
import com.patrykandpatrick.vico.views.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.views.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.views.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.views.common.Fill
import com.patrykandpatrick.vico.views.common.component.LineComponent
import com.patrykandpatrick.vico.views.common.component.TextComponent
import com.patrykandpatrick.vico.views.common.data.ExtraStore
import com.patrykandpatrick.vico.views.common.shader.ShaderProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private val bottomNavBinding: LayoutBottomNavigationBinding by lazy {
        LayoutBottomNavigationBinding.bind(binding.root)
    }

    private val viewModel: AnalyticsViewModel by lazy {
        ViewModelProvider(this)[AnalyticsViewModel::class.java]
    }

    private val detalleAdapter = TransaccionAdapter()  // read-only: no edit/delete
    private val categoryAdapter = CategoryRankingAdapter { nombre ->
        viewModel.cargarDetalleCategoria(nombre)
    }
    private lateinit var stateHelper: ViewStateHelper

    // One producer per Vico chart. Data is pushed via runTransaction on each render.
    private val balanceProducer = CartesianChartModelProducer()
    private val netoProducer = CartesianChartModelProducer()
    private val weekdayProducer = CartesianChartModelProducer()

    // Axis label lists read by the formatters at draw time; refreshed before each
    // transaction so labels always match the series currently plotted.
    private var monthLabels: List<String> = emptyList()
    private var weekdayLabels: List<String> = emptyList()

    // Latest data cached so a mode switch can re-render without a re-fetch.
    private var tendencia: Tendencia? = null
    private var diaSemana: List<IngresoDiaSemana> = emptyList()
    private var categorias: Map<String, Double> = emptyMap()
    private var meta: Meta? = null

    private val proyectar = ProyectarTendenciaUseCase()

    // Adds ~15% headroom above the tallest bar/point (and below the deepest
    // deficit) so top value labels never clip — the "espacio arriba" fix.
    private val headroomRange = object : CartesianLayerRangeProvider {
        override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore) =
            if (minY < 0) minY * HEADROOM else 0.0
        override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore) =
            if (maxY > 0) maxY * HEADROOM else 0.0
    }

    // Reusable chart components (theme-correct colours), built once.
    private lateinit var axisLabel: TextComponent
    private lateinit var monthFormatter: CartesianValueFormatter
    private lateinit var weekdayFormatter: CartesianValueFormatter
    private lateinit var currencyFormatter: CartesianValueFormatter

    private var detalleDialog: BottomSheetDialog? = null
    private var detalleBinding: BottomSheetCategoriaDetalleBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupCharts()
        stateHelper = ViewStateHelper(binding.viewState) { viewModel.cargar() }
        applyWindowInsets()
        bindGlassScrim()
        bindActions()
        bindFlows()

        viewModel.cargar()
    }

    override fun onDestroy() {
        dismissDetalle()
        super.onDestroy()
    }

    // Edge-to-edge draws the status bar over the fixed top bar. Push the status-bar
    // inset into the bar's paddingTop, and start the scroll content below the bar's
    // full (measured) height so nothing is ever hidden behind it.
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
    // user scrolls, so content slides cleanly under a frosted bar.
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

    private fun setupRecyclerViews() {
        binding.rvCategoryRanking.apply {
            layoutManager = LinearLayoutManager(this@AnalyticsActivity)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    // --- Vico chart setup ---

    private fun setupCharts() {
        // Explicit label components so axis text is visible in BOTH light and dark
        // (Vico's default label colour does not follow our theme).
        axisLabel = TextComponent(color = color(R.color.on_surface_variant), textSizeSp = 10f)

        // Vico probes out-of-range x-values while measuring the axis (getMaxLabelWidth)
        // and rejects blank results, so never return "": fall back to the raw index,
        // which only surfaces during measurement, never on a drawn label.
        monthFormatter = CartesianValueFormatter { _, value, _ ->
            monthLabels.getOrNull(value.toInt()) ?: value.toInt().toString()
        }
        weekdayFormatter = CartesianValueFormatter { _, value, _ ->
            weekdayLabels.getOrNull(value.toInt()) ?: value.toInt().toString()
        }
        currencyFormatter = CartesianValueFormatter { _, value, _ -> formatShort(value) }

        // Line + column charts are (re)configured per render so colours match the
        // active mode; the weekday column chart is static.
        binding.chartBalance.modelProducer = balanceProducer
        binding.chartNeto.modelProducer = netoProducer

        binding.chartWeekday.modelProducer = weekdayProducer
        binding.chartWeekday.chart = CartesianChart(
            ColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    LineComponent(Fill(color(R.color.success)), COLUMN_THICKNESS_DP)
                ),
                dataLabel = dataLabel(R.color.success),
                dataLabelValueFormatter = currencyFormatter,
                rangeProvider = headroomRange,
            ),
            startAxis = VerticalAxis.start(label = axisLabel, valueFormatter = currencyFormatter),
            bottomAxis = HorizontalAxis.bottom(label = axisLabel, valueFormatter = weekdayFormatter),
        )
    }

    /** Horizontal target line ("Meta establecida") anchored at the goal amount. */
    private fun metaLine(y: Double): HorizontalLine {
        val fill = Fill(color(R.color.blue_primary))
        return HorizontalLine(
            { y },
            LineComponent(fill, META_LINE_THICKNESS_DP),
            TextComponent(color = color(R.color.blue_primary), textSizeSp = 10f),
            { "Meta" },
        )
    }

    private fun dataLabel(colorRes: Int): TextComponent =
        TextComponent(color = color(colorRes), textSizeSp = 11f)

    /** A single line with the given colour, optional soft area fill, coloured value labels. */
    private fun buildLine(colorRes: Int, withArea: Boolean): LineCartesianLayer.Line {
        val c = color(colorRes)
        val areaFill = if (withArea) {
            LineCartesianLayer.AreaFill.single(
                Fill(
                    ShaderProvider.verticalGradient(
                        ColorUtils.setAlphaComponent(c, 90),
                        ColorUtils.setAlphaComponent(c, 0)
                    )
                )
            )
        } else null
        return LineCartesianLayer.Line(
            fill = LineCartesianLayer.LineFill.single(Fill(c)),
            areaFill = areaFill,
            dataLabel = dataLabel(colorRes),
            dataLabelValueFormatter = currencyFormatter,
        )
    }

    private fun lineChart(
        vararg lines: LineCartesianLayer.Line,
        decorations: List<HorizontalLine> = emptyList(),
    ): CartesianChart =
        CartesianChart(
            LineCartesianLayer(
                LineCartesianLayer.LineProvider.series(lines.toList()),
                rangeProvider = headroomRange,
            ),
            startAxis = VerticalAxis.start(label = axisLabel, valueFormatter = currencyFormatter),
            bottomAxis = HorizontalAxis.bottom(label = axisLabel, valueFormatter = monthFormatter),
            decorations = decorations,
        )

    private fun bindActions() {
        // Shared bottom nav: this screen is the STATS tab; FAB does the quick-add.
        BottomNav.setup(this, bottomNavBinding, BottomNav.Tab.STATS) {
            AddTransactionDialog.show(this, viewModel.categorias.value) { monto, tipo, descripcion, categoriaId ->
                viewModel.registrarTransaccion(monto, tipo, descripcion, categoriaId)
            }
        }

        // Lens selector — pure UI switch, re-renders from cached data.
        binding.toggleModo.check(R.id.btnModoJam)
        binding.toggleModo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val modo = when (checkedId) {
                R.id.btnModoIngresos -> ModoAnalytics.INGRESOS
                R.id.btnModoEgresos -> ModoAnalytics.EGRESOS
                else -> ModoAnalytics.JAM
            }
            viewModel.cambiarModo(modo)
        }

        // Period window — drives both trend charts. Check default before wiring
        // to avoid a spurious reload.
        binding.togglePeriodo.check(R.id.btnPeriodo6M)
        binding.togglePeriodo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val meses = when (checkedId) {
                R.id.btnPeriodo1M -> 1
                R.id.btnPeriodo3M -> 3
                R.id.btnPeriodo1Y -> 12
                else -> 6
            }
            viewModel.cambiarPeriodo(meses)
        }

        // Trend bucketing: month vs week (weeks are labeled by their Monday).
        binding.toggleGranularidad.check(R.id.btnGranMes)
        binding.toggleGranularidad.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.cambiarGranularidad(
                if (checkedId == R.id.btnGranSemana) Granularidad.SEMANA else Granularidad.MES
            )
        }

        // Weeks window for the weekday-earnings chart.
        binding.toggleVentanaSemanas.check(R.id.btnVentana4S)
        binding.toggleVentanaSemanas.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.cambiarVentanaSemanas(if (checkedId == R.id.btnVentana12S) 12 else 4)
        }
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
            viewModel.tendencia.collect {
                tendencia = it
                renderAll()
            }
        }

        lifecycleScope.launch {
            viewModel.ingresosDiaSemana.collect {
                diaSemana = it
                renderAll()
            }
        }

        lifecycleScope.launch {
            viewModel.resumenCategorias.collect {
                categorias = it
                renderAll()
            }
        }

        lifecycleScope.launch {
            viewModel.metaActual.collect {
                meta = it
                renderAll()
            }
        }

        lifecycleScope.launch {
            viewModel.modo.collect { renderAll() }
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

    // --- Render dispatch ---

    private fun renderAll() {
        val modo = viewModel.modo.value
        renderBalance(modo, tendencia)
        renderNeto(modo, tendencia)
        renderThird(modo, diaSemana, categorias)
    }

    // --- Chart renderers ---

    private fun renderBalance(modo: ModoAnalytics, t: Tendencia?) {
        if (t == null || t.periodos.isEmpty()) {
            binding.chartBalance.visibility = View.GONE
            binding.tvBalanceEmpty.visibility = View.VISIBLE
            return
        }
        binding.chartBalance.visibility = View.VISIBLE
        binding.tvBalanceEmpty.visibility = View.GONE
        monthLabels = t.periodos.map(::shortPeriod)

        binding.tvBalanceSubtitle.text = when (modo) {
            ModoAnalytics.INGRESOS -> "Ingresos por mes"
            ModoAnalytics.EGRESOS -> "Egresos por mes"
            ModoAnalytics.JAM -> "Ingresos vs egresos"
        }

        // Target line only where an income comparison makes sense.
        val metaDecos = meta
            ?.takeIf { modo != ModoAnalytics.EGRESOS && it.montoObjetivo > 0 }
            ?.let { listOf(metaLine(it.montoObjetivo)) }
            ?: emptyList()

        // Configure the line provider so each series carries its own colour.
        binding.chartBalance.chart = when (modo) {
            ModoAnalytics.INGRESOS ->
                lineChart(buildLine(R.color.success, withArea = true), decorations = metaDecos)
            ModoAnalytics.EGRESOS ->
                lineChart(buildLine(R.color.chart_egreso, withArea = true))
            ModoAnalytics.JAM -> lineChart(
                buildLine(R.color.success, withArea = false),
                buildLine(R.color.chart_egreso, withArea = false),
                decorations = metaDecos,
            )
        }

        lifecycleScope.launch {
            balanceProducer.runTransaction {
                lineSeries {
                    when (modo) {
                        ModoAnalytics.INGRESOS -> series(t.ingresos)
                        ModoAnalytics.EGRESOS -> series(t.egresos)
                        ModoAnalytics.JAM -> {
                            series(t.ingresos)
                            series(t.egresos)
                        }
                    }
                }
            }
        }
    }

    private fun renderNeto(modo: ModoAnalytics, t: Tendencia?) {
        if (t == null || t.periodos.isEmpty()) {
            binding.chartNeto.visibility = View.GONE
            binding.tvNetoEmpty.visibility = View.VISIBLE
            return
        }
        binding.chartNeto.visibility = View.VISIBLE
        binding.tvNetoEmpty.visibility = View.GONE
        monthLabels = t.periodos.map(::shortPeriod)

        // Per-mode series: the raw side in single modes, the net in JAM.
        val values = t.periodos.indices.map { i ->
            val ing = t.ingresos.getOrElse(i) { 0.0 }
            val egr = t.egresos.getOrElse(i) { 0.0 }
            when (modo) {
                ModoAnalytics.INGRESOS -> ing
                ModoAnalytics.EGRESOS -> egr
                ModoAnalytics.JAM -> ing - egr
            }
        }
        val base = when (modo) {
            ModoAnalytics.INGRESOS -> "Ingresos, mes a mes"
            ModoAnalytics.EGRESOS -> "Egresos, mes a mes"
            ModoAnalytics.JAM -> "Ingresos menos egresos, mes a mes"
        }
        // Straight-line projection of the next period, shown as a small insight
        // (an honest trend read, not an on-chart promise). Needs some history.
        val proy = if (values.size >= MIN_HISTORY_FORECAST) {
            proyectar(values, 1).firstOrNull()
        } else null
        binding.tvNetoSubtitle.text =
            if (proy != null) "$base · proy. próximo: S/ ${formatShort(proy)}" else base

        val columnColor = when (modo) {
            ModoAnalytics.EGRESOS -> R.color.chart_egreso
            else -> R.color.success
        }
        // ponytail: single colour per mode; sign-based green/red split for JAM
        // columns is a custom ColumnProvider — deferred to the Vico polish pass.
        binding.chartNeto.chart = CartesianChart(
            ColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    LineComponent(Fill(color(columnColor)), COLUMN_THICKNESS_DP)
                ),
                dataLabel = dataLabel(columnColor),
                dataLabelValueFormatter = currencyFormatter,
                rangeProvider = headroomRange,
            ),
            startAxis = VerticalAxis.start(label = axisLabel, valueFormatter = currencyFormatter),
            bottomAxis = HorizontalAxis.bottom(label = axisLabel, valueFormatter = monthFormatter),
        )

        lifecycleScope.launch {
            netoProducer.runTransaction {
                columnSeries { series(values) }
            }
        }
    }

    /**
     * Third card swaps by mode:
     *  - EGRESOS -> category ranking ("where the money goes")
     *  - else    -> weekday-earnings columns ("when do I earn most")
     */
    private fun renderThird(
        modo: ModoAnalytics,
        dias: List<IngresoDiaSemana>,
        cats: Map<String, Double>,
    ) {
        if (modo == ModoAnalytics.EGRESOS) {
            binding.tvThirdTitle.text = "Ranking de categorías"
            binding.tvThirdSubtitle.text = "En qué se va tu dinero este mes"
            binding.chartWeekday.visibility = View.GONE
            binding.toggleVentanaSemanas.visibility = View.GONE
            renderCategoryRanking(cats)
        } else {
            binding.tvThirdTitle.text = "¿Qué día ganás más?"
            binding.tvThirdSubtitle.text = "Ingreso por día de la semana"
            binding.rvCategoryRanking.visibility = View.GONE
            binding.toggleVentanaSemanas.visibility = View.VISIBLE
            renderWeekday(dias)
        }
    }

    private fun renderWeekday(dias: List<IngresoDiaSemana>) {
        val withIncome = dias.filter { it.ingresos > 0 }
        if (withIncome.isEmpty()) {
            binding.chartWeekday.visibility = View.GONE
            binding.tvThirdEmpty.visibility = View.VISIBLE
            binding.tvThirdEmpty.text = "Sin ingresos en este período"
            return
        }
        binding.chartWeekday.visibility = View.VISIBLE
        binding.tvThirdEmpty.visibility = View.GONE
        weekdayLabels = dias.map { shortDay(it.dia) }

        val ingresos = dias.map { it.ingresos }
        lifecycleScope.launch {
            weekdayProducer.runTransaction {
                columnSeries { series(ingresos) }
            }
        }
    }

    private fun renderCategoryRanking(mapa: Map<String, Double>) {
        val ranked = mapa.filter { it.value > 0 }.entries.sortedByDescending { it.value }
        if (ranked.isEmpty()) {
            binding.rvCategoryRanking.visibility = View.GONE
            binding.tvThirdEmpty.visibility = View.VISIBLE
            binding.tvThirdEmpty.text = "Sin egresos registrados este mes"
            categoryAdapter.submitList(emptyList())
            return
        }
        binding.rvCategoryRanking.visibility = View.VISIBLE
        binding.tvThirdEmpty.visibility = View.GONE

        val max = ranked.first().value
        val items = ranked.map { (nombre, total) ->
            val pct = if (max > 0) ((total / max) * 100).toInt().coerceIn(0, 100) else 0
            CategoryRankingAdapter.Item(nombre, total, pct)
        }
        categoryAdapter.submitList(items)
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
    }

    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    /**
     * Axis label for a trend period: "2026-05" -> "may" (monthly) and
     * "2026-05-12" -> "12 may" (weekly, labeled by the week's Monday).
     * Falls back to the raw string on any unexpected shape.
     */
    private fun shortPeriod(raw: String): String {
        val parts = raw.split('-')
        val mes = parts.getOrNull(1)?.toIntOrNull()?.let { MESES.getOrNull(it - 1) } ?: return raw
        val dia = parts.getOrNull(2)?.toIntOrNull()
        return if (dia != null) "$dia $mes" else mes
    }

    /** First three letters of a weekday label ("MIERCOLES" -> "Mie"). */
    private fun shortDay(raw: String): String =
        raw.take(3).lowercase().replaceFirstChar { it.uppercase() }

    /** Compact amount for axis/data labels: "1.2k" past a thousand, else "800". No currency symbol. */
    private fun formatShort(value: Double): String {
        val abs = kotlin.math.abs(value)
        return when {
            abs >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", value / 1_000_000)
            abs >= 1_000 -> String.format(Locale.getDefault(), "%.1fk", value / 1_000)
            else -> String.format(Locale.getDefault(), "%.0f", value)
        }
    }

    private companion object {
        /** Peak opacity of the frosted top-bar scrim once fully scrolled. */
        const val GLASS_ALPHA = 0.92f
        const val COLUMN_THICKNESS_DP = 16f
        const val META_LINE_THICKNESS_DP = 2f
        /** Fraction the axis extends past the data extremes so labels never clip. */
        const val HEADROOM = 1.15
        /** Minimum history points before a projection is worth showing. */
        const val MIN_HISTORY_FORECAST = 3
        val MESES = listOf(
            "ene", "feb", "mar", "abr", "may", "jun",
            "jul", "ago", "sep", "oct", "nov", "dic"
        )
    }
}
