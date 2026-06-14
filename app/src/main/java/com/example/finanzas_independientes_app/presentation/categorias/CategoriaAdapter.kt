package com.example.finanzas_independientes_app.presentation.categorias

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.databinding.ItemCategoriaBinding
import com.example.finanzas_independientes_app.domain.model.Categoria

class CategoriaAdapter : ListAdapter<Categoria, CategoriaAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Categoria>() {
            override fun areItemsTheSame(a: Categoria, b: Categoria) = a.id == b.id
            override fun areContentsTheSame(a: Categoria, b: Categoria) = a == b
        }

        private const val COLOR_INGRESO = "#2E7D32"
        private const val COLOR_EGRESO = "#C62828"
        private const val COLOR_INGRESO_BG = "#E8F5E9"
        private const val COLOR_EGRESO_BG = "#FFEBEE"
    }

    inner class ViewHolder(val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Categoria) {
            binding.tvNombre.text = item.nombre

            val isIngreso = item.tipo == "INGRESO"
            binding.tvTipo.text = item.tipo
            binding.tvTipo.setTextColor(
                Color.parseColor(if (isIngreso) COLOR_INGRESO else COLOR_EGRESO)
            )
            binding.tvTipo.setBackgroundColor(
                Color.parseColor(if (isIngreso) COLOR_INGRESO_BG else COLOR_EGRESO_BG)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoriaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
