package com.example.finanzas_independientes_app.presentation.analytics

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.R
import com.example.finanzas_independientes_app.databinding.ItemHealthSignalBinding
import com.example.finanzas_independientes_app.domain.model.SaludFinancieraItem

class HealthSignalAdapter :
    ListAdapter<SaludFinancieraItem, HealthSignalAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHealthSignalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemHealthSignalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SaludFinancieraItem) {
            val ctx = binding.root.context
            binding.tvHealthMensaje.text = item.mensaje
            binding.tvHealthMensaje.setTextColor(ContextCompat.getColor(ctx, R.color.on_surface))

            // Severity by code (README: branch by code, never message). Colours are
            // theme-aware resources — no hardcoded hex, so dark mode holds up.
            val (accentRes, bgRes, emoji) = when (item.code) {
                "GASTO_DIARIO_ALTO", "META_EN_RIESGO" ->
                    Triple(R.color.health_danger, R.color.health_danger_bg, "⚠️")
                "META_CERCA" ->
                    Triple(R.color.health_positive, R.color.health_positive_bg, "🎉")
                else ->
                    Triple(R.color.health_info, R.color.health_info_bg, "ℹ️")
            }

            val accent = ContextCompat.getColor(ctx, accentRes)
            binding.tvHealthIcon.text = emoji
            binding.root.setCardBackgroundColor(ContextCompat.getColor(ctx, bgRes))
            binding.healthAccent.backgroundTintList = ColorStateList.valueOf(accent)
            // Soft disc behind the emoji — accent at low alpha so the glyph stays legible.
            binding.healthIconCircle.backgroundTintList =
                ColorStateList.valueOf(ColorUtils.setAlphaComponent(accent, 0x33))
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SaludFinancieraItem>() {
        override fun areItemsTheSame(oldItem: SaludFinancieraItem, newItem: SaludFinancieraItem) =
            oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: SaludFinancieraItem, newItem: SaludFinancieraItem) =
            oldItem == newItem
    }
}
