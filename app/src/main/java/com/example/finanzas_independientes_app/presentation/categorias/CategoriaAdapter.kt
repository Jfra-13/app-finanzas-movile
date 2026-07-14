package com.example.finanzas_independientes_app.presentation.categorias

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ItemCategoriaBinding
import com.example.finanzas_independientes_app.domain.model.Categoria
import com.example.finanzas_independientes_app.domain.model.Presupuesto
import java.util.Locale

/** A category paired with its budget for the current month, if one is set. */
data class CategoriaConPresupuesto(
    val categoria: Categoria,
    val presupuesto: Presupuesto?
)

class CategoriaAdapter(
    private val onItemClick: (CategoriaConPresupuesto) -> Unit = {}
) : ListAdapter<CategoriaConPresupuesto, CategoriaAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CategoriaConPresupuesto>() {
            override fun areItemsTheSame(a: CategoriaConPresupuesto, b: CategoriaConPresupuesto) =
                a.categoria.id == b.categoria.id
            override fun areContentsTheSame(a: CategoriaConPresupuesto, b: CategoriaConPresupuesto) = a == b
        }
    }

    inner class ViewHolder(val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoriaConPresupuesto) {
            val ctx = binding.root.context
            val categoria = item.categoria
            binding.root.setOnClickListener { onItemClick(item) }
            binding.tvNombre.text = categoria.nombre

            // Income vs expense read at a glance: green pill vs red pill, theme-aware.
            val isIngreso = categoria.tipo == "INGRESO"
            val accentRes = if (isIngreso) R.color.health_positive else R.color.health_danger
            val bgRes = if (isIngreso) R.color.health_positive_bg else R.color.health_danger_bg

            binding.tvTipo.text = if (isIngreso) "Ingreso" else "Egreso"
            binding.tvTipo.setTextColor(ContextCompat.getColor(ctx, accentRes))
            binding.tvTipo.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, bgRes))

            bindPresupuesto(ctx, item.presupuesto)
        }

        private fun bindPresupuesto(ctx: Context, presupuesto: Presupuesto?) {
            if (presupuesto == null) {
                binding.barPresupuesto.visibility = View.GONE
                binding.tvPresupuesto.visibility = View.GONE
                return
            }
            binding.barPresupuesto.visibility = View.VISIBLE
            binding.tvPresupuesto.visibility = View.VISIBLE

            // Bar caps at 100 %; the label keeps the real (possibly > 100) figure.
            binding.barPresupuesto.setProgressCompat(
                presupuesto.consumoPct.toInt().coerceIn(0, 100), true
            )
            val danger = presupuesto.excedido
            val accentRes = if (danger) R.color.health_danger else R.color.health_info
            binding.barPresupuesto.setIndicatorColor(ContextCompat.getColor(ctx, accentRes))

            binding.tvPresupuesto.text = String.format(
                Locale.getDefault(),
                "S/ %,.0f de %,.0f · %.0f%%",
                presupuesto.gastadoMes, presupuesto.montoMensual, presupuesto.consumoPct
            )
            binding.tvPresupuesto.setTextColor(
                ContextCompat.getColor(ctx, if (danger) R.color.health_danger else R.color.text_gray)
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
