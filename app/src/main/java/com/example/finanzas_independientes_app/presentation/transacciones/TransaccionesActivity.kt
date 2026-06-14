package com.example.finanzas_independientes_app.presentation.transacciones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ActivityTransaccionesBinding
import com.example.finanzas_independientes_app.databinding.DialogEditTransactionBinding
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.Transaccion
import com.example.finanzas_independientes_app.presentation.common.ViewStateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransaccionesActivity : AppCompatActivity() {

    companion object {
        // Trigger the next-page load when this many rows remain below the fold.
        private const val PREFETCH_THRESHOLD = 4
    }

    private lateinit var binding: ActivityTransaccionesBinding
    private val viewModel: TransaccionesViewModel by lazy {
        ViewModelProvider(this)[TransaccionesViewModel::class.java]
    }

    private lateinit var adapter: TransaccionAdapter
    private lateinit var stateHelper: ViewStateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransaccionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        stateHelper = ViewStateHelper(binding.viewState) { viewModel.cargar(refresh = true) }
        bindFlows()
        bindActions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = TransaccionAdapter(
            onEdit = { transaccion -> showEditDialog(transaccion) },
            onDelete = { transaccion -> showDeleteConfirmation(transaccion) }
        )
        val layoutManager = LinearLayoutManager(this)
        binding.rvTransacciones.layoutManager = layoutManager
        binding.rvTransacciones.adapter = adapter
        binding.rvTransacciones.setHasFixedSize(true)

        // Infinite scroll: prefetch the next page before the user hits the bottom.
        binding.rvTransacciones.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val visibleItems = layoutManager.childCount
                val totalItems = layoutManager.itemCount
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible + visibleItems >= totalItems - PREFETCH_THRESHOLD) {
                    viewModel.cargarMas()
                }
            }
        })
    }

    private fun bindFlows() {
        lifecycleScope.launch {
            viewModel.transacciones.collect { list ->
                adapter.submitList(list)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                if (loading) {
                    stateHelper.showLoading()
                    binding.rvTransacciones.visibility = View.GONE
                } else {
                    // Content/empty will be handled by isEmpty flow below
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isEmpty.collect { empty ->
                if (!viewModel.isLoading.value) {
                    if (empty) {
                        stateHelper.showEmpty("No hay transacciones aún")
                        binding.rvTransacciones.visibility = View.GONE
                    } else {
                        stateHelper.showContent()
                        binding.rvTransacciones.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLastPage.collect { isLast ->
                binding.btnLoadMore.visibility = if (isLast) View.GONE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            viewModel.mensajeError.collect { error ->
                if (error != null) {
                    stateHelper.showError(error) { viewModel.limpiarError() }
                    Toast.makeText(this@TransaccionesActivity, error, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarError()
                }
            }
        }
    }

    private fun bindActions() {
        binding.btnLoadMore.setOnClickListener {
            viewModel.cargarMas()
        }
    }

    // --- Edit dialog ---

    private fun showEditDialog(transaccion: Transaccion) {
        val dialogBinding = DialogEditTransactionBinding.inflate(LayoutInflater.from(this))

        // Pre-fill current values
        dialogBinding.etMonto.setText(transaccion.monto.toString())
        dialogBinding.etDescripcion.setText(transaccion.descripcion ?: "")

        // Type toggle
        if (transaccion.tipo == "INGRESO") {
            dialogBinding.toggleTipo.check(R.id.btnTipoIngreso)
        } else {
            dialogBinding.toggleTipo.check(R.id.btnTipoEgreso)
        }

        // Category spinner
        val currentCategorias = mutableListOf<Categoria>()
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<String>()
        )
        dialogBinding.actvCategoria.setAdapter(spinnerAdapter)

        // Populate spinner with current categories snapshot
        val cats = viewModel.categorias.value
        currentCategorias.clear()
        currentCategorias.addAll(cats)
        spinnerAdapter.clear()
        spinnerAdapter.add("Sin categoría")
        spinnerAdapter.addAll(cats.map { it.nombre })
        spinnerAdapter.notifyDataSetChanged()

        // Pre-select current category
        val currentCatName = transaccion.categoriaNombre
        if (currentCatName != null) {
            dialogBinding.actvCategoria.setText(currentCatName, false)
        } else {
            dialogBinding.actvCategoria.setText("Sin categoría", false)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnGuardar.setOnClickListener {
            val montoText = dialogBinding.etMonto.text?.toString()?.trim() ?: ""
            val monto = montoText.toDoubleOrNull()
            if (monto == null || monto <= 0) {
                Toast.makeText(this, "Ingresá un monto válido (mayor a 0)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipo = if (dialogBinding.toggleTipo.checkedButtonId == R.id.btnTipoIngreso) {
                "INGRESO"
            } else {
                "EGRESO"
            }

            val descripcion = dialogBinding.etDescripcion.text?.toString()
            val selectedName = dialogBinding.actvCategoria.text?.toString()
            val categoriaId = currentCategorias.firstOrNull { it.nombre == selectedName }?.id

            viewModel.actualizar(transaccion.id, monto, tipo, descripcion, categoriaId)
            dialog.dismiss()
        }

        dialog.show()
    }

    // --- Delete confirmation ---

    private fun showDeleteConfirmation(transaccion: Transaccion) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar transacción")
            .setMessage("¿Querés eliminar esta transacción de S/ ${String.format("%.2f", transaccion.monto)}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminar(transaccion.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
