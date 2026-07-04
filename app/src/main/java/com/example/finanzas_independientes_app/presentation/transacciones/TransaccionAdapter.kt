package com.example.finanzas_independientes_app.presentation.transacciones

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.databinding.ItemTransaccionBinding
import com.example.finanzas_independientes_app.domain.model.Transaccion

/**
 * Shared transaction list adapter. Pass [onEdit]/[onDelete] for the editable list;
 * omit them (read-only, e.g. the analytics drill-down sheet) to hide the action buttons.
 */
class TransaccionAdapter(
    private val onEdit: ((Transaccion) -> Unit)? = null,
    private val onDelete: ((Transaccion) -> Unit)? = null
) : ListAdapter<Transaccion, TransaccionAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Transaccion>() {
            override fun areItemsTheSame(a: Transaccion, b: Transaccion) = a.id == b.id
            override fun areContentsTheSame(a: Transaccion, b: Transaccion) = a == b
        }

        // Income green / expense red
        private const val COLOR_INGRESO = "#2E7D32"
        private const val COLOR_EGRESO = "#C62828"
        private const val COLOR_INGRESO_BG = "#E8F5E9"
        private const val COLOR_EGRESO_BG = "#FFEBEE"
    }

    inner class ViewHolder(val binding: ItemTransaccionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Transaccion) {
            val isIngreso = item.tipo == "INGRESO"
            val color = if (isIngreso) COLOR_INGRESO else COLOR_EGRESO
            val bgColor = if (isIngreso) COLOR_INGRESO_BG else COLOR_EGRESO_BG
            val sign = if (isIngreso) "+" else "−"

            binding.tvMonto.text = "$sign S/ ${String.format("%.2f", item.monto)}"
            binding.tvMonto.setTextColor(Color.parseColor(color))

            binding.tvTipo.text = item.tipo
            binding.tvTipo.setTextColor(Color.parseColor(color))
            binding.tvTipo.setBackgroundColor(Color.parseColor(bgColor))

            // Show only date part of the ISO string (first 10 chars: YYYY-MM-DD)
            binding.tvFecha.text = item.fecha.take(10)

            binding.tvDescripcion.text = item.descripcion?.takeIf { it.isNotBlank() } ?: "Sin descripción"

            val catLabel = item.categoriaNombre?.takeIf { it.isNotBlank() }
            binding.tvCategoria.text = if (catLabel != null) "Categoría: $catLabel" else ""

            binding.btnEdit.visibility = if (onEdit != null) View.VISIBLE else View.GONE
            binding.btnDelete.visibility = if (onDelete != null) View.VISIBLE else View.GONE
            binding.btnEdit.setOnClickListener { onEdit?.invoke(item) }
            binding.btnDelete.setOnClickListener { onDelete?.invoke(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransaccionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
