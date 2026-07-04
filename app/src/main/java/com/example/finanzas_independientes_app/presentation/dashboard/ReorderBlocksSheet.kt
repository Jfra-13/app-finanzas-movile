package com.example.finanzas_independientes_app.presentation.dashboard

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finanzas_independientes_app.databinding.BottomSheetReorderBlocksBinding
import com.example.finanzas_independientes_app.databinding.ItemReorderBlockBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Collections

/**
 * Curved bottom sheet to reorder the three dashboard blocks by drag. On API 31+
 * it uses a real blur-behind (the only backdrop blur the View system supports
 * cleanly); older devices get a plain dim. Persists via [BlockOrderStore].
 */
class ReorderBlocksSheet(
    private val activity: AppCompatActivity,
    private val onSaved: (List<String>) -> Unit
) {

    fun show() {
        val binding = BottomSheetReorderBlocksBinding.inflate(activity.layoutInflater)
        val order = BlockOrderStore.load(activity).toMutableList()
        val adapter = ReorderAdapter(order)

        binding.rvReorder.layoutManager = LinearLayoutManager(activity)
        binding.rvReorder.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.move(vh.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = true
        })
        touchHelper.attachToRecyclerView(binding.rvReorder)

        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(binding.root)
        enableBlurBehind(dialog)

        binding.btnGuardar.setOnClickListener {
            val result = adapter.current()
            BlockOrderStore.save(activity, result)
            onSaved(result)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun enableBlurBehind(dialog: BottomSheetDialog) {
        val window = dialog.window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.2f)
            window.attributes = window.attributes.apply { blurBehindRadius = 48 }
        }
        // Pre-31 (or if the device has cross-window blur disabled) the system just
        // shows the default dim — no crash, graceful fallback.
    }

    private class ReorderAdapter(private val items: MutableList<String>) :
        RecyclerView.Adapter<ReorderAdapter.VH>() {

        fun current(): List<String> = items.toList()

        fun move(from: Int, to: Int) {
            if (from < 0 || to < 0 || from >= items.size || to >= items.size) return
            Collections.swap(items, from, to)
            notifyItemMoved(from, to)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemReorderBlockBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.binding.tvBlockName.text = BlockOrderStore.displayName(items[position])
        }

        override fun getItemCount() = items.size

        class VH(val binding: ItemReorderBlockBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
