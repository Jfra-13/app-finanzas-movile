package com.example.finanzas_independientes_app.presentation.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            binding.tvHealthMensaje.text = item.mensaje

            // Branch by code for icon + card background color
            when (item.code) {
                "GASTO_DIARIO_ALTO", "META_EN_RIESGO" -> {
                    binding.tvHealthIcon.text = "⚠️"
                    binding.root.setCardBackgroundColor(
                        binding.root.context.getColor(android.R.color.holo_red_light)
                            .let { 0x1AFF4444.toInt() } // light red tint
                    )
                    binding.root.strokeColor = 0xFFFF4444.toInt()
                    binding.tvHealthMensaje.setTextColor(
                        binding.root.context.getColor(android.R.color.holo_red_dark)
                    )
                }
                "META_CERCA" -> {
                    binding.tvHealthIcon.text = "🎉"
                    binding.root.setCardBackgroundColor(0x1A4CAF50.toInt()) // light green tint
                    binding.root.strokeColor = 0xFF4CAF50.toInt()
                    binding.tvHealthMensaje.setTextColor(0xFF2E7D32.toInt())
                }
                else -> {
                    binding.tvHealthIcon.text = "ℹ️"
                    binding.root.setCardBackgroundColor(0xFFFFFFFF.toInt())
                    binding.root.strokeColor = 0xFFA0C3D9.toInt()
                    binding.tvHealthMensaje.setTextColor(0xFF555555.toInt())
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SaludFinancieraItem>() {
        override fun areItemsTheSame(oldItem: SaludFinancieraItem, newItem: SaludFinancieraItem) =
            oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: SaludFinancieraItem, newItem: SaludFinancieraItem) =
            oldItem == newItem
    }
}
