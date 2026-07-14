package com.example.finanzas_independientes_app.presentation.categorias

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ActivityCategoriasBinding
import com.example.finanzas_independientes_app.databinding.DialogCreateCategoriaBinding
import com.example.finanzas_independientes_app.databinding.DialogInputTextBinding
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.presentation.common.ViewStateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class CategoriasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriasBinding
    private val viewModel: CategoriasViewModel by lazy {
        ViewModelProvider(this)[CategoriasViewModel::class.java]
    }

    private lateinit var adapter: CategoriaAdapter
    private lateinit var stateHelper: ViewStateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        stateHelper = ViewStateHelper(binding.viewState) { viewModel.cargar() }
        bindFlows()
        bindActions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = CategoriaAdapter { row -> showOptionsDialog(row) }
        binding.rvCategorias.layoutManager = LinearLayoutManager(this)
        binding.rvCategorias.adapter = adapter
    }

    private fun bindFlows() {
        lifecycleScope.launch {
            viewModel.categorias.collect { list ->
                adapter.submitList(list)
                if (!viewModel.isLoading.value) {
                    if (list.isEmpty()) {
                        stateHelper.showEmpty("No hay categorías aún")
                        binding.rvCategorias.visibility = View.GONE
                    } else {
                        stateHelper.showContent()
                        binding.rvCategorias.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                if (loading) {
                    stateHelper.showLoading()
                    binding.rvCategorias.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.mensajeError.collect { error ->
                if (error != null) {
                    stateHelper.showError(error) { viewModel.limpiarError() }
                    Toast.makeText(this@CategoriasActivity, error, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarError()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.createSuccess.collect { msg ->
                if (msg != null) {
                    Toast.makeText(this@CategoriasActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarCreateSuccess()
                }
            }
        }
    }

    private fun bindActions() {
        binding.fabCrearCategoria.setOnClickListener {
            showCreateDialog()
        }
    }

    // --- Create dialog ---

    private fun showCreateDialog() {
        val dialogBinding = DialogCreateCategoriaBinding.inflate(LayoutInflater.from(this))

        // Default: INGRESO selected
        dialogBinding.toggleTipo.check(R.id.btnTipoIngreso)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCrear.setOnClickListener {
            val nombre = dialogBinding.etNombre.text?.toString() ?: ""
            val tipo = if (dialogBinding.toggleTipo.checkedButtonId == R.id.btnTipoIngreso) {
                "INGRESO"
            } else {
                "EGRESO"
            }

            viewModel.crear(nombre, tipo)
            dialog.dismiss()
        }

        dialog.show()
    }

    // --- Edit / delete ---

    // System categories also open this dialog; the backend answers 403 and the
    // ViewModel surfaces a clear message (the DTO carries no "system" flag yet).
    // Budget actions are offered only on expense categories — a cap is a spending limit.
    private fun showOptionsDialog(row: CategoriaConPresupuesto) {
        val categoria = row.categoria
        val esEgreso = categoria.tipo == "EGRESO"
        val tieneTope = row.presupuesto != null

        val acciones = buildList {
            add("Renombrar")
            if (esEgreso) add(if (tieneTope) "Editar tope" else "Fijar tope")
            if (esEgreso && tieneTope) add("Quitar tope")
            add("Eliminar")
        }

        AlertDialog.Builder(this)
            .setTitle(categoria.nombre)
            .setItems(acciones.toTypedArray()) { _, which ->
                when (acciones[which]) {
                    "Renombrar" -> showRenameDialog(categoria)
                    "Fijar tope", "Editar tope" -> showBudgetDialog(row)
                    "Quitar tope" -> confirmQuitarTope(row)
                    "Eliminar" -> confirmDelete(categoria)
                }
            }
            .show()
    }

    private fun showRenameDialog(categoria: Categoria) {
        val dialogBinding = DialogInputTextBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = "Renombrar categoría"
        dialogBinding.tilInput.hint = "Nombre"
        dialogBinding.etInput.setText(categoria.nombre)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnGuardar.setOnClickListener {
            viewModel.renombrar(categoria.id, dialogBinding.etInput.text?.toString() ?: "")
            dialog.dismiss()
        }
        dialog.show()
    }

    // --- Budget (tope) ---

    // Reuses the generic input dialog with a numeric keypad; prefilled when editing.
    private fun showBudgetDialog(row: CategoriaConPresupuesto) {
        val actual = row.presupuesto
        val dialogBinding = DialogInputTextBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = if (actual != null) "Editar tope" else "Fijar tope"
        dialogBinding.tilInput.hint = "Monto mensual (S/)"
        dialogBinding.etInput.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        if (actual != null) {
            dialogBinding.etInput.setText(String.format(Locale.getDefault(), "%.2f", actual.montoMensual))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnGuardar.setOnClickListener {
            viewModel.fijarTope(row.categoria.id, dialogBinding.etInput.text?.toString() ?: "")
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun confirmQuitarTope(row: CategoriaConPresupuesto) {
        val presupuesto = row.presupuesto ?: return
        AlertDialog.Builder(this)
            .setTitle("Quitar tope")
            .setMessage("Se eliminará el tope de \"${row.categoria.nombre}\".")
            .setPositiveButton("Quitar") { _, _ -> viewModel.quitarTope(presupuesto.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete(categoria: Categoria) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar categoría")
            .setMessage(
                "\"${categoria.nombre}\" se eliminará. Sus movimientos no se borran: " +
                    "quedan sin categoría. Los presupuestos asociados sí se eliminan."
            )
            .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminar(categoria.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
