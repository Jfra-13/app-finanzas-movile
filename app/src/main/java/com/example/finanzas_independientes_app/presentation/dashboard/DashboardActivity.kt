package com.example.finanzas_independientes_app.presentation.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ActivityDashboardBinding
import com.example.finanzas_independientes_app.databinding.DialogAddTransactionBinding
import com.example.finanzas_independientes_app.databinding.DialogSetGoalBinding
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.presentation.categorias.CategoriasActivity
import com.example.finanzas_independientes_app.presentation.transacciones.TransaccionesActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by lazy {
        ViewModelProvider(this)[DashboardViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load all dashboard data + categories for the dialog spinner
        viewModel.cargarResumen()
        viewModel.cargarCategorias()

        bindFlows()
        bindActions()
    }

    // --- Flow bindings ---

    private fun bindFlows() {
        // Daily quota → progreso diario semicircle label
        lifecycleScope.launch {
            viewModel.cuotaActual.collect { cuota ->
                binding.incProgresoDiario.tvMontoActual?.text = cuota
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
                    binding.incProgresoSemanal.pbProgresoSemanal?.progress = pctSemanal
                    binding.incProgresoSemanal.tvSemanalPorcentaje?.text = "$pctSemanal%"

                    // Daily progress ring: ingresoDiario / metaDiaria (200 max for semicircle)
                    val pctDiario = if (progreso.metaDiaria > 0) {
                        ((progreso.ingresoDiario / progreso.metaDiaria) * 200).toInt().coerceIn(0, 200)
                    } else 0
                    binding.incProgresoDiario.pbProgresoDiario?.progress = pctDiario
                    binding.incProgresoDiario.tvProgresoPorcentaje?.text =
                        "${(pctDiario / 2)}%"

                    // Objective in progreso diario (below divider)
                    binding.incProgresoDiario.tvMontoObjetivo?.text =
                        String.format("%.0f", progreso.metaDiaria)
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
        // FAB → add-transaction dialog
        binding.incBottomNav.cardFabAdd?.setOnClickListener {
            showAddTransactionDialog()
        }

        // navCalendar → transactions history
        binding.incBottomNav.navCalendar?.setOnClickListener {
            startActivity(Intent(this, TransaccionesActivity::class.java))
        }

        // navStats → categories
        binding.incBottomNav.navStats?.setOnClickListener {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }

        // Metas "Fijar meta" button
        binding.incMetas.btnFijarMeta?.setOnClickListener {
            showSetGoalDialog()
        }
    }

    // --- Dialogs ---

    private fun showAddTransactionDialog() {
        val dialogBinding = DialogAddTransactionBinding.inflate(LayoutInflater.from(this))

        // Default: INGRESO selected
        dialogBinding.toggleTipo.check(R.id.btnTipoIngreso)

        // Category spinner — populated from VM
        val currentCategorias = mutableListOf<Categoria>()
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<String>()
        )
        dialogBinding.actvCategoria.setAdapter(spinnerAdapter)

        lifecycleScope.launch {
            viewModel.categorias.collect { cats ->
                currentCategorias.clear()
                currentCategorias.addAll(cats)
                spinnerAdapter.clear()
                spinnerAdapter.add("Sin categoría")
                spinnerAdapter.addAll(cats.map { it.nombre })
                spinnerAdapter.notifyDataSetChanged()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnConfirmarTransaccion.setOnClickListener {
            val monto = dialogBinding.etMonto.text?.toString() ?: ""
            val tipo = if (dialogBinding.toggleTipo.checkedButtonId == R.id.btnTipoIngreso) {
                "INGRESO"
            } else {
                "EGRESO"
            }
            val descripcion = dialogBinding.etDescripcion.text?.toString()
            val selectedName = dialogBinding.actvCategoria.text?.toString()
            val categoriaId = currentCategorias.firstOrNull { it.nombre == selectedName }?.id

            viewModel.registrarTransaccion(monto, tipo, descripcion, categoriaId)
            dialog.dismiss()
        }

        dialog.show()
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
}
