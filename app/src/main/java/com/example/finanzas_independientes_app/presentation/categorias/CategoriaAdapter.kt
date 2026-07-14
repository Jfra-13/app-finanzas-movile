package com.example.finanzas_independientes_app.presentation.categorias

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ItemCategoriaBinding
import com.example.finanzas_independientes_app.domain.model.Categoria

class CategoriaAdapter(
    private val onItemClick: (Categoria) -> Unit = {}
) : ListAdapter<Categoria, CategoriaAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Categoria>() {
            override fun areItemsTheSame(a: Categoria, b: Categoria) = a.id == b.id
            override fun areContentsTheSame(a: Categoria, b: Categoria) = a == b
        }
    }

    inner class ViewHolder(val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Categoria) {
            val ctx = binding.root.context
            binding.root.setOnClickListener { onItemClick(item) }
            binding.tvNombre.text = item.nombre

            // Income vs expense read at a glance: green pill vs red pill, theme-aware.
            val isIngreso = item.tipo == "INGRESO"
            val accentRes = if (isIngreso) R.color.health_positive else R.color.health_danger
            val bgRes = if (isIngreso) R.color.health_positive_bg else R.color.health_danger_bg

            binding.tvTipo.text = if (isIngreso) "Ingreso" else "Egreso"
            binding.tvTipo.setTextColor(ContextCompat.getColor(ctx, accentRes))
            binding.tvTipo.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, bgRes))
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
