package com.example.finanzas_independientes_app.presentation.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ItemComparacionCategoriaBinding
import com.example.finanzas_independientes_app.domain.model.ComparacionCategoriaItem
import java.util.Locale

/**
 * Per-category spend, current period vs the comparison window. The delta badge is
 * red when spend rose, green when it fell, and "—" when there is no prior base
 * (deltaPct null) so a 0-base period never reads as a real 0 %.
 */
class ComparacionAdapter :
    ListAdapter<ComparacionCategoriaItem, ComparacionAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(
        private val binding: ItemComparacionCategoriaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ComparacionCategoriaItem) {
            val ctx = binding.root.context
            binding.tvCompCategoria.text = item.categoria
            binding.tvCompAnterior.text =
                String.format(Locale.getDefault(), "antes S/ %,.0f", item.anterior)
            binding.tvCompActual.text =
                String.format(Locale.getDefault(), "S/ %,.0f", item.actual)

            val delta = item.deltaPct
            if (delta == null) {
                binding.tvCompDelta.text = "—"
                binding.tvCompDelta.setTextColor(ContextCompat.getColor(ctx, R.color.text_gray))
            } else {
                val signo = if (delta > 0) "+" else ""
                binding.tvCompDelta.text =
                    String.format(Locale.getDefault(), "%s%.1f%%", signo, delta)
                val colorRes = when {
                    delta > 0 -> R.color.health_danger    // spending more = worse
                    delta < 0 -> R.color.health_positive   // spending less = better
                    else -> R.color.text_gray
                }
                binding.tvCompDelta.setTextColor(ContextCompat.getColor(ctx, colorRes))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemComparacionCategoriaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ComparacionCategoriaItem>() {
            override fun areItemsTheSame(a: ComparacionCategoriaItem, b: ComparacionCategoriaItem) =
                a.categoria == b.categoria
            override fun areContentsTheSame(a: ComparacionCategoriaItem, b: ComparacionCategoriaItem) = a == b
        }
    }
}
