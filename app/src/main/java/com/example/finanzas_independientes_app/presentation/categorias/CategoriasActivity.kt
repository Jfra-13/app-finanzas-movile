package com.example.finanzas_independientes_app.presentation.categorias

import android.os.Bundle
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
import com.example.finanzas_independientes_app.presentation.common.ViewStateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        adapter = CategoriaAdapter()
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
}
