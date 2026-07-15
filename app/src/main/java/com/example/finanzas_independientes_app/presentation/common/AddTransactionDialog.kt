package com.example.finanzas_independientes_app.presentation.common

import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.DialogAddTransactionBinding
import com.example.finanzas_independientes_app.domain.model.Categoria

/**
 * Shared "add transaction" dialog behind the central FAB. Extracted so every
 * screen that hosts the bottom nav opens the exact same flow. The caller supplies
 * a category snapshot (for the spinner) and receives the parsed selection back —
 * validation and persistence stay in the caller's ViewModel.
 */
object AddTransactionDialog {

    fun show(
        activity: AppCompatActivity,
        categorias: List<Categoria>,
        onConfirm: (monto: String, tipo: String, descripcion: String?, categoriaId: Long?) -> Unit
    ) {
        val binding = DialogAddTransactionBinding.inflate(activity.layoutInflater)

        // Default: INGRESO selected.
        binding.toggleTipo.check(R.id.btnTipoIngreso)

        val spinnerAdapter = ArrayAdapter(
            activity,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<String>()
        )
        binding.actvCategoria.setAdapter(spinnerAdapter)
        spinnerAdapter.add("Sin categoría")
        spinnerAdapter.addAll(categorias.map { it.nombre })

        val dialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .create()

        binding.btnConfirmarTransaccion.setOnClickListener {
            val monto = binding.etMonto.text?.toString() ?: ""
            val tipo = if (binding.toggleTipo.checkedButtonId == R.id.btnTipoIngreso) "INGRESO" else "EGRESO"
            val descripcion = binding.etDescripcion.text?.toString()
            val selectedName = binding.actvCategoria.text?.toString()
            val categoriaId = categorias.firstOrNull { it.nombre == selectedName }?.id

            onConfirm(monto, tipo, descripcion, categoriaId)
            dialog.dismiss()
        }

        dialog.show()
    }
}
