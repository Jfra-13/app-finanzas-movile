package com.example.finanzas_independientes_app.presentation.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.widget.NestedScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.databinding.ActivityDashboardBinding
import com.example.finanzas_independientes_app.databinding.DialogSetGoalBinding
import com.example.finanzas_independientes_app.databinding.LayoutBottomNavigationBinding
import com.example.finanzas_independientes_app.presentation.common.AddTransactionDialog
import com.example.finanzas_independientes_app.presentation.common.BottomNav
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val bottomNavBinding: LayoutBottomNavigationBinding by lazy {
        LayoutBottomNavigationBinding.bind(binding.root)
    }
    private val viewModel: DashboardViewModel by lazy {
        ViewModelProvider(this)[DashboardViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        applyFrostedSurfaces()
        bindScrollEffects()

        // Load all dashboard data + categories for the dialog spinner
        viewModel.cargarResumen()
        viewModel.cargarCategorias()

        bindFlows()
        bindActions()

        applyBlockOrder(BlockOrderStore.load(this))
    }

    // --- Scroll-driven effect: frosted top bar ---
    //
    // On scroll the fixed top bar's background fades in (frosted look — the View
    // system has no real backdrop blur). The floating pill stays fixed; it does
    // not collapse (an earlier collapse-on-scroll stuttered on slow drags).
    private fun bindScrollEffects() {
        val scrimThreshold = dp(48f).toFloat()
        binding.dashboardScroll.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                binding.topBarContainer.background?.alpha =
                    ((scrollY / scrimThreshold).coerceIn(0f, 1f) * FROSTED_ALPHA * 255).toInt()
            }
        )
    }

    // Frost the fixed top bar (the floating pill is frosted by BottomNav). The
    // scrim starts transparent; the scroll listener fades it in.
    private fun applyFrostedSurfaces() {
        binding.topBarContainer.background?.mutate()?.alpha = 0
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    // Edge-to-edge is forced on modern targetSdk, so the status bar draws over
    // the fixed top bar. Push the status-bar inset into the top bar itself, and
    // start the scroll content below the bar's full height (inset included).
    private fun applyWindowInsets() {
        val topBar = binding.incTopBar.root
        val baseTopBarPaddingTop = topBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBarContainer) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(top = baseTopBarPaddingTop + bars.top)
            insets
        }
        binding.topBarContainer.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            if (binding.dashboardScroll.paddingTop != v.height) {
                binding.dashboardScroll.updatePadding(top = v.height)
            }
        }
    }

    // --- Flow bindings ---

    private fun bindFlows() {
        // Daily quota → progreso diario semicircle label
        lifecycleScope.launch {
            viewModel.cuotaActual.collect { cuota ->
                binding.incProgresoDiario.gaugeProgresoDiario?.amountText = cuota
            }
        }

        // Today income → dinero acumulado card (tvDineroTotal)
        lifecycleScope.launch {
            viewModel.ingresoHoy.collect { ingreso ->
                binding.incDineroAcumulado.tvDineroTotal?.text = ingreso
            }
        }

        // Goals progress → metas cards + objective label + semanal card
        lifecycleScope.launch {
            viewModel.progresoMetas.collect { progreso ->
                if (progreso != null) {
                    // Metas section: daily / weekly / monthly targets derived from ProgresoMetas
                    binding.incMetas.tvMetaDiaria?.text =
                        String.format("%.0f", progreso.metaDiaria)
                    binding.incMetas.tvMetaSemanal?.text =
                        String.format("%.0f", progreso.metaSemanal)
                    binding.incMetas.tvMetaMensual?.text =
                        String.format("%.0f", progreso.metaMensual)

                    // Objective label in dinero acumulado card
                    binding.incDineroAcumulado.tvDineroObjetivo?.text =
                        String.format("%.0f", progreso.metaMensual)

                    // Weekly progress ring: ingresoSemanal / metaSemanal → 0-100
                    val pctSemanal = if (progreso.metaSemanal > 0) {
                        ((progreso.ingresoSemanal / progreso.metaSemanal) * 100).toInt().coerceIn(0, 100)
                    } else 0
                    binding.incProgresoSemanal.pbProgresoSemanal?.setProgress(pctSemanal / 100f)
                    binding.incProgresoSemanal.tvSemanalPorcentaje?.text = "$pctSemanal%"

                    // Daily progress ring: ingresoDiario / metaDiaria (200 max for semicircle)
                    val pctDiario = if (progreso.metaDiaria > 0) {
                        ((progreso.ingresoDiario / progreso.metaDiaria) * 200).toInt().coerceIn(0, 200)
                    } else 0
                    binding.incProgresoDiario.gaugeProgresoDiario?.setProgress(pctDiario / 200f)
                    binding.incProgresoDiario.tvProgresoPorcentaje?.text =
                        "${(pctDiario / 2)}%"

                    // Objective in progreso diario (below divider). Symbol trails the
                    // amount to match the headline; the gauge shrinks the "S/" token.
                    binding.incProgresoDiario.gaugeProgresoDiario?.objetivoText =
                        String.format("%.0f S/", progreso.metaDiaria)
                }
            }
        }

        // Weekly summary bars (Fase 5 will do full charting; we just scale heights here)
        lifecycleScope.launch {
            viewModel.resumenSemanal.collect { lista ->
                if (lista.size == 7) {
                    val maxVal = lista.maxOf { maxOf(it.ingresos, it.egresos) }.takeIf { it > 0 } ?: 1.0
                    val maxPx = 200  // match tallest bar in original layout (dp, good-enough proxy)
                    val barIds = listOf(
                        Pair(binding.incDineroAcumulado.barIngresoLu, binding.incDineroAcumulado.barEgresoLu),
                        Pair(binding.incDineroAcumulado.barIngresoMar, binding.incDineroAcumulado.barEgresoMar),
                        Pair(binding.incDineroAcumulado.barIngresoMie, binding.incDineroAcumulado.barEgresoMie),
                        Pair(binding.incDineroAcumulado.barIngresoJue, binding.incDineroAcumulado.barEgresoJue),
                        Pair(binding.incDineroAcumulado.barIngresoVie, binding.incDineroAcumulado.barEgresoVie),
                        Pair(binding.incDineroAcumulado.barIngresoSab, binding.incDineroAcumulado.barEgresoSab),
                        Pair(binding.incDineroAcumulado.barIngresoDom, binding.incDineroAcumulado.barEgresoDom)
                    )
                    val density = resources.displayMetrics.density
                    lista.forEachIndexed { i, dia ->
                        val (ingBar, egBar) = barIds[i]
                        val ingPx = ((dia.ingresos / maxVal) * maxPx * density).toInt().coerceAtLeast(4)
                        val egPx = ((dia.egresos / maxVal) * maxPx * density).toInt().coerceAtLeast(4)
                        ingBar?.layoutParams = ingBar?.layoutParams?.also { it.height = ingPx }
                        egBar?.layoutParams = egBar?.layoutParams?.also { it.height = egPx }
                    }
                }
            }
        }

        // Toast for any UI messages
        lifecycleScope.launch {
            viewModel.mensajeUI.collect { mensaje ->
                if (mensaje != null) {
                    Toast.makeText(this@DashboardActivity, mensaje, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                }
            }
        }
    }

    // --- Action bindings ---

    private fun bindActions() {
        // Shared bottom nav: routing + active tab + FAB, all wired in one place.
        BottomNav.setup(this, bottomNavBinding, BottomNav.Tab.HOME) {
            showAddTransactionDialog()
        }

        // Metas "Fijar meta" button
        binding.incMetas.btnFijarMeta?.setOnClickListener {
            showSetGoalDialog()
        }

        // Top-bar overflow → reorder the dashboard blocks.
        binding.incTopBar.ivMenu.setOnClickListener {
            ReorderBlocksSheet(this) { order -> applyBlockOrder(order) }.show()
        }
    }

    // Rearranges the reorderable blocks to the persisted order (Metas stays pinned).
    private fun applyBlockOrder(order: List<String>) {
        val blocks = mapOf(
            BlockOrderStore.DIARIO to binding.incProgresoDiario.root,
            BlockOrderStore.SEMANAL to binding.incProgresoSemanal.root,
            BlockOrderStore.ACUMULADO to binding.incDineroAcumulado.root
        )
        val container = binding.reorderContainer
        container.removeAllViews()
        order.forEach { key -> blocks[key]?.let { container.addView(it) } }
    }

    // --- Dialogs ---

    private fun showAddTransactionDialog() {
        AddTransactionDialog.show(this, viewModel.categorias.value) { monto, tipo, descripcion, categoriaId ->
            viewModel.registrarTransaccion(monto, tipo, descripcion, categoriaId)
        }
    }

    private fun showSetGoalDialog() {
        val dialogBinding = DialogSetGoalBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnConfirmarMeta.setOnClickListener {
            val monto = dialogBinding.etMontoMeta.text?.toString() ?: ""

            // Day map: CheckBox id → API day int (1=Mon … 7=Sun)
            val diasSeleccionados = mutableListOf<Int>()
            if (dialogBinding.cbLunes.isChecked)    diasSeleccionados.add(1)
            if (dialogBinding.cbMartes.isChecked)   diasSeleccionados.add(2)
            if (dialogBinding.cbMiercoles.isChecked) diasSeleccionados.add(3)
            if (dialogBinding.cbJueves.isChecked)   diasSeleccionados.add(4)
            if (dialogBinding.cbViernes.isChecked)  diasSeleccionados.add(5)
            if (dialogBinding.cbSabado.isChecked)   diasSeleccionados.add(6)
            if (dialogBinding.cbDomingo.isChecked)  diasSeleccionados.add(7)

            viewModel.fijarMeta(monto, diasSeleccionados)
            dialog.dismiss()
        }

        dialog.show()
    }

    private companion object {
        // Opacity of frosted surfaces (top-bar scrim, nav pill, FAB cradle).
        const val FROSTED_ALPHA = 0.9f
    }
}
