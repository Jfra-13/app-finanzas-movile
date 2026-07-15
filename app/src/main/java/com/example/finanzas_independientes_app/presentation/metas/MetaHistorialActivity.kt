package com.example.finanzas_independientes_app.presentation.metas

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finanzas_independientes_app.databinding.ActivityMetaHistorialBinding
import com.example.finanzas_independientes_app.presentation.common.ViewStateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Read-only list of past monthly goals and whether each was met (GET /metas/historial). */
@AndroidEntryPoint
class MetaHistorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMetaHistorialBinding
    private val viewModel: MetaHistorialViewModel by lazy {
        ViewModelProvider(this)[MetaHistorialViewModel::class.java]
    }

    private lateinit var adapter: MetaHistorialAdapter
    private lateinit var stateHelper: ViewStateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMetaHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MetaHistorialAdapter()
        binding.rvHistorial.layoutManager = LinearLayoutManager(this)
        binding.rvHistorial.adapter = adapter

        stateHelper = ViewStateHelper(binding.viewState) { viewModel.cargar() }
        bindFlows()
    }

    private fun bindFlows() {
        lifecycleScope.launch {
            viewModel.items.collect { list ->
                adapter.submitList(list)
                if (!viewModel.isLoading.value) {
                    if (list.isEmpty()) {
                        stateHelper.showEmpty("Todavía no hay metas pasadas")
                        binding.rvHistorial.visibility = View.GONE
                    } else {
                        stateHelper.showContent()
                        binding.rvHistorial.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                if (loading) {
                    stateHelper.showLoading()
                    binding.rvHistorial.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.mensajeError.collect { error ->
                if (error != null) {
                    stateHelper.showError(error) { viewModel.cargar() }
                    binding.rvHistorial.visibility = View.GONE
                    Toast.makeText(this@MetaHistorialActivity, error, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarError()
                }
            }
        }
    }
}
