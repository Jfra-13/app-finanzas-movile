package com.example.finanzas_independientes_app.presentation.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.databinding.ItemCategoryRankingBinding
import java.util.Locale

/**
 * Expense categories ranked by amount, drawn as proportional horizontal bars.
 * Replaces the old pie chart: bar length compares amounts far more accurately
 * than pie angles, and the row keeps the tap-to-drill-down behaviour.
 */
class CategoryRankingAdapter(
    private val onClick: (String) -> Unit
) : ListAdapter<CategoryRankingAdapter.Item, CategoryRankingAdapter.ViewHolder>(DIFF) {

    /** @param pct bar fill 0..100, relative to the largest category. */
    data class Item(val nombre: String, val monto: Double, val pct: Int)

    inner class ViewHolder(
        private val binding: ItemCategoryRankingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.tvCategoryName.text = item.nombre
            binding.tvCategoryAmount.text = String.format(Locale.getDefault(), "S/ %.0f", item.monto)
            binding.barCategory.setProgressCompat(item.pct, true)
            binding.root.setOnClickListener { onClick(item.nombre) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryRankingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(a: Item, b: Item) = a.nombre == b.nombre
            override fun areContentsTheSame(a: Item, b: Item) = a == b
        }
    }
}
