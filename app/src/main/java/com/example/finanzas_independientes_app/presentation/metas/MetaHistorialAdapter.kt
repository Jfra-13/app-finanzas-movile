package com.example.finanzas_independientes_app.presentation.metas

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ItemMetaHistorialBinding
import com.example.finanzas_independientes_app.domain.model.MetaHistorialItem
import java.util.Locale

class MetaHistorialAdapter :
    ListAdapter<MetaHistorialItem, MetaHistorialAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MetaHistorialItem>() {
            override fun areItemsTheSame(a: MetaHistorialItem, b: MetaHistorialItem) =
                a.periodo == b.periodo
            override fun areContentsTheSame(a: MetaHistorialItem, b: MetaHistorialItem) = a == b
        }

        private val MESES = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        /** "2026-06" -> "Junio 2026"; falls back to the raw value if it can't parse. */
        private fun formatPeriodo(periodo: String): String {
            val parts = periodo.split("-")
            val mes = parts.getOrNull(1)?.toIntOrNull() ?: return periodo
            val anio = parts.getOrNull(0) ?: return periodo
            if (mes !in 1..12) return periodo
            return "${MESES[mes - 1]} $anio"
        }
    }

    inner class ViewHolder(val binding: ItemMetaHistorialBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MetaHistorialItem) {
            val ctx = binding.root.context
            binding.tvPeriodo.text = formatPeriodo(item.periodo)

            binding.tvDetalle.text = String.format(
                Locale.getDefault(),
                "Utilidad S/ %,.0f de S/ %,.0f",
                item.utilidadReal, item.metaMensual
            )

            val accentRes = if (item.cumplida) R.color.health_positive else R.color.health_danger
            val bgRes = if (item.cumplida) R.color.health_positive_bg else R.color.health_danger_bg
            binding.tvEstado.text = if (item.cumplida) "Cumplida" else "No cumplida"
            binding.tvEstado.setTextColor(ContextCompat.getColor(ctx, accentRes))
            binding.tvEstado.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, bgRes))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMetaHistorialBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
