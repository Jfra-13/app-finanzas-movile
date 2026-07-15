package com.example.finanzas_independientes_app.presentation.analytics

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ItemHealthSignalBinding
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem
import com.google.android.material.card.MaterialCardView

/**
 * Financial-health signals, tinted by severity. Signals arrive already ordered
 * (most urgent first) from the ViewModel. A signal that carries a categoriaId —
 * only PRESUPUESTO_EXCEDIDO does — is tappable and deep-links to that category;
 * every other row is inert.
 */
class SaludAdapter(
    private val onCategoriaClick: (Long) -> Unit
) : ListAdapter<SaludFinancieraItem, SaludAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(
        private val binding: ItemHealthSignalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SaludFinancieraItem) {
            val ctx = binding.root.context
            val estilo = estiloDe(item)
            (binding.root as MaterialCardView)
                .setCardBackgroundColor(ContextCompat.getColor(ctx, estilo.bgColor))
            val accentTint = ColorStateList.valueOf(ContextCompat.getColor(ctx, estilo.accentColor))
            binding.healthAccent.backgroundTintList = accentTint
            binding.healthIconCircle.backgroundTintList = accentTint
            binding.tvHealthIcon.text = estilo.icon
            binding.tvHealthMensaje.text = item.mensaje

            val categoriaId = item.categoriaId
            if (categoriaId != null) {
                binding.root.isClickable = true
                binding.root.setOnClickListener { onCategoriaClick(categoriaId) }
            } else {
                binding.root.isClickable = false
                binding.root.setOnClickListener(null)
            }
        }
    }

    private data class Estilo(val accentColor: Int, val bgColor: Int, val icon: String)

    /** Colour + icon by tipo/severidad, reusing the health palette (no new colours). */
    private fun estiloDe(item: SaludFinancieraItem): Estilo = when {
        item.tipo.equals("FELICITACION", ignoreCase = true) ->
            Estilo(R.color.health_positive, R.color.health_positive_bg, "🎉")
        item.severidad.equals("ALTA", ignoreCase = true) ->
            Estilo(R.color.health_danger, R.color.health_danger_bg, "⚠️")
        else ->
            Estilo(R.color.health_info, R.color.health_info_bg, "ℹ️")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHealthSignalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<SaludFinancieraItem>() {
            override fun areItemsTheSame(a: SaludFinancieraItem, b: SaludFinancieraItem) =
                a.code == b.code && a.categoriaId == b.categoriaId
            override fun areContentsTheSame(a: SaludFinancieraItem, b: SaludFinancieraItem) = a == b
        }
    }
}
